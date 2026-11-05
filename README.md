# CallGuide — caligrafia offline para Android

**Aplicativo gratuito de caligrafia para Android**, com exercícios de letras e palavras, modelos de escrita e área pautada para prática com o toque. Funciona offline, sem cadastro ou anúncios, e tem código aberto sob licença MIT.

[Conheça o CallGuide e seus recursos](https://sidineyr.github.io/callguide/) · [Portfólio de Sidiney Rodrigues](https://sidineyr.github.io/)

A free, offline Android application for introductory handwriting practice. The project combines pedagogical guidance, letter models, and a ruled touch-based practice area.

## Features

- guidance on posture, pencil grip, pressure, and rhythm;
- progressive exercises with letters and words;
- a tracing canvas with **Undo** and **Clear** controls;
- a responsive Portuguese-language interface compatible with screen readers;
- no accounts, advertisements, special permissions, or data collection.

## Run the project

Requirements: Android Studio, JDK 11, and Android SDK 33. On Windows, use `gradlew.bat` instead of `./gradlew`.

```bash
./gradlew test
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/`.

### Test on Windows without publishing to an app store

The recommended test mode generates a local debug APK. In PowerShell, from the project directory, run:

```powershell
.\scripts\test-apk.ps1
```

To also install the application on an Android device connected through USB, run:

```powershell
.\scripts\test-apk.ps1 -Install
```

First enable **Developer options** and **USB debugging** on the Android device. The script uses the Gradle Wrapper and ADB, detects the default Android Studio installation paths, and requires no developer account, production signing key, or Play Store publication.

The **Android CI** workflow remains available for manual runs. It will not run automatically while the account runner continues to fail before executing any project step.

## Pedagogical principles

CallGuide values legibility, comfort, and authorship. The application does not assign grades or define “perfect handwriting.” It provides models and encourages deliberate practice while respecting different learning rhythms and motor abilities.

## Privacy

All content runs locally. The application does not request internet access and does not store completed traces.

## Fonte inspirada em gabarito manuscrito

O projeto também publica a **Sidiney Caligrafia Refinada**, uma fonte TTF inspirada em um gabarito manuscrito de Sidiney Rodrigues. O redesenho usa curvas contínuas, terminais arredondados e contraste entre subidas finas e descidas fortes.

[Conheça o processo, veja o gabarito e baixe os arquivos](fonts/sidiney-caligrafia/README.md).

![Prévia da fonte Sidiney Caligrafia Refinada](fonts/sidiney-caligrafia/previas/Previa-Caligrafia-Refinada.png)

## License

Distributed under the MIT License. See [LICENSE](LICENSE).
