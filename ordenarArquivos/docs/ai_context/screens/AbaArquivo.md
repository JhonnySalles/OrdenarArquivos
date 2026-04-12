# Aba Arquivo - AbaArquivoController

## 🎯 Objetivo / Contexto

Tela responsável por processar e organizar arquivos individuais de mangás/comics. Permite renomear itens em lote, gerar a estrutura numérica de capítulos, manipular metadados do arquivo `ComicInfo.xml` (através de buscas automáticas na Amazon ou MyAnimeList - MAL), importar histórico de edições e realizar manipulação e ajuste fino de imagens de capa (Frente/Trás).

## 🧩 Componentes de UI e Arquivos

- **Classe Principal:** `AbaArquivoController.kt`
- **Layout XML (FXML):** `AbaArquivo.fxml`
- **Principais Views (IDs):**
  - `txtPastaOrigem` e `txtPastaDestino`: Campos de texto para indicar o roteamento dos arquivos.
  - `txtNomePastaManga`, `txtVolume`, `txtNomeArquivo`: TextFields do `JFoenix` para lidar com a formatação e geração dos nomes de saída.
  - `tbTabRootArquivo`: `JFXTabPane` que encapsula as lógicas de subdivisão: `Arquivos`, `ComicInfo` e `Capas`.
  - `txtAreaImportar`: `JFXTextArea` que recebe o conteúdo de capítulos importados ou digitados manualmente.
  - `tbViewTabela`: Tabela (`TableView`) que armazena os arquivos enfileirados ou pastas a sofrerem ação de renomeio e organização.
  - `cbOcrSumario`, `cbMesclarCapaTudo`, `cbCompactarArquivo`: `JFXCheckBox` para habilitar rotinas e fluxos assíncronos.
  - Botões Globais: `btnProcessar`, `btnAjustarNomes`, `btnCompactar`, `btnGerarCapa`.

## ⚙️ Regras de Negócio e Lógica (Core Logic)

- **Geração Automática de Capítulos:** A partir de campos como Início/Fim e com botões de incremento de Volume (+ / -), o sistema compila strings formatadas para os novos capítulos.
- **Buscador de Metadados (ComicInfo):** Dispara `Tasks` que se comunicam com a API da Amazon e do MyAnimeList. Ao preencher via MAL (ID ou Nome), injeta nas views informações como: `AgeRating`, `Series`, `Publisher` e `Title`.
- **Ajuste de Capas e OCR:** Manipula sliders de margem (`sliderFrente`, `sliderTras`) para apagar bordas brancas. A função de OCR inspeciona páginas marcadas como sumário usando Tesseract para sugerir os títulos japoneses ou inglês aos episódios (`JFXAutoCompletePopup`).
- **Rotinas Async:** Todo processamento de arquivos é assíncrono para evitar o travamento da thread gráfica (UI), disparando o carregamento da barra de progresso no `Controller Pai`.

## 🔄 Fluxo de Navegação

- **De onde vem:** Embutida através do `JFXTabPane` global na tela primária (`TelaInicial`).
- **Para onde vai:** Consegue chamar modais (via `StackPane` e `BoxBlur`), como `PopupCapitulos` (ao acionar importação em lote de links da internet) e o `PopupAmazon`.

## 🌍 Atalhos / Dicionário (Referência)

- **Atalhos Globais:**
  - `Ctrl + Espaço`: Aciona o processamento principal dos arquivos em lote.
  - `Ctrl + M`: Foca na área de importação.
  - `Ctrl + S`: Solicita sugestão via OCR do Sumário selecionado.
  - `Ctrl + O`: Ordena as linhas preenchidas na área de texto.
  - `Ctrl + E`: Alterna extra no título do capítulo (Toggle).
  - `Ctrl + T`: Limpa as tags.
  - `Ctrl + W`: Abre consulta do MyAnimeList (MAL).
  - `Ctrl + P` ou `Ctrl + Enter`: Grava `ComicInfo`.
- **Classes Visuais:** `.texto-stilo-1`, `.background-Black3`, `.background-Indigo1`.
