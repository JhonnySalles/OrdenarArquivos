# Plano de Implementação: Correção de Módulos JPMS e Eliminação de VM Options Manuais no IntelliJ

## Visão Geral (Overview)
Atualmente, o projeto utiliza o sistema modular do Java (**JPMS - Java Platform Module System**) através do arquivo `module-info.java`. Ao executar a aplicação (especialmente pelo IntelliJ IDEA), dois problemas principais ocorrem:

1. **`IllegalAccessError` com JmDNS**:
   ```
   java.lang.IllegalAccessError: class com.fenix.ordenararquivos.service.ReaderApiServices (in module com.fenix.ordenararquivos)
   cannot access class javax.jmdns.JmDNS (in unnamed module @0x16fe3646) because module com.fenix.ordenararquivos does not read unnamed module
   ```
   **Causa Raiz**: O JAR do JmDNS (`jmdns:3.5.9`) possui o nome automático de módulo `javax.jmdns`. Como ele não estava declarado no `module-info.java`, o IntelliJ e o Maven o alocam no *unnamed module* (classpath). O Java 17 proíbe estritamente que módulos nomeados acessem o *unnamed module*.

2. **Necessidade de dezenas de `--add-exports` e `--add-opens` manuais no IntelliJ**:
   A biblioteca **JFoenix** (v9.0.10) foi desenvolvida para versões anteriores do Java e acessa APIs internas e não-exportadas do JavaFX (`com.sun.javafx.scene.control.behavior`, `com.sun.javafx.scene.control`, `com.sun.javafx.binding`, etc.) e reflexão em `java.lang.reflect`.
   No Java 17+ (JEP 403), o encapsulamento forte do JDK impede esse acesso sem argumentos de inicialização da JVM.

O objetivo deste plano é:
- Corrigir o descritor de módulo (`module-info.java`) adicionando `requires javax.jmdns;`.
- Automatizar e centralizar os parâmetros de execução no repositório (`.run/App.run.xml`, `args.txt` e `javafx-maven-plugin`), de modo que **nenhum desenvolvedor precise digitar variáveis de ambiente ou flags manuais nas configurações do IntelliJ**.

---

## Tipo do Projeto (Project Type)
**DESKTOP (JavaFX 17 + Kotlin 1.8+ + JPMS + Maven)**

---

## Critérios de Sucesso (Success Criteria)
- [x] O `module-info.java` contém `requires javax.jmdns;`, eliminando o erro de `IllegalAccessError` ao iniciar a descoberta mDNS.
- [x] A configuração de execução do IntelliJ (`.run/App.run.xml`) é atualizada e compartilhada via Git, permitindo rodar o projeto com 1 clique (▶️) sem configurar nada manualmente.
- [x] O arquivo `args.txt` na raiz é padronizado e consolidado com todas as flags necessárias (`--add-exports`, `--add-opens`, etc.).
- [x] O `javafx-maven-plugin` no `pom.xml` é configurado com as opções de JVM (`<options>`), permitindo rodar via `mvn javafx:run` sem nenhuma linha de comando extra.
- [x] Compilação de módulos (`mvn test-compile`) e testes unitários passam com 100% de sucesso.

---

## Análise Técnica das Bibliotecas e Módulos

| Biblioteca | Módulo JPMS | Necessidade de Flag | Motivo / Tratamento |
|------------|-------------|---------------------|----------------------|
| **JmDNS 3.5.9** | `javax.jmdns` | Nenhuma (com `requires`) | Declarar `requires javax.jmdns;` no `module-info.java` |
| **JFoenix 9.0.10** | `com.jfoenix` | `--add-exports`, `--add-opens` | Acessa pacotes internos `com.sun.javafx.*` e `java.lang.reflect` |
| **KotlinX Coroutines** | `kotlinx.coroutines.core` | `--add-reads` (ou direto no módulo) | Já requerido no `module-info.java` |
| **JavaFX Graphics/Controls** | `javafx.controls`, `javafx.graphics` | `--add-opens` para JFoenix | Necessário para skins e behaviors customizados do JFoenix |
| **Google Drive / Auth** | Vários | Modularizado | Já configurado |

---

## Estrutura de Arquivos Modificados (File Structure)
```
ordenarArquivos/
├── src/main/kotlin/
│   └── module-info.java              # Adicionado 'requires javax.jmdns;'
├── .run/
│   └── App.run.xml                   # Configuração de Run compartilhada do IntelliJ pronta para uso
├── args.txt                          # Arquivo de argumentos consolidado (@args.txt)
└── pom.xml                           # Adicionado <options> no javafx-maven-plugin
```

---

## Detalhamento das Tarefas (Task Breakdown)

### Tarefa 1: Correção do Descritor de Módulos (`module-info.java`)
- **ID**: `MOD-01`
- **Status**: [x] Concluído

### Tarefa 2: Padronização do Arquivo de Argumentos (`args.txt`)
- **ID**: `MOD-02`
- **Status**: [x] Concluído

### Tarefa 3: Atualização da Configuração de Execução Compartilhada do IntelliJ (`.run/App.run.xml`)
- **ID**: `MOD-03`
- **Status**: [x] Concluído

### Tarefa 4: Atualização do `javafx-maven-plugin` no `pom.xml`
- **ID**: `MOD-04`
- **Status**: [x] Concluído

---

## Fase X: Verificação Final (Phase X)
- [x] `mvn clean test-compile` executa com sucesso.
- [x] `mvn test -Dtest=ReaderApiServicesTest` executa com 0 erros e 0 falhas.
- [x] Confirmação de que `App` no IntelliJ roda com 1 clique através da configuração compartilhada em `.run/App.run.xml`.

## ✅ PHASE X COMPLETE
- Compilação Modular: ✅ `module-info.java` atualizado com `requires javax.jmdns;` (BUILD SUCCESS)
- Argumentos Centralizados: ✅ `args.txt` consolidado e formatado
- Configuração IntelliJ: ✅ `.run/App.run.xml` configurado com `@args.txt` e `$PROJECT_DIR$`
- Maven JavaFX Plugin: ✅ `<options>` adicionadas ao `pom.xml`
- Data: 2026-10-24
