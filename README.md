# CallGuide — caligrafia cursiva animada para Android

Aplicativo gratuito para crianças e adultos observarem o desenho de letras cursivas. A abertura traz um pequeno príncipe indígena imaginário, criado com IA; o teclado oferece 26 letras em cada caixa. Ao escolher uma letra, uma caneta revela seus traços em cerca de 4,2 segundos e retorna ao alfabeto. Há pausa, repetição, retorno automático opcional e papel digital para experimentar.

[Página do projeto](https://sidineyr.github.io/callguide/) · [Política de privacidade](https://sidineyr.github.io/callguide/privacy/) · [Preparação para Google Play](docs/PLAY_STORE.md)

## Pressão da caneta

As 52 animações usam as trajetórias originais da fonte Sidiney Caligrafia Refinada, e não o contorno de uma letra preenchido artificialmente. Cada amostra contém as duas bordas da tinta, a posição da ponta, a largura e a distância percorrida. Descidas recebem pressão, subidas ficam leves; a largura é suavizada e aliviada nas entradas e saídas. Travessas finas preservam sua espessura própria. O contraste mediano nas amostras de descida forte/subida leve é aproximadamente 4:1. Android, prévia HTML e exportações GIF consomem a mesma geometria.

Essa é uma simulação visual de pressão para ensino, não uma medida de força física do dedo. Os modelos valorizam conforto e legibilidade, sem notas ou exigência de uma escrita perfeita. Revisão pedagógica em dispositivo permanece parte do lançamento.

## Build e teste

Requisitos: JDK 17, Android SDK 36, Build Tools 35.0.0. O wrapper usa Gradle 8.11.1 e AGP 8.10.1. No Windows, substituir `./gradlew` por `.\gradlew.bat`.

```sh
./gradlew testFreeDebugUnitTest assembleFreeDebug
./gradlew assembleSponsoredDebug
./gradlew bundleFreeRelease
```

- `free`: sem SDK publicitário ou permissão de internet; APK em `app/build/outputs/apk/free/debug/app-free-debug.apk`.
- `sponsored` debug: pergunta etária neutra; somente adultos entram no fluxo UMP. Usa unidade demonstrativa oficial do Google, nunca anúncios reais durante o desenvolvimento.
- `sponsored` release: desativada por padrão. Após validação dos controles de consentimento, da política, dos dados, da conta AdMob e do app na loja, gerar com `./gradlew bundleSponsoredRelease -PproductionAds=true`. Essa variante usa os IDs públicos do proprietário; o AAB precisa da assinatura de upload do proprietário antes do envio. Não misturar as variantes numa mesma publicação.

A propriedade opcional `-PdebugSigningStore=/caminho/chave-de-teste.jks` permite um keystore local de debug. Não versionar chaves de assinatura nem senhas. Não usar essa propriedade como substituto da assinatura de release.

No Windows, `scripts/test-apk.ps1` gera a variante livre; `-Install` também instala em dispositivo USB autorizado. O CI permanece manual enquanto a disponibilidade do runner da conta não for confirmada.

## Anúncios e público misto

O formato é um banner identificado numa pausa após pelo menos seis lições e 90 segundos de sessão; a continuação é imediata. Não há anúncio por letra, sobre a animação, na abertura ou exigência de clicar em publicidade. Sem rede ou consentimento elegível, as letras continuam funcionando.

A variante patrocinada não solicita anúncios para menores de 18 anos ou idade desconhecida. A faixa etária não é salva em disco. O SDK está incluído nessa variante e possui componentes automáticos próprios; não afirmar que a variante patrocinada é equivalente à versão sem SDK. A permissão AD_ID é removida. A interface oferece opções de privacidade UMP. Conteúdo publicitário é limitado à classificação G e solicitações são não personalizadas, o que não equivale a ausência de processamento técnico.

O aplicativo CallGuide e o banner foram cadastrados no AdMob, com vídeo e atualização automática do banner desativados. A aprovação do aplicativo ainda depende de vinculação à loja e revisão. Veja [ADMOB.md](docs/ADMOB.md) para IDs e próximas verificações. O formato Banner agrupa texto, gráfico e rich media; não há promessa de anúncios exclusivamente textuais.

## Prévia e validação dos traços

```sh
python scripts/export-strokes.py
python scripts/validate-strokes.py
python scripts/build-preview.py
```

Abra `preview/index.html` para revisar as animações sem internet. Para exportar a folha de revisão e os 52 GIFs, instale Pillow e execute `python scripts/render-pressure.py --gifs`. Esses arquivos são prévias, não capturas do app Android. Os scripts de exportação e validação dos dados não precisam de Pillow ou das dependências de construção da fonte.

## Fonte, arte e licença

[Sidiney Caligrafia Refinada](fonts/sidiney-caligrafia/README.md) inclui o processo da fonte e o gabarito manuscrito original. A ilustração de abertura e seu prompt estão documentados em [ARTWORK.md](docs/ARTWORK.md).

Código distribuído sob [licença MIT](LICENSE). Publicação na Play Store requer artefato assinado, declaração de dados, testes e aprovação do Google; a presença deste projeto não representa aprovação da loja.
