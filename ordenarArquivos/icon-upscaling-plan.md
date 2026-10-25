# Plano de Implementação: Otimização de Escala dos Ícones 48x48 (Full-Bleed Proporcional)

## Visão Geral (Overview)
Os ícones do aplicativo foram padronizados no estilo **Duotone Moderno** com contorno espesso e preenchimento translúcido. No entanto, na interface gráfica real (`TelaInicial.fxml`, abas e botões da barra superior), os elementos gráficos estão visualmente pequenos e com áreas vazias excessivas ao redor (padding interno de 6px a 12px dentro do quadro de 48x48 px).

O objetivo deste plano é **redimensionar proporcionalmente todos os ícones para ocuparem a área máxima útil do quadro 48x48**, encostando os limites dos traços na borda do canvas (com margem técnica de segurança de 2.0px a 2.5px para acomodar o anti-aliasing do traço de 4.0px sem corte).

---

## Critérios de Sucesso (Success Criteria)
- [x] **Ocupação Máxima do Quadro 48x48**:
  - Cada ícone foi expandido proporcionalmente até a margem segura de `2.0px` a `2.5px` das bordas externas (largura/altura útil de ~43px a 44px).
- [x] **Preservação Rígida das Proporções (Aspect Ratio)**:
  - Círculos continuam perfeitamente esféricos, e ícones assimétricos/retangulares foram ampliados sem distorção.
- [x] **Harmonia de Espessura de Traço (Stroke Balance)**:
  - Traços sólidos de 3.8px a 4.2px preservando o espaço negativo e a legibilidade.
- [x] **Alinhamento dos Componentes Internos**:
  - Conexão e abertura das pernas das setas no ícone de atualização mantidas intactas.
- [x] **Cobertura Completa (16 Geradores / 26 Arquivos PNG)**:
  - Todos os ícones em `src/main/resources/images/` e `images/alert/` foram regerados com sucesso via `IconGeneratorTest.kt`.

---

## Diagnóstico e Reescalonamento Proporcional por Ícone

| Ícone / Função | Arquivos Gerados | Dimensões Atuais | Novo Fator de Escala / Limites | Ganho Visual |
|---|---|---|---|---|
| **Círculos de Controle (+, -, Confirmar, Cancelar)** | `icoMais`, `icoMenos`, `icoConfirmar`, `icoCancelar`, `alert/btnConfirma` | Diâmetro 36px (margem 6px) | Diâmetro **43px** (centro 24, raio 21.5, margem 2.5px) | **+20% de área** |
| **Engrenagem (Configuração)** | `icoConfiguracao_48.png` | Raio externo 18.2px (diâm. 36.4px) | Raio externo **21.5px** (diâm. 43px, margem 2.5px) | **+18% de área** |
| **Setas de Atualização / Refresh** | `icoAtualizar_48.png`, `icoRecarregarPagina_48.png` | Raio 15px (diâm. 30px) | Raio **19.5px** (diâm. 39px, com pontas das setas em 44.5px) | **+30% de área** |
| **Pasta (Abrir e Próxima Pasta)** | `icoAbrir_48.png`, `icoProximaPasta_48.png` | L: 34.5px, A: 25px (muito vazia) | L: **43.5px** (x: 2.5 a 46.0), A: **32px** (y: 8.0 a 40.0) | **+28% largura, +28% altura** |
| **Setas de Ordenação (Subir e Descer)** | `icoSubir_48.png`, `icoDescer_48.png` | L: 20px, A: 25px (muito estreitas) | L: **34px** (x: 7.0 a 41.0), A: **43px** (y: 2.5 a 45.5) | **+70% largura, +72% altura** |
| **Bandeja de Importação** | `icoBtnImportar_48.png` | L: 30px, A: 28px | L: **43px** (x: 2.5 a 45.5), A: **43px** (y: 2.5 a 45.5) | **+43% de presença** |
| **Lupa (Pesquisar)** | `icoPesquisar_48.png` | Diâm. lente 21px, cabo até 39 | Diâm. lente **25px**, cabo estendido até **(45.5, 45.5)** | **+25% de presença** |
| **Sincronização Cloud (Ondas)** | `icoCompartilhamento*` (4 arquivos) | Raio 25px (restrito a um quadrante) | Raio **36px** (origem em 6, 42 estendendo até 42, 6) | **+44% de alcance** |
| **API / Servidor Nuvem** | `icoApi*` (4 arquivos) | L: 34px, A: 32px | L: **43px** (x: 2.5 a 45.5), A: **42px** (y: 3.0 a 45.0) | **+26% de presença** |
| **Alertas Modais (Triângulo e Balão)** | `alert/icoAlerta`, `icoAviso`, `icoErro`, `icoConfirma` | Altura 33px / Largura 37px | Triângulo base **43px** (x: 2.5 a 45.5, y: 3.5 a 45.0) / Balão **43x36px** | **+25% de presença** |

---

## Fases de Execução

### Fase 1: Recálculo Paramétrico em `IconGeneratorTest.kt`
- Atualizar as fórmulas matemáticas de cada um dos 16 métodos desenhadores no teste:
  1. `desenharIconePastaDuotone` e `desenharIconeProximaPastaDuotone`
  2. `desenharIconeConfiguracaoDuotone`
  3. `desenharIconeAtualizarDuotone` (mantendo a perna interna aberta e conexão precisa)
  4. `desenharIconeCompartilhamentoDuotone`
  5. `desenharIconeMaisDuotone` e `desenharIconeMenosDuotone`
  6. `desenharIconeSubirDuotone` e `desenharIconeDescerDuotone`
  7. `desenharIconeConfirmarDuotone` e `desenharIconeCancelarDuotone`
  8. `desenharIconeImportarDuotone`
  9. `desenharIconePesquisarDuotone`
  10. `desenharIconeApiDuotone`
  11. `desenharIconeAlertaTrianguloDuotone` e `desenharIconePerguntaBalaoDuotone`

### Fase 2: Execução do Teste de Geração
- Executar `mvn test "-Dtest=IconGeneratorTest"` para regerar todos os 26 arquivos `.png`.

### Fase 3: Validação Visual
- Inspecionar via ferramenta visual os ícones regerados para garantir que nenhum traço foi cortado nas bordas de 48x48 e que a legibilidade foi maximizada.
