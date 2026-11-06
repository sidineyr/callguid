"""Package CallGuide with an official, checksum-verified Electron runtime.

No npm installation, native compilation, account or private signing key is required.
macOS archives must be generated on macOS to produce a valid local ad-hoc signature.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import plistlib
import shutil
import stat
import subprocess
import sys
import tarfile
import urllib.request
import zipfile

VERSION = '2.2.0'
ELECTRON = '45.2.3'
SOURCE = Path(__file__).resolve().parent
REQUIRED = ['main.js', 'package.json', 'index.html', 'app.js', 'style.css', 'geometry.js', 'ink.js', 'LICENSE']


def download(url, target):
    if target.exists():
        return
    temporary = target.with_suffix(target.suffix + '.partial')
    request = urllib.request.Request(url, headers={'User-Agent': 'CallGuide-desktop-builder'})
    with urllib.request.urlopen(request, timeout=120) as response, temporary.open('wb') as stream:
        shutil.copyfileobj(response, stream)
    temporary.replace(target)


def verified_runtime(cache, platform, arch):
    name = f'electron-v{ELECTRON}-{platform}-{arch}.zip'
    base = f'https://github.com/electron/electron/releases/download/v{ELECTRON}/'
    sums = cache / f'electron-v{ELECTRON}-SHASUMS256.txt'
    download(base + 'SHASUMS256.txt', sums)
    manifest = {}
    for line in sums.read_text().splitlines():
        parts = line.split()
        if len(parts) == 2:
            manifest[parts[1].lstrip('*')] = parts[0]
    if name not in manifest:
        raise RuntimeError(f'Official checksum not found: {name}')
    archive = cache / name
    download(base + name, archive)
    digest = hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest()
    if digest != manifest[name]:
        raise RuntimeError(f'Runtime checksum mismatch; remove cached file and retry: {archive}')
    print(f'Verified official runtime: {name} SHA256={digest}', flush=True)
    return archive


def extract_runtime(archive, directory):
    directory.mkdir(parents=True)
    root = directory.resolve()
    with zipfile.ZipFile(archive) as package:
        for member in package.infolist():
            relative = PurePosixPath(member.filename)
            if relative.is_absolute() or '..' in relative.parts:
                raise RuntimeError('Unsafe runtime archive path')
            target = directory.joinpath(*relative.parts)
            if not target.resolve().is_relative_to(root):
                raise RuntimeError('Runtime archive path escapes build directory')
            mode = member.external_attr >> 16
            if member.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            if stat.S_ISLNK(mode):
                link = package.read(member).decode('utf-8')
                if not (target.parent / link).resolve().is_relative_to(root):
                    raise RuntimeError('Runtime archive symlink escapes build directory')
                target.symlink_to(link)
            else:
                with package.open(member) as src, target.open('wb') as dst:
                    shutil.copyfileobj(src, dst)
                if os.name != 'nt' and mode:
                    target.chmod(stat.S_IMODE(mode))


def copy_app(resources):
    app = resources / 'app'
    app.mkdir(parents=True, exist_ok=True)
    for name in REQUIRED:
        shutil.copy2(SOURCE / name, app / name)
    shutil.copytree(SOURCE / 'assets', app / 'assets')
    # Generate browser data from the canonical JSON; the repository stores one copy.
    data = (app / 'assets/letters.json').read_text(encoding='utf-8')
    (app / 'assets/letters.js').write_text('window.CALLGUIDE_LETTERS=' + data + ';',encoding='utf-8')
    # The runtime licenses remain beside the executable, and the project's license
    # also travels inside resources/app.
    return app


def zip_directory(directory, destination):
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as package:
        for entry in sorted(directory.rglob('*')):
            if entry.is_file():
                package.write(entry, entry.relative_to(directory.parent).as_posix())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--platform', choices=['win32', 'linux', 'darwin'], required=True)
    parser.add_argument('--arch', choices=['x64', 'arm64'], required=True)
    parser.add_argument('--output', type=Path, default=SOURCE / 'dist')
    args = parser.parse_args()
    if args.platform == 'darwin' and sys.platform != 'darwin':
        raise SystemExit('Build the macOS package on macOS so codesign and ditto preserve the bundle.')
    for name in REQUIRED + ['assets/letters.json', 'README.md']:
        if not (SOURCE / name).is_file():
            raise SystemExit(f'Missing source file: {name}')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    cache = output / '.runtime-cache'
    cache.mkdir(exist_ok=True)
    platform_name = {'win32': 'windows', 'linux': 'linux', 'darwin': 'macos'}[args.platform]
    folder_name = f'CallGuide-{platform_name}-{args.arch}'
    build = output / '.build' / folder_name
    if build.exists():
        # Only delete the one resolved task-specific staging directory.
        expected = (output / '.build').resolve()
        if build.resolve().parent != expected:
            raise RuntimeError('Invalid staging directory')
        shutil.rmtree(build)
    archive = verified_runtime(cache, args.platform, args.arch)
    extract_runtime(archive, build)
    if args.platform == 'darwin':
        original = build / 'Electron.app'
        bundle = build / 'CallGuide.app'
        original.rename(bundle)
        copy_app(bundle / 'Contents/Resources')
        plist_file = bundle / 'Contents/Info.plist'
        with plist_file.open('rb') as stream:
            info = plistlib.load(stream)
        info.update(CFBundleName='CallGuide', CFBundleDisplayName='CallGuide',
                    CFBundleIdentifier='com.sidineyr.callguide.desktop',
                    CFBundleShortVersionString=VERSION, CFBundleVersion=VERSION)
        with plist_file.open('wb') as stream:
            plistlib.dump(info, stream)
        # A local ad-hoc signature makes the modified bundle internally valid.
        # It is not Developer ID signing and does not imply Apple notarization.
        subprocess.run(['codesign', '--force', '--deep', '--sign', '-', str(bundle)], check=True)
        subprocess.run(['codesign', '--verify', '--deep', '--strict', str(bundle)], check=True)
    else:
        copy_app(build / 'resources')
        if args.platform == 'win32':
            (build / 'electron.exe').rename(build / 'CallGuide.exe')
        else:
            executable = build / 'callguide'
            (build / 'electron').rename(executable)
            executable.chmod(executable.stat().st_mode | 0o111)
    shutil.copy2(SOURCE / 'README.md', build / 'LEIA-ME-README.md')
    basename = f'CallGuide-Desktop-{VERSION}-{platform_name}-{args.arch}'
    if args.platform == 'linux':
        artifact = output / (basename + '.tar.gz')
        with tarfile.open(artifact, 'w:gz') as package:
            package.add(build, arcname=folder_name)
    elif args.platform == 'darwin':
        artifact = output / (basename + '.zip')
        subprocess.run(['ditto', '-c', '-k', '--sequesterRsrc', '--keepParent', str(build), str(artifact)], check=True)
    else:
        artifact = output / (basename + '.zip')
        zip_directory(build, artifact)
    digest = hashlib.file_digest(artifact.open('rb'), 'sha256').hexdigest()
    artifact.with_suffix(artifact.suffix + '.sha256').write_text(f'{digest}  {artifact.name}\n', encoding='ascii')
    print(json.dumps({'artifact': str(artifact), 'sha256': digest, 'bytes': artifact.stat().st_size,
                      'platform': args.platform, 'arch': args.arch, 'electron': ELECTRON}), flush=True)


if __name__ == '__main__':
    main()
