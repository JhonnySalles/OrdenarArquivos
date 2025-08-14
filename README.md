# Ordenar Arquivos
> Programa para realizar a ordenação de arquivos no sistema, movendo-os em uma sequencia de pastas previamente informada.

<h4 align="center"> 
	🛰  Versão 0.0.1
</h4>

[![Build Status][travis-image]][travis-url]

Programa para realizar a organização de arquivos de uma pasta informada, separando e organizando em uma estrutura de pasta pré-definida.

<p align="center">
 <a href="#Sobre">Sobre</a> •
 <a href="#Ambiente-de-Desenvolvimento">Ambiente de Desenvolvimento</a> • 
 <a href="#Histórico-de-Release">Histórico de Release</a> • 
 <a href="#Features">Features</a> • 
 <a href="#Meta">Meta</a> • 
 <a href="#Contribuindo">Contribuindo</a> • 
 <a href="#Licença">Licença</a>
</p>

## Sobre

Projetado para listar os arquivos de uma pasta informada e mover esses arquivos para outra pasta, sendo agrupado conforme uma certa quantidade informada.

## Ambiente de Desenvolvimento

Necessário realizar préviamente a instalação do [java na versão 20](https://www.java.com/pt-BR/).
Pode-se utilizar qualquer IDE de desenvolvimento de sua escolha, recomenda-se a versão 2020-12 do [eclipse](https://www.eclipse.org/downloads/).
Utilizado no projeto o mavem para controle e gerenciamento das dependências.

## Histórico de Release

* 0.0.1
    * Desenvolvido e compilado a primeira versão
* 0.0.3
    * Nova funcionalidade de ordenação
* 1.0.0
    * Migração do projeto para Intellij
    * Primeira versão estável
* 1.0.1
    * Migração do projeto para kotlin
* 1.0.2
    * Integração com o firebase par compartilhamento do banco de dados de arquivos processados
    * Adicionado método de incremento de capítulos
    * Adicionado de proximos capítulos conforme média do volume anterior
* 1.0.3
    * Miniaturas das capas como preview
    * Método de copia de dados de epub opf
* 1.0.4
    * Integração de consulta com o MyAnimeList
    * Implementação de leitura via OCR para o sumário com tesseract e opencv
    * Adição de sugestão para os capitulos de dados do OCR
    * Refatorado código e pom
* 1.0.5
    * Implementação de geração do layout xml ComicInfo
    * Implementação de rotina de teste
    * Implementação de aba para processamento do ComicInfo
* 1.0.6
    * Implementação de geração do layout xml CoMet
    * Implmentação de sugestões do sumário via Gemini
    * Implementação de aba para renomear pastas de capítulos e volume

### Features

- [X] Compartilhamento do banco de dados via Firebase
- [X] Apresentação de prévia das capas
- [X] Geração de capítulos de forma inteligênte
- [X] Integração com o MyAnimeList
- [X] Gerar xml com dados no formato ComicInfo
- [X] Gerar xml com dados no formato CoMet
- [X] Implementação de rotinas de teste
- [X] Impementação de OCR no sumário
- [X] Sugestão de capítulos a partir da seleção do sumário
- [X] Integração com o Gemini para sugestão dos capítulos
- [X] Ajustes em pastas de capítulos conforme dados de volume interno no banco de dados

## Meta

Distribuido sobre a licença GPL. Veja o arquivo ``COPYING`` para maiores informações.
[https://github.com/JhonnySalles/github-link](https://github.com/JhonnySalles/TwiterBot/blob/master/COPYING)

## Contribuindo

1. Fork (<https://github.com/JhonnySalles/TwiterBot/fork>)
2. Crie sua branch de recurso (`git checkout -b feature/fooBar`)
3. Faça o commit com suas alterações (`git commit -am 'Add some fooBar'`)
4. Realize o push de sua branch (`git push origin feature/fooBar`)
5. Crie um novo Pull Request

<!-- Markdown link & img dfn's -->

## Licença

[GPL-3.0 License](https://github.com/JhonnySalles/TwiterBot/blob/master/COPYING)
