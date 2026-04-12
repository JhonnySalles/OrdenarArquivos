# Visão Geral da Arquitetura - OrdenarArquivos

## 🏗️ Padrão e Tecnologias

O ecossistema é inteiramente desenhado no padrão MVC (`Model-View-Controller`), voltado para aplicativos Desktop Modernos e orientados a Eventos/Threads, impulsionado por Kotlin e UI baseada em JavaFX.

- **UI / Interface Gráfica:** **JavaFX** orquestrado pela biblioteca **JFoenix** (Componentes Material Design como `JFXTextField`, `JFXDialog`, `JFXButton`, etc.) e controle adicional do **ControlsFX**.
- **Linguagem / Core:** Kotlin (fortemente ancorado no suporte de Data Classes e POJOs).
- **Persistência (Banco de Dados):** SQLite (`jdbc:sqlite`), abstraído na estrutura gerencial da classe singleton `DataBase` e serviços atrelados (Manga, Historico).
- **UI Toolkit (Estilização):** Layouts mantidos em `.fxml` (gerados no SceneBuilder) acoplados aos `.css` próprios (Ex: `Dark_TelaInicial.css`).
- **Testes e Automações:** Interface coberta por Unit Tests de UI acionados pelo `TestFX` com asserts baseados em robôs UI (Ex: `FxRobot`), instâncias e rotinas `Mockito`.

## 📂 Estrutura de Diretórios Primários

- `src/main/kotlin/com/fenix/ordenararquivos/controller`: Controladores diretos de telas e popups.
- `src/main/kotlin/com/fenix/ordenararquivos/model/entities`: POJOs refletindo metadados ou tabelas (`Manga`, `ComicInfo`, `Mal`, `Historico`, `Processar`).
- `src/main/kotlin/com/fenix/ordenararquivos/service`: Camada inteligente que processa as regras de negócio (Integrações MAL/Amazon, manipulação de CBR `WinrarServices`, `OcrServices`).
- `src/main/kotlin/com/fenix/ordenararquivos/notification`: Instâncias modulares visuais (`AlertasPopup` usando `BoxBlur`, `Notificacoes` usando _Timeline/TranslateTransition_).

## 🧠 Core Features da Aplicação

1. **Tratamento de Comic Info:** O app varre e processa sem corromper arquivos empacotados (`.cbr`/`.cbz`) injetando tags com nomenclaturas unificadas e IDs de referências das bases da Web (via extração `WinRar`).
2. **Serviços de Extensão de Metadados:** Buscas transparentes conectadas através das classes de dados das APIs da Amazon e MyAnimeList (via MAL4J).
3. **Machine Learning Integrado (OCR):** Implementação robusta do `Tesseract` voltada pra captura de textos originados nos sumários (mangás) auxiliando em traduções massivas de episódios sem input manual.
4. **Scraping Dinâmico de Web:** Parser poderoso em HTML (`Jsoup`) atuando de forma inteligente via `PopupCapitulos` consumindo as diretrizes assíncronas do DOM dos maiores sites do nicho (MangaDex, MangaPlanet, ZBato).

## 🤝 Tratamento de Concorrência e UI Threads

Como é mandatório no JavaFX, as rotinas que englobam descompressão de dezenas de `cbr/cbz`, requisições na Web, Scraping e uso do OCR, são executadas fora da `Thread do JavaFX` delegando à implementações em Backgound como as rotinas internas do sistema monitoradas pelo painel de `Progresso Global` atrelado no Controller Primário.

## 🎨 Padrão de Modais "Imersivos"

As chamadas interativas de "Erro", "Aviso" e modais customizados invocam a entidade assíncrona `AlertasPopup`. O sistema força o escurecimento natural com distorção através da propriedade de FX `BoxBlur` injetando nas views as janelas através dos frames do `StackPane`.
