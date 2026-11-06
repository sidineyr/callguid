# CallGuide Desktop 2.2.0

Aplicativo gratuito e offline de escrita cursiva para **Windows, Linux e macOS**. Digite palavras e nomes, observe a animação e pratique com **mouse ou caneta de mesa digitalizadora**.

## Downloads e instalação

Os pacotes estão na [release CallGuide Desktop](https://github.com/sidineyr/callguid/releases/tag/desktop-v2.2.0). O diretório `desktop/` preserva o código e estas instruções. Não é necessário instalar Node.js ou Electron para usar os pacotes prontos.

| Sistema | Pacote | Como abrir |
|---|---|---|
| Windows x64 | `CallGuide-Desktop-2.2.0-windows-x64.zip` | Extraia a pasta inteira e abra `CallGuide.exe`. |
| Linux x64 | `CallGuide-Desktop-2.2.0-linux-x64.tar.gz` | Extraia a pasta inteira e execute `./callguide` dentro dela. |
| macOS Intel | `CallGuide-Desktop-2.2.0-macos-x64.zip` | Extraia e mova `CallGuide.app` para Aplicativos. |
| macOS Apple Silicon | `CallGuide-Desktop-2.2.0-macos-arm64.zip` | Extraia e mova `CallGuide.app` para Aplicativos. |

Mantenha os arquivos de cada pacote juntos. A versão Windows é portátil, sem instalador. Os pacotes não possuem assinatura comercial; macOS recebe assinatura ad-hoc para integridade local, sem notarização Apple. O sistema pode bloquear a abertura; não desative as proteções de segurança. Em equipamentos administrados, peça autorização ao responsável. Não há atualizações automáticas.

## Mouse e mesa digitalizadora

1. Prepare uma palavra e escolha **Praticar**.
2. Em **Entrada**, escolha Automática, Mouse ou Mesa / caneta.
3. Desenhe no papel. No modo Mesa / caneta, o mouse não desenha.
4. Ative **Pressão da caneta** para variar a espessura quando o dispositivo e seu driver fornecerem esse dado. Com mouse ou pressão desativada, a espessura é constante.

A mesa deve estar conectada e configurada com o driver do fabricante no sistema operacional. Não há pareamento dentro do CallGuide, conexão WebHID ou suporte direto a protocolos de fabricantes. A compatibilidade depende de o sistema apresentar a caneta como evento de ponteiro. Pressão física e modelos específicos precisam ser conferidos em uma mesa real.

## Recursos

- 52 letras, acentos portugueses, ç, números, espaços e pontuação básica.
- Nomes e frases curtas de até 32 caracteres, animados com pausa, repetição e velocidade ajustável.
- Modelo para traçar, numeração dos movimentos, ampliação e deslocamento horizontal.
- Cores, espessura, borracha, desfazer, limpar e exportação do papel em PNG.
- Até 12 palavras favoritas guardadas apenas neste computador.
- Conteúdo original inspirado na **Sidiney Caligrafia Refinada** e na escrita com caneta esferográfica **BIC**.

[Baixar a fonte TTF](https://github.com/sidineyr/callguid/raw/80bf6f2d79ac7f85c2d7e68b4855ecf85211bd16/fonts/sidiney-caligrafia/SidineyCaligrafia-Refinada.ttf) · [Artigo do autor no Substack](https://substack.com/home/post/p-223397084)

## Privacidade

Sem conta, anúncios, telemetria ou envio de palavras/desenhos. As favoritas ficam no armazenamento local e podem ser excluídas pela interface. Os desenhos não são salvos automaticamente; o botão Salvar PNG cria um arquivo escolhido pelo usuário. Não existe nota ou correção automática da escrita.

## Código e validação

Execute `node --test geometry.test.js ink.test.js` para os testes sem dependências. `package-desktop.py` baixa um runtime oficial, valida seu SHA-256 e gera pacotes; veja [PACKAGING.md](PACKAGING.md). O workflow gera cada plataforma em seu sistema nativo, com testes de fumaça quando executáveis na arquitetura do runner. Esses testes usam ponteiros sintéticos e não substituem ensaio com hardware de mesa digitalizadora.

O código usa Electron com renderer isolado, sandbox, Node.js desativado no renderer e bloqueio de navegação/requisições remotas. Bibliotecas e avisos de licença do runtime acompanham cada pacote.

Suporte: **sidiney@edu.univali.br**. Licença MIT. Arte do pequeno príncipe indígena imaginário criada com IA.
