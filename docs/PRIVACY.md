# Política de privacidade do CallGuide

Versão 2.0 · Atualização: 5 de novembro de 2026.

Política publicada: [https://sidineyr.github.io/callguide/privacy/](https://sidineyr.github.io/callguide/privacy/). Este documento registra as mesmas práticas da página pública. A disponibilidade de cada variante na loja e o formulário Segurança dos dados devem corresponder ao pacote efetivamente distribuído.

## Identificação e contato

O CallGuide é um aplicativo educativo de Sidiney Rodrigues para demonstrar letras cursivas e oferecer um papel digital de prática. Atende crianças e adultos. Dúvidas sobre privacidade e pedidos relacionados a dados podem ser enviados a **[sidiney@edu.univali.br](mailto:sidiney@edu.univali.br)**. Responsáveis por crianças podem usar o mesmo contato. Código público: [github.com/sidineyr/callguid](https://github.com/sidineyr/callguid).

## Conteúdo educativo e dados locais

O aplicativo não exige cadastro, nome, e-mail, localização ou data completa de nascimento para acessar as letras. As trajetórias das letras, a ilustração e as animações estão incluídas no aplicativo e funcionam sem internet. Os traçados feitos no papel digital permanecem temporariamente na memória do aplicativo e são descartados ao sair dessa tela ou encerrar o processo; não são enviados ao desenvolvedor. A escolha atual de letra e opções de exibição podem ser restauradas pelo Android durante uma recriação da tela. O backup de dados do aplicativo está desativado.

## Versão gratuita sem publicidade

A variante **free** não incorpora SDK publicitário ou de análise, não solicita anúncios e não possui permissão de acesso à internet. O desenvolvedor não recebe registros de letras escolhidas, desenhos ou progresso nessa variante. A abertura voluntária desta política no navegador usa a conexão do navegador e o serviço de hospedagem GitHub Pages, que pode processar dados técnicos da visita conforme suas próprias políticas.

## Versão com publicidade

A variante **sponsored** inclui o Google Mobile Ads SDK e a plataforma de consentimento UMP. Uma pergunta de faixa etária, sem resposta pré-selecionada, distingue até 12 anos, 13 a 17 anos, 18 anos ou mais e idade não informada. A resposta fica apenas na memória da sessão. O código do aplicativo só solicita anúncios quando o usuário informa ter 18 anos ou mais e os controles de consentimento permitem. Não solicita anúncios para crianças, adolescentes ou idade não informada.

O SDK publicitário está presente nessa variante e pode instalar componentes de inicialização próprios; a ausência de uma solicitação de anúncio não deve ser interpretada como ausência de qualquer componente do Google. A variante sem publicidade é a opção que não contém esse SDK. A permissão de identificador de publicidade `AD_ID` é removida do manifest da variante patrocinada.

Para servir anúncios a adultos, o Google pode processar endereço IP, informações técnicas do dispositivo e do aplicativo, interações publicitárias, diagnósticos e identificadores permitidos pela configuração e pelo sistema. As finalidades incluem entrega e medição de anúncios, funcionamento do serviço, prevenção de fraude e segurança. O app solicita anúncios não personalizados e com classificação máxima de conteúdo G; isso não significa que não exista processamento técnico. O desenvolvedor não mantém um servidor próprio para receber os desenhos ou a faixa etária.

A publicidade aparece identificada numa pausa entre lições, com botão de continuação imediata. Nenhuma letra exige clicar em anúncio. As lições continuam disponíveis quando não há rede ou anúncio. Builds de desenvolvimento usam unidades demonstrativas oficiais do Google e não são destinados à monetização real.

## Consentimento e controles

Para adultos elegíveis, a UMP atualiza o estado de consentimento a cada sessão e apresenta as mensagens exigidas quando configuradas e aplicáveis à região. As opções de privacidade ficam em “Orientações e privacidade” → “Opções de privacidade dos anúncios”. Quando a UMP indicar que novas escolhas são necessárias, esse controle abre o formulário correspondente. Também é possível fechar o app, iniciar nova sessão e não informar a idade para usar o conteúdo sem solicitações de anúncios.

## Retenção e atendimento

O desenvolvedor não retém no servidor desenhos, histórico de letras ou respostas de idade. Dados técnicos processados pelo Google e pelo serviço de hospedagem seguem as políticas e os períodos de retenção desses provedores. Não prometemos excluir diretamente dados mantidos por terceiros. Pedidos e dúvidas podem ser enviados ao contato acima; evite anexar dados sensíveis desnecessários. Dados eventualmente enviados por e-mail serão usados para atender o pedido e mantidos apenas pelo período necessário ao atendimento e às obrigações aplicáveis.

Referências dos provedores: [Política de privacidade do Google](https://policies.google.com/privacy), [dados tratados pelo Google Mobile Ads SDK](https://developers.google.com/admob/android/privacy/play-data-disclosure) e [privacidade do GitHub](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).

## Alterações

Esta política será revisada quando houver mudanças no aplicativo ou em seus provedores. A data desta página identifica a versão vigente. A disponibilidade de uma variante na loja e as declarações de Segurança dos dados devem corresponder ao pacote efetivamente distribuído.

## Registro de preparação da distribuição

O CallGuide Android foi criado no AdMob com App ID público `ca-app-pub-4924241177850142~6693829379` e unidade banner `ca-app-pub-4924241177850142/2754584368`. Vídeo e atualização automática foram desativados na unidade. Isso não equivale a anúncios aprovados ou ativos: a vinculação à loja e a análise do aplicativo continuam pendentes.

O build `free` não inclui SDK publicitário. `sponsored` usa por padrão o sufixo `.adstest`; em debug, o controller sempre solicita a unidade demonstrativa oficial, mesmo usando o App ID do proprietário. A release patrocinada é desabilitada por padrão e exige `-PproductionAds=true`, que mantém o pacote de produção `com.sidineyr.callguide` sem sufixo e habilita o fluxo `ADS_ENABLED`. A inicialização explícita e as solicitações pelo código são restritas a adultos elegíveis; a presença de componentes automáticos do SDK deve ser considerada na revisão do manifest e do tráfego.

Antes do envio à loja, ainda é necessário testar o artefato em dispositivo, concluir a assinatura de upload, revisar o comportamento e o inventário dos SDKs no AAB final e preencher Segurança dos dados. A política publicada e a criação do app no AdMob não substituem essas verificações nem a aprovação das plataformas. Consulte [PLAY_STORE.md](PLAY_STORE.md).
