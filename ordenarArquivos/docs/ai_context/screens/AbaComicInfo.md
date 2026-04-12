# Aba ComicInfo - AbaComicInfoController

## 🎯 Objetivo / Contexto

Tela destinada à leitura, processamento e alteração direta do padrão `.xml` (arquivo `ComicInfo.xml`) dentro de containers compactados (`.cbr`, `.cbz`, `.rar`, `.zip`). Facilita visualizar o mapeamento de marcações de páginas (bookmarks, capítulos e tradução de nome de episódios).

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** `AbaComicInfoController.kt`
- **Layout XML (FXML):** `AbaComicInfo.fxml`
- **Principais Views (IDs):**
  - `txtPastaProcessar`: Campo de texto de origem dos arquivos.
  - `btnCarregar`: Dispara rotina de inspeção dos CBR/CBZs.
  - `tbViewProcessar`: Tabela central contendo checkboxes de "Processado", dados de arquivos de capa, botões emulados nas células (`btnAmazon`, `btnSalvar`) e as linhas para as Tags/Capítulos.
  - `btnTagsNormaliza`, `btnTagsProcessar`, `btnTagsAplicar`: Botões que interagem em lote manipulando as Strings que alimentam a coluna de tags dos Mangás/Capítulos.
  - `btnSalvarTodos`: Reempacota os metadados gerados para dentro dos seus respectivos arquivos `.cbr` originais.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Descompactação Silenciosa:** O `WinrarServices` entra no arquivo compactado, extrai exclusivamente o `ComicInfo.xml` para parsing, ou puxa as páginas marcadas como sumário para rodar o OCR, sem descompactar toda a obra desnecessariamente na memória.
- **Normalização Bilíngue:** Substitui ou organiza marcações de separadores de capítulos em inglês/japonês (`-`, `|`, `SEPARADOR_IMPORTACAO`) processando o arquivo de metadados antes da gravação.
- **Modo de Edição Avançado (Cell):** Aceita atalhos customizados na edição textual das tags para "Deletar linha" (`Shift + Alt + Delete`) ou "Substituir e formatar" (`Shift + Alt + Enter`).
- **Tarefas de Contexto da Tabela:** Permite o controle por linha, deletando registros anteriores da lista, marcando iterações feitas e regravando `.xml` arquivo a arquivo.

## 🔄 Fluxo de Navegação

- **De onde vem:** Embutida na `TelaInicial`.
- **Para onde vai:** Por ser um processador independente, evoca diálogos modais como o `PopupAmazon` para buscar dados de um item específico do grid, e `PopupCapitulos` (acessado pelo botão `#btnCapitulos`).

## 🌍 Atalhos / Dicionário (Referência)

- `Shift + Alt + Enter`: Converte o delimitador e encerra a edição da Tag na Grid.
- `Shift + Alt + Delete`: Deleta a linha correspondente a célula que está em foco na caixa de texto.
