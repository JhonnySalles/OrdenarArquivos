# Plano de Melhoria: Descoberta DNS (mDNS) e Ordenação na API

## Visão Geral (Overview)
Ajustar e aprimorar a integração do cliente desktop (`ordenarArquivos`) com a API `ReaderServer`, incorporando as atualizações recentes realizadas no servidor:
1. **Descoberta de Rede (mDNS/DNS-SD)**: O servidor agora registra o serviço dedicado `_readerserver._tcp.local.` com atributos TXT (`version`, `path`, `name`), além de expor a versão no endpoint `/health`.
2. **Consulta e Ordenação na API**: O endpoint `/api/comicinfo/search/advanced` agora suporta os parâmetros de paginação e ordenação no banco de dados (`sortField` e `direction`), garantindo que os resultados já venham ordenados do maior para o menor volume diretamente da consulta ao banco.

---

## Tipo do Projeto (Project Type)
**DESKTOP (JavaFX + Kotlin + Maven)**

---

## Critérios de Sucesso (Success Criteria)
- [x] O cliente descobre a API primariamente via serviço `_readerserver._tcp.local.` no mDNS, mantendo compatibilidade com `_http._tcp.local.` como fallback.
- [x] O cliente extrai a versão da API tanto via registros TXT do mDNS quanto no retorno JSON de `/health`.
- [x] O status do botão da API (`btnApi`) exibe a versão conectada no tooltip (ex: `ReaderServer API v1.0.0: Conectado (http://192.168.1.13:8083)`).
- [x] As requisições de busca chamam `/api/comicinfo/search/advanced` incluindo `sortField=volume&direction=desc`, trazendo os volumes mais recentes no topo mesmo com paginação/limite de registros.
- [x] Ordenação secundária por número de capítulo (`number desc`) mantida em memória para volumes idênticos.
- [x] Todos os testes unitários (`ReaderApiServicesTest`) passam com 100% de sucesso.

---

## Stack Tecnológica (Tech Stack)
- **Kotlin 1.9+** & **Java 17 (OpenJFX 21)**
- **JmDNS 3.5.9**: Descoberta multicast DNS / ZeroConf.
- **Java 11 HttpClient (`java.net.http`)**: Comunicação HTTP/REST assíncrona/síncrona nativa.
- **Org.JSON**: Parsing de respostas REST da API.
- **JFoenix & JavaFX**: Interface do usuário e feedback visual.

---

## Estrutura de Arquivos Impactados (File Structure)
```
ordenarArquivos/
├── src/main/kotlin/com/fenix/ordenararquivos/
│   ├── service/
│   │   └── ReaderApiServices.kt         # Atualização mDNS (_readerserver), parsing version, sortField/direction
│   └── controller/
│       ├── TelaInicialController.kt     # Tooltip enriquecido com versão da API
│       └── PopupBuscaApiController.kt   # Ordenação parametrizada e feedback de busca
└── src/test/kotlin/com/fenix/ordenararquivos/
    └── service/
        └── ReaderApiServicesTest.kt     # Testes de query params de ordenação e versão
```

---

## Detalhamento de Tarefas (Task Breakdown)

### Tarefa 1: Upgrade da Descoberta mDNS e Captura de Versão
- **ID**: `DNS-01`
- **Status**: [x] Concluído

### Tarefa 2: Integração de Ordenação no Backend (`sortField` e `direction`)
- **ID**: `SORT-01`
- **Status**: [x] Concluído

### Tarefa 3: Atualização da UI com Informações de Versão e Ordenação
- **ID**: `UI-01`
- **Status**: [x] Concluído

### Tarefa 4: Atualização dos Testes Unitários
- **ID**: `TEST-01`
- **Status**: [x] Concluído

---

## Fase X: Checklist de Verificação Final (Phase X)
- [x] Compilação limpa sem warnings impeditivos: `mvn test-compile`
- [x] Execução dos testes unitários: `mvn test -Dtest=ReaderApiServicesTest` (3/3 testes passaram)
- [x] Verificação de compatibilidade com servidor local rodando em `http://localhost:8083`
- [x] Nenhuma violação de convenções de código limpo (Clean Code)

## ✅ PHASE X COMPLETE
- Testes: ✅ 3/3 Passaram (`ReaderApiServicesTest`)
- Descoberta mDNS: ✅ Suporte a `_readerserver._tcp.local.` e `_http._tcp.local.` com extração de versão
- Ordenação Backend: ✅ Query params `sortField=volume` e `direction=desc` aplicados
- Build: ✅ Sucesso (BUILD SUCCESS)
- Data: 2026-10-24
