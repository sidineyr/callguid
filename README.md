# CallGuide

Aplicativo Android gratuito e offline para iniciação à caligrafia. O projeto combina orientação pedagógica, modelos de letras e uma área pautada de prática por toque.

## Recursos

- guia de postura, empunhadura, pressão e ritmo;
- exercícios progressivos com letras e palavras;
- tela de traçado com **desfazer** e **limpar**;
- interface em português, responsiva e compatível com leitores de tela;
- nenhuma conta, anúncio, permissão especial ou coleta de dados.

## Executar

Requisitos: Android Studio, JDK 11 e Android SDK 33. No Windows, use `gradlew.bat` no lugar de `./gradlew`.

```bash
./gradlew test
./gradlew assembleDebug
```

O APK será criado em `app/build/outputs/apk/debug/`.

### Testar no Windows sem publicar em loja

O modo de teste recomendado gera um APK de depuração local. No PowerShell, dentro da pasta do projeto, execute:

```powershell
.\scripts\test-apk.ps1
```

Para também instalar o aplicativo em um celular conectado por USB:

```powershell
.\scripts\test-apk.ps1 -Install
```

Ative antes as **Opções do desenvolvedor** e a **Depuração USB** no Android. O script usa o Gradle Wrapper e o ADB, detecta as instalações padrão do Android Studio e não exige conta de desenvolvedor, assinatura de produção ou publicação na Play Store.

O workflow **Android CI** permanece disponível para acionamento manual. Ele não roda automaticamente enquanto o runner da conta falha antes de executar qualquer etapa do projeto.

## Princípios pedagógicos

O CallGuide valoriza legibilidade, conforto e autoria. O aplicativo não atribui notas nem define uma “letra perfeita”; ele oferece modelos e incentiva a prática consciente, respeitando ritmos e condições motoras diferentes.

## Privacidade

Todo o conteúdo funciona localmente. O aplicativo não solicita acesso à internet e não armazena os traçados realizados.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).
