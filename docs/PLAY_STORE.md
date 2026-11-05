# Preparação para Google Play — CallGuide

Revisão das fontes oficiais: 5 de novembro de 2026. Este documento é um roteiro de entrega, não uma aprovação do Google nem uma declaração de conformidade concluída. Conferir novamente as regras na data do envio.

## Público e experiência

O público solicitado é **crianças e adultos**. Declarar no Play Console apenas as faixas etárias para as quais o app foi realmente projetado e testado. A personagem infantil e a aprendizagem da escrita fazem parte dessa avaliação. Preencher classificação indicativa, público-alvo, acesso ao app e declaração de anúncios com base no artefato enviado. Não declarar o app como exclusivo de adultos para evitar as regras de famílias. [Público-alvo](https://support.google.com/googleplay/android-developer/answer/9867159?hl=pt-BR).

O fluxo esperado é abertura → alfabeto → demonstração cursiva → retorno ao alfabeto. Disponibilizar saída, repetição e pausa; testar leitores de tela, fonte ampliada, áreas de toque, orientação, retomada e opção de reduzir movimento. Validar a sequência pedagógica de todos os traços com uma pessoa com experiência em alfabetização/caligrafia antes da publicação.

Foram regeneradas 52 animações, abrangendo maiúsculas e minúsculas, com malhas de pressão cuja razão mediana entre largura de descida e subida é aproximadamente 4:1. Essa medição de geração não substitui a revisão visual e pedagógica nem teste em dispositivo.

## Anúncios: decisão de implementação

Usar **AdMob para Android**, não o código AdSense de sites. O formato de criativo servido pela rede não deve ser prometido como exclusivamente textual sem uma configuração que efetivamente o garanta.

A variante `free` não contém SDK publicitário nem permissão de internet. A variante `sponsored` inclui Google Mobile Ads e UMP; o código do app só inicializa explicitamente publicidade e solicita anúncios para quem informa ter 18 anos ou mais e passa pelos controles de consentimento. Crianças, adolescentes e idade desconhecida não entram nesse fluxo. Componentes automáticos do SDK podem existir no manifest patrocinado: não prometer ausência de qualquer comportamento do SDK. A permissão `AD_ID` é removida dessa variante.

O banner aparece numa pausa opcional após pelo menos seis lições e 90 segundos, com continuação imediata; esses limites são escolha de produto, não números prescritos pelo Google. As builds debug de `sponsored` sempre usam a unidade demonstrativa oficial, mesmo com o App ID do proprietário. A release patrocinada é desabilitada por padrão e exige a opção explícita `-PproductionAds=true`; essa opção não significa aprovação nem liberação automática de anúncios.

Não inserir intersticial a cada toque de letra: as lições duram poucos segundos. Como regra de produto, preferir um único espaço claramente identificado, separado do teclado e sem bloquear a lição; se forem adotados intersticiais, exibi-los apenas em pausas naturais e com intervalo conservador, nunca na abertura nem sobre a escrita. Se não houver anúncio, o aprendizado deve continuar. [Orientação de intersticiais](https://support.google.com/admob/answer/6066980?hl=pt-BR), [implementações proibidas](https://support.google.com/admob/answer/6201362?hl=pt-BR).

Para público misto, implementar uma tela etária neutra antes de inicializar publicidade; não pré-selecionar a idade, sugerir a resposta ou dar vantagem para quem se declara adulto. As opções locais previstas são menos de 13, de 13 a 17, 18 ou mais e não informar, sem data de nascimento completa. Se anúncios infantis forem introduzidos no futuro, usar somente versões de SDK autocertificadas para famílias, sem publicidade por interesses ou remarketing, com conteúdo adequado e fechamento em até cinco segundos para anúncios que interrompem o uso. Não mostrar várias posições na mesma página nem disfarçar publicidade de tecla ou lição. [Política de famílias](https://support.google.com/googleplay/android-developer/answer/9893335?hl=pt-BR).

### Conta AdMob configurada

O aplicativo Android CallGuide foi criado na conta AdMob do proprietário. Seus identificadores são públicos, não senhas ou chaves secretas:

| Campo | Valor |
| --- | --- |
| App ID | `ca-app-pub-4924241177850142~6693829379` |
| Unidade banner | `ca-app-pub-4924241177850142/2754584368` |

Vídeo e atualização automática do banner foram desativados na configuração da unidade. Não prometer, por isso, um criativo exclusivamente textual. O aplicativo ainda depende de vinculação à loja e da análise/liberação do AdMob; a configuração da conta não comprova monetização ativa.

Antes de habilitar tráfego real:

- Validar ausência de inicialização explícita e de solicitações publicitárias pelo app nos caminhos de menores e idade desconhecida, inclusive na retomada do processo. Inspecionar o manifest mesclado, os componentes automáticos e o tráfego do SDK patrocinado; a variante `free` é a opção sem SDK.
- Se no futuro houver publicidade infantil, confirmar versão exata na lista de SDKs autocertificados e adaptadores; aplicar tratamento infantil/idade desconhecida e limite `G` antes das solicitações. [Configuração AdMob](https://developers.google.com/admob/android/targeting), [AdMob e famílias](https://support.google.com/admob/answer/6223431?hl=pt-BR).
- Para usuários elegíveis, atualizar consentimento UMP a cada abertura com `requestConsentInfoUpdate()`, mostrar formulários quando exigidos, verificar `canRequestAds()` e expor opções de privacidade quando requeridas. Não usar consentimento adulto como substituto das proteções infantis. [UMP](https://developers.google.com/admob/android/privacy).
- Durante desenvolvimento, usar apenas unidades demonstrativas oficiais ou dispositivos configurados para teste; nunca clicar em anúncios reais. O controller usa a unidade demonstrativa em debug; os IDs públicos do proprietário ficam reservados ao fluxo de release. [Anúncios de teste](https://developers.google.com/admob/android/test-ads).
- Finalizar vinculação à loja, análise do aplicativo e mensagens de privacidade na conta AdMob. Não há keystore, senha ou credencial privada de produção neste repositório.

## Artefato, assinatura e teste

Para novos apps e atualizações Android comuns, a exigência vigente desde 31/08/2026 é `targetSdk` **36 ou superior**. Atualizar também compile SDK e ferramentas compatíveis, e testar as mudanças de comportamento. [API alvo](https://developer.android.com/google/play/requirements/target-sdk).

Gerar um **Android App Bundle de release assinado** com a chave de upload do proprietário e configurar Play App Signing. Não versionar keystore, senhas, propriedades de assinatura ou credenciais. Um APK debug permite teste local, mas não representa um lançamento assinado na loja. Preservar o identificador do aplicativo se já houver uma listagem existente. [Assinatura](https://developer.android.com/studio/publish/app-signing).

As variantes usam o gate `ADS_ENABLED`. Por padrão, `sponsored` recebe o sufixo de pacote `.adstest`, usa anúncios demonstrativos em debug e não gera release. A invocação deliberada `./gradlew bundleSponsoredRelease -PproductionAds=true` habilita a variante de produção com o pacote `com.sidineyr.callguide`, sem esse sufixo, e a unidade banner do proprietário. A versão sem SDK é gerada com `./gradlew bundleFreeRelease`. Esses comandos não configuram por si a assinatura de upload: concluir a assinatura segura antes do envio. No Windows, usar `gradlew.bat`.

Executar build, testes e lint; instalar em dispositivo físico e emulador, testar sem rede, anúncios indisponíveis, consentimento recusado, retorno do segundo plano e todas as letras. Anexar resultados reais à release. Contas pessoais criadas depois de 13/11/2023 estão sujeitas ao teste fechado com pelo menos 12 participantes inscritos continuamente por 14 dias e posterior solicitação de acesso à produção. Isso não significa aprovação automática. [Teste de contas pessoais](https://support.google.com/googleplay/android-developer/answer/14151465?hl=pt-BR).

## Ficha da loja e privacidade

Preparar nome, descrições, ícone, capturas reais do app e imagem de destaque. Descrever demonstrações cursivas, público, uso offline do conteúdo e anúncios de acordo com a versão entregue. Não declarar “sem coleta de dados” numa versão que incorpora SDK publicitário sem revisar os dados desse SDK.

A política de privacidade precisa estar no app e numa URL pública ativa, sem PDF ou bloqueio geográfico, com identificação do app/desenvolvedor, canal de contato, dados, finalidades, compartilhamento, retenção e exclusão. Preencher Segurança dos dados a partir do **AAB final e seus SDKs**, incluindo publicidade, mesmo que o desenvolvedor não mantenha servidor próprio. [Políticas de dados e privacidade](https://support.google.com/googleplay/android-developer/answer/18265346?hl=pt-BR).

A política foi publicada em [sidineyr.github.io/callguide/privacy/](https://sidineyr.github.io/callguide/privacy/), com contato público autorizado **sidiney@edu.univali.br**. O texto distingue `free` de `sponsored` e os componentes automáticos do SDK. Manter o documento [PRIVACY.md](PRIVACY.md), a página pública e as declarações da loja coerentes com o artefato escolhido.

## Portões de lançamento ainda dependentes do proprietário

- Conta Play Console e verificações aplicáveis; confirmação de package/listagem existente.
- Chave de upload, assinatura e AAB final testado.
- Revisão pedagógica dos traços e revisão cultural da ilustração original.
- Revisão final da política já hospedada contra o AAB distribuído e preenchimento de Segurança dos dados.
- Vinculação à loja e análise AdMob, SDK e consentimento, teste de anúncios e declarações de dados.
- Classificação, público-alvo, ficha, teste fechado quando exigido e aprovação pelo Google.

Não marcar esses itens como concluídos apenas porque existem código ou documentos.
