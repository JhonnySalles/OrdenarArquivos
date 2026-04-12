# Aba Pastas - AbaPastasController

## 🎯 Objetivo / Contexto

Tela com foco direto no processamento e tratamento em lote de múltiplas pastas contendo imagens de mangás. Através de expressões regulares (Regex), extrai automaticamente informações do nome das pastas e aplica o renomeio maciço, permitindo que fiquem no formato final padrão antes de serem transformados em `.cbz` ou `.cbr`.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** `AbaPastasController.kt`
- **Layout XML (FXML):** `AbaPastas.fxml` (associado logicamente à raiz do sistema)
- **Principais Views (IDs):**
  - `txtPasta`: Onde o usuário informa ou seleciona o diretório pai.
  - `btnCarregar`: Dispara o mapeamento das subpastas na View.
  - `tbViewProcessar`: Tabela central (`TableView<Pasta>`) que armazena as entidades de pastas extraídas, com colunas ativas para duplo clique.
  - `cbManga`: `JFXComboBox` usado para identificar o escopo principal do projeto para adicionar nos metadados ou renomear as saídas baseando-se no título global.
  - `btnAplicar`: Ação que executa a transição ou renomeio em disco.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Extração por Regex:** Detecta automaticamente a estrutura do nome, puxando informações vitais como a _Scan_, número do _Volume_, _Capítulo_ e _Título_ da obra.
- **Edição Dinâmica Inline:** O usuário tem liberdade de ajustar com duplo clique (`edit()`) células específicas da `tbViewProcessar`, onde as alterações são processadas para uma pre-visualização instantânea na coluna final formatada.
- **Propagação de Dados via ContextMenu:** Funções de clique-direito permitem injetar o nome de uma "Scan" para todos os itens seguintes, anteriores, ou zerar a estrutura de volumes.
- **Integração de Base:** Auto-sugere o nome dos mangás no ComboBox, varrendo o repositório do banco de dados na digitação.

## 🔄 Fluxo de Navegação

- **De onde vem:** Embutida na `TelaInicial`.
- **Para onde vai:** Reflete suas lógicas interativamente no filesystem, renomeando arquivos ou extraindo dados da base de metadados sem alternar o layout global.

## 🌍 Atalhos / Dicionário (Referência)

- **Atalhos Globais:** `Ctrl + D` aciona o retorno/volta para a `AbaArquivos`.
- **Itens de Menu:** "Aplicar scan nos arquivos próximos", "Zerar volumes".
