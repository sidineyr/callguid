# CallGuide no AdMob

Cadastrado em 5 de novembro de 2026 na conta do proprietário, como aplicativo Android ainda não publicado em loja.

| Campo | Identificador público |
| --- | --- |
| App | CallGuide |
| App ID | `ca-app-pub-4924241177850142~6693829379` |
| Banner “CallGuide — pausa entre letras” | `ca-app-pub-4924241177850142/2754584368` |
| Unidade demonstrativa para debug | `ca-app-pub-3940256099942544/6300978111` |

O banner foi criado com **vídeo desmarcado** e **atualização automática desativada**. O tipo disponível agrupa texto, gráfico e rich media: não existe garantia de criativo exclusivamente textual nessa configuração. A conta mantém suas configurações de eCPM; não foram criadas campanhas pagas ou alterados dados de pagamento.

O AdMob mostrou **“Requer revisão”** para o aplicativo. Cadastro da conta e criação de unidade não significam aprovação do app para anúncios de produção. Depois de publicar na loja compatível, vincular sua listagem ao aplicativo existente no AdMob e acompanhar a revisão. Não criar outro cadastro duplicado.

O site publica `https://sidineyr.github.io/app-ads.txt` com o publisher autorizado. Usar `https://sidineyr.github.io/` como site do desenvolvedor na listagem da Play Store, permitindo que o Google encontre esse arquivo. A verificação pode levar alguns dias e depende da listagem e do rastreamento; ela não foi declarada concluída.

Política de privacidade: https://sidineyr.github.io/callguide/privacy/ . Contato público autorizado pelo proprietário: `sidiney@edu.univali.br`.

Antes de distribuir a variante patrocinada:

1. Revisar, publicar e testar mensagens UMP aplicáveis aos países de distribuição. O rascunho **“CallGuide — consentimento europeu”** já foi salvo, associado somente ao CallGuide, com a política pública vinculada, português como idioma padrão, inglês adicional e botão “Não consentir” habilitado para todas as regiões da mensagem. Ele **não foi publicado**: revisar parceiros/finalidades e testar no app antes de ativá-lo. Os controles regionais dos EUA ainda precisam ser configurados se a distribuição abranger esses mercados.
2. Testar os caminhos menor/adolescente/idade desconhecida sem solicitações publicitárias; considerar o comportamento de componentes automáticos do SDK na análise de dados.
3. Inspecionar o manifest e o inventário do AAB final para Segurança dos dados. Não afirmar “nenhuma coleta” na variante com SDK.
4. Testar com unidade demonstrativa/dispositivo de teste, rede indisponível, recusa e revisão de consentimento. Não clicar em anúncios reais.
5. Gerar `bundleSponsoredRelease -PproductionAds=true`, assinar com chave de upload do proprietário e concluir as revisões da Play Store e AdMob. Esse comando habilita a compilação da variante de produção, não publica nem aprova o app.

Os IDs acima são configurações públicas incorporadas em aplicativos, não senhas ou chaves de API.

