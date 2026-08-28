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

O workflow **Android CI** repete esses testes a cada envio ao branch `master` e disponibiliza o APK como artefato quando o build termina com sucesso.

## Princípios pedagógicos

O CallGuide valoriza legibilidade, conforto e autoria. O aplicativo não atribui notas nem define uma “letra perfeita”; ele oferece modelos e incentiva a prática consciente, respeitando ritmos e condições motoras diferentes.

## Privacidade

Todo o conteúdo funciona localmente. O aplicativo não solicita acesso à internet e não armazena os traçados realizados.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).
