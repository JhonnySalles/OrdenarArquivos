# Popup Capítulos (Web Scraping) - PopupCapitulos

## 🎯 Objetivo / Contexto

Painel em modal (Popup Blur) com a finalidade exclusiva de realizar o mapeamento (Scraping HTML) ou importação dos títulos, números e volumes de capítulos extraídos de portais da web, agilizando o acervo de tags.

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** `PopupCapitulos.kt`
- **Layout XML (FXML):** `PopupCapitulos.fxml`
- **Principais Views (IDs):**
  - `txtEndereco`: Campo do tipo Textfield que aceita inserção em texto (URL) de site, ou receber evento nativo de **Drag & Drop** (arrastar um HTML da máquina para dentro dele).
  - `hplMangaPlanet`, `hplComick`, `hplTaiyo`, `hplMangaFire` etc.: Hyperlinks predefinidos que o usuário pode utilizar para consultar a lista.
  - `cbLinguagem`: Determina qual modelo de linguagem a extração deverá preferir mapear (Inglês, Português, Japonês).
  - `btnExecutar`: Executa o parser via Jsoup.
  - `tbViewTabela`: Tabela central baseada no POJO `Volume`, que lista os capítulos localizados.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Web Scraping Condicional:** O sistema analisa a string no `txtEndereco`. Se detectar um host conhecido (`mangaplanet.com`, `mangadex.org`, `taiyo.moe`, etc.), delega para sua respectiva função de extração especializada (ex: `extractTayo()`, `extractMangaDex()`).
- **Extração Regex Local:** Suporta processamento do arquivo offline (arquivos HTML do disco) para sites restritos.
- **Deduplicação de Entradas:** Regras especializadas que evitam duplicação de capítulos e resolvem extrações com títulos vazios ou em conflitos de versões de idioma na tabela HTML extraída.
- **Preparação Callback:** Ao confirmar, serializa o mapeamento extraído na formatação correta do projeto (`00.## | Titulo`), gerando as listas separadas por volumes para o `Controller` pai.

## 🔄 Fluxo de Navegação

- **De onde vem:** Evocada via janela de contexto pelo método estático `PopupCapitulos.abreTelaCapitulos` passando o `rootStackPane` das telas filhas (`AbaComicInfo` / `AbaArquivo`).
- **Para onde vai:** Não avança para outras telas; após preenchido, os dados retornam em via de `Callback` de ObservableList e o diálogo modal animado sofre fechamento.

## 🌍 Dicionário (Referência)

- A classe CSS usada no modal: `.dialog-black`, `.texto-stilo-1`.
- Mapeamento de DOM Elements: `span.text-foreground`, `li.list-group-item` etc (internos do Jsoup).
- Suporte Drag & Drop: Aceita eventos `handleDragOver`, `handleDragDropped`.
