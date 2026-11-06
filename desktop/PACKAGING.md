# Desktop packaging

`package-desktop.py` downloads Electron 45.2.3 from the Electron project's official GitHub release and checks it against the corresponding official SHA-256 manifest. It copies only the application allowlist and `assets/` into `resources/app`; no npm installation, credentials or private key is included. Electron's own license files remain in the distribution.

```sh
python package-desktop.py --platform win32 --arch x64 --output dist
python package-desktop.py --platform linux --arch x64 --output dist
# Run the following on macOS:
python package-desktop.py --platform darwin --arch x64 --output dist
python package-desktop.py --platform darwin --arch arm64 --output dist
```

Output filenames identify the system and architecture. Windows is a portable ZIP directory; Linux is a tar.gz directory preserving executable permissions and symlinks; macOS is a ZIP containing `CallGuide.app`. Extract the entire directory. The desktop renderer loads only packaged local content.

The macOS step uses native `codesign --force --deep --sign -` and verifies the resulting bundle, then archives it with `ditto`. This is an ad-hoc signature for internal consistency, **not** an Apple Developer ID signature or notarization. Windows is not Authenticode-signed. Users' OS or organization policies may prevent unsigned software from launching. No script disables OS protections or Electron's sandbox.

The workflow template `desktop-workflow.yml` belongs at `.github/workflows/desktop.yml` in the repository. It runs independent native builds, tests geometry, and runs smoke tests under Windows x64, Linux x64 (Xvfb), macOS Intel x64 (`macos-15-intel`) and macOS Apple Silicon ARM64 (`macos-15`). Physical-tablet behavior remains unverified. Smoke evidence is retained as a separate workflow artifact.

For the Linux runner's smoke test only, the extracted sandbox helper is assigned its required root ownership and setuid mode. The distributed archive is created before that runner-specific change. On Linux desktops, Chromium's sandbox also depends on the OS namespace/sandbox policy. This project does not ship an automatic security-policy bypass.

Successful builds on `master` publish four packages and aggregate SHA-256 checksums into `desktop-v2.2.0`; PR builds do not publish. The Android `v2.1.0` release is left untouched. The publishing token is the workflow-scoped `GITHUB_TOKEN` with repository contents permission, not a stored personal secret.

The renderer's `main.js --smoke-test` contract is: read `CALLGUIDE_SMOKE_DIR`, run the bounded offline smoke checks, write `smoke.json` with `passed: true` on success plus `CallGuide-desktop.png`, then exit 0 (otherwise exit nonzero). This is automated runtime evidence, not a claim of manual cross-platform device testing.
