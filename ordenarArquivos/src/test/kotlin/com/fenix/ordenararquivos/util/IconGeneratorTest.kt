package com.fenix.ordenararquivos.util

import org.junit.jupiter.api.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

class IconGeneratorTest {

    private val outputDir = File("src/main/resources/images")
    private val alertDir = File(outputDir, "alert")

    @Test
    fun gerarIconesPadronizados() {
        if (!outputDir.exists()) outputDir.mkdirs()
        if (!alertDir.exists()) alertDir.mkdirs()

        // Paleta oficial de tokens de design do projeto
        val corCiano = Color(0, 160, 233)     // Ciano elétrico #00A0E9 (Neutro / Ação)
        val corVerde = Color(118, 192, 67)     // Verde folha #76C043 (Sucesso / Conectado / Adicionar)
        val corLaranja = Color(245, 158, 11)   // Laranja âmbar #F59E0B (Espera / Alerta)
        val corVermelho = Color(229, 62, 62)   // Vermelho #E53E3E (Erro / Cancelar / Remover)

        // 1. Conjunto API Duotone (4 estados)
        salvarIcone("icoApiDesconectado_48.png", desenharIconeApiDuotone(corCiano))
        salvarIcone("icoApiConectado_48.png", desenharIconeApiDuotone(corVerde))
        salvarIcone("icoApiEspera_48.png", desenharIconeApiDuotone(corLaranja))
        salvarIcone("icoApiErro_48.png", desenharIconeApiDuotone(corVermelho))

        // 2. Configuração, Atualização e Sincronização Cloud Duotone
        salvarIcone("icoConfiguracao_48.png", desenharIconeConfiguracaoDuotone(corCiano))
        salvarIcone("icoAtualizar_48.png", desenharIconeAtualizarDuotone(corCiano))
        salvarIcone("icoRecarregarPagina_48.png", desenharIconeAtualizarDuotone(corCiano))

        // Conjunto de Sincronização Cloud Duotone (4 estados - Opção 2 aprovada)
        salvarIcone("icoCompartilhamento_48.png", desenharIconeCompartilhamentoDuotone(corCiano))
        salvarIcone("icoCompartilhamentoEnvio_48.png", desenharIconeCompartilhamentoDuotone(corVerde))
        salvarIcone("icoCompartilhamentoEspera_48.png", desenharIconeCompartilhamentoDuotone(corLaranja))
        salvarIcone("icoCompartilhamentoErro_48.png", desenharIconeCompartilhamentoDuotone(corVermelho))

        // 3. Pastas Duotone (Abrir e Próxima Pasta)
        salvarIcone("icoAbrir_48.png", desenharIconePastaDuotone(corCiano))
        salvarIcone("icoProximaPasta_48.png", desenharIconeProximaPastaDuotone(corCiano))

        // 4. Controles de Volume / Lista Duotone (+ e -)
        salvarIcone("icoMais_48.png", desenharIconeMaisDuotone(corVerde))
        salvarIcone("icoMenos_48.png", desenharIconeMenosDuotone(corCiano))

        // 5. Ordenação Duotone (Subir e Descer)
        salvarIcone("icoSubir_48.png", desenharIconeSubirDuotone(corCiano))
        salvarIcone("icoDescer_48.png", desenharIconeDescerDuotone(corCiano))

        // 6. Confirmação e Cancelamento Duotone
        salvarIcone("icoConfirmar_48.png", desenharIconeConfirmarDuotone(corVerde))
        salvarIcone("icoCancelar_48.png", desenharIconeCancelarDuotone(corVermelho))

        // 7. Importação e Busca Duotone
        salvarIcone("icoBtnImportar_48.png", desenharIconeImportarDuotone(corCiano))
        salvarIcone("icoPesquisar_48.png", desenharIconePesquisarDuotone(corCiano))

        // 8. Ícones de Alerta Duotone (Pasta alert/)
        salvarIconeAlert("icoAlerta_48.png", desenharIconeAlertaTrianguloDuotone(corLaranja))
        salvarIconeAlert("icoAviso_48.png", desenharIconeAlertaTrianguloDuotone(corCiano))
        salvarIconeAlert("icoErro_48.png", desenharIconeAlertaTrianguloDuotone(corVermelho))
        salvarIconeAlert("icoConfirma_48.png", desenharIconePerguntaBalaoDuotone(corCiano))
        salvarIconeAlert("btnConfirma_48.png", desenharIconeConfirmarDuotone(corVerde))

        println("Todos os ícones Duotone foram gerados com sucesso em: ${outputDir.absolutePath}")
    }

    // --- Ícone de Pasta Duotone (Abrir Diretório) ---
    private fun desenharIconePastaDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        val corpo = Path2D.Double().apply {
            moveTo(5.5, 6.0)
            lineTo(18.0, 6.0)
            lineTo(22.0, 11.0)
            lineTo(42.5, 11.0)
            quadTo(45.5, 11.0, 45.5, 14.0)
            lineTo(45.5, 42.0)
            quadTo(45.5, 45.0, 42.5, 45.0)
            lineTo(5.5, 45.0)
            quadTo(2.5, 45.0, 2.5, 42.0)
            lineTo(2.5, 9.0)
            quadTo(2.5, 6.0, 5.5, 6.0)
            closePath()
        }

        // Fundo translúcido Duotone (~22% opacidade)
        g2.color = Color(cor.red, cor.green, cor.blue, 55)
        g2.fill(corpo)

        // Contorno com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(corpo)

        // Divisória frontal horizontal da pasta com traço forte
        g2.stroke = BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(4, 21, 44, 21)

        g2.dispose()
        return img
    }

    // --- Ícone de Próxima Pasta Duotone (Pasta com Seta de Avanço) ---
    private fun desenharIconeProximaPastaDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        val pasta = Path2D.Double().apply {
            moveTo(5.0, 7.0)
            lineTo(14.0, 7.0)
            lineTo(17.0, 11.0)
            lineTo(28.0, 11.0)
            quadTo(30.5, 11.0, 30.5, 13.5)
            lineTo(30.5, 40.5)
            quadTo(30.5, 43.0, 28.0, 43.0)
            lineTo(5.0, 43.0)
            quadTo(2.5, 43.0, 2.5, 40.5)
            lineTo(2.5, 9.5)
            quadTo(2.5, 7.0, 5.0, 7.0)
            closePath()
        }

        // Fundo translúcido na pasta
        g2.color = Color(cor.red, cor.green, cor.blue, 55)
        g2.fill(pasta)

        // Contorno da pasta em traço forte
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(pasta)
        g2.drawLine(3, 20, 30, 20)

        // Seta de avanço destacada na direita com traço de 4.2px
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(22, 27, 44, 27)

        val setaPonta = Path2D.Double().apply {
            moveTo(35.0, 18.0)
            lineTo(44.5, 27.0)
            lineTo(35.0, 36.0)
        }
        g2.draw(setaPonta)

        g2.dispose()
        return img
    }

    // --- Ícone de API Duotone (Servidores em Rack) ---
    private fun desenharIconeApiDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Preenchimento translúcido dos racks
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fillRoundRect(3, 4, 42, 16, 7, 7)
        g2.fillRoundRect(3, 28, 42, 16, 7, 7)

        // Contorno com traço forte de 3.8px
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawRoundRect(3, 4, 42, 16, 7, 7)
        g2.drawRoundRect(3, 28, 42, 16, 7, 7)

        // Linha central de dados / barramento
        g2.stroke = BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(24, 20, 24, 28)

        // Slots horizontais
        g2.stroke = BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(10, 12, 23, 12)
        g2.drawLine(10, 36, 23, 36)

        // LEDs de status preenchidos 100% sólidos
        g2.fillOval(35, 9, 6, 6)
        g2.fillOval(35, 33, 6, 6)

        g2.dispose()
        return img
    }

    // --- Ícone de Configuração Duotone (Engrenagem) ---
    private fun desenharIconeConfiguracaoDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        val path = Path2D.Double()
        val teeth = 6
        val innerR = 14.5
        val outerR = 21.5
        val toothAngle = Math.PI / 12.0

        for (i in 0 until teeth) {
            val angle = i * 2 * Math.PI / teeth
            val a1 = angle - toothAngle
            val a2 = angle + toothAngle
            val m1 = angle - toothAngle * 1.55
            val m2 = angle + toothAngle * 1.55

            val x0 = 24.0 + innerR * cos(m1)
            val y0 = 24.0 + innerR * sin(m1)
            val x1 = 24.0 + outerR * cos(a1)
            val y1 = 24.0 + outerR * sin(a1)
            val x2 = 24.0 + outerR * cos(a2)
            val y2 = 24.0 + outerR * sin(a2)
            val x3 = 24.0 + innerR * cos(m2)
            val y3 = 24.0 + innerR * sin(m2)

            if (i == 0) path.moveTo(x0, y0) else path.lineTo(x0, y0)
            path.lineTo(x1, y1)
            path.lineTo(x2, y2)
            path.lineTo(x3, y3)
        }
        path.closePath()

        // Fundo translúcido Duotone
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fill(path)

        // Contorno com traço forte de 3.8px
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(path)

        // Furo central com traço sólido forte
        g2.stroke = BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(18, 18, 12, 12)

        g2.dispose()
        return img
    }

    // --- Ícone de Atualização Duotone (Setas de Refresh) ---
    private fun desenharIconeAtualizarDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Disco interno translúcido de fundo Duotone
        g2.color = Color(cor.red, cor.green, cor.blue, 40)
        g2.fillOval(7, 7, 34, 34)

        // Arcos semicirculares opostos em traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        // Raio R=18.5, centro (24.0, 24.0).
        // Arco superior: de 155° a 25° no sentido horário (extent -130°).
        // Ponto final exato: x = 24.0 + 18.5 * cos(25°) = 40.77, y = 24.0 - 18.5 * sin(25°) = 16.18
        val arc1 = Arc2D.Double(5.5, 5.5, 37.0, 37.0, 155.0, -130.0, Arc2D.OPEN)
        // Arco inferior: de 335° a 205° no sentido horário (extent -130°).
        // Ponto final exato: x = 24.0 - 18.5 * cos(25°) = 7.23, y = 24.0 + 18.5 * sin(25°) = 31.82
        val arc2 = Arc2D.Double(5.5, 5.5, 37.0, 37.0, 335.0, -130.0, Arc2D.OPEN)
        g2.draw(arc1)
        g2.draw(arc2)

        // Seta superior conectada ao final do arco, com perna interna bem aberta para não sobrepor o traço
        val arrow1 = Path2D.Double().apply {
            moveTo(43.5, 6.5)
            lineTo(40.77, 16.18)
            lineTo(30.5, 19.5)
        }
        g2.draw(arrow1)

        // Seta inferior (simetria 180°), perna interna bem aberta
        val arrow2 = Path2D.Double().apply {
            moveTo(4.5, 41.5)
            lineTo(7.23, 31.82)
            lineTo(17.5, 28.5)
        }
        g2.draw(arrow2)

        g2.dispose()
        return img
    }

    // --- Ícone de Compartilhamento Duotone (Ondas concêntricas - Opção 2 aprovada) ---
    private fun desenharIconeCompartilhamentoDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Setor circular translúcido Duotone (~22% opacidade)
        val corFundo = Color(cor.red, cor.green, cor.blue, 55)
        g2.color = corFundo
        val pie = Arc2D.Double(6.5 - 35.0, 41.5 - 35.0, 70.0, 70.0, 0.0, 90.0, Arc2D.PIE)
        g2.fill(pie)

        // Contorno e arcos em cor sólida forte de 4.2px
        g2.color = cor
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        // Ponto de origem preenchido sólido
        g2.fillOval(3, 38, 7, 7)

        val arc1 = Arc2D.Double(6.5 - 18.0, 41.5 - 18.0, 36.0, 36.0, 0.0, 90.0, Arc2D.OPEN)
        val arc2 = Arc2D.Double(6.5 - 35.0, 41.5 - 35.0, 70.0, 70.0, 0.0, 90.0, Arc2D.OPEN)
        g2.draw(arc1)
        g2.draw(arc2)

        g2.dispose()
        return img
    }

    // --- Ícone de Adicionar Duotone (+) ---
    private fun desenharIconeMaisDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Fundo circular translúcido
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fillOval(3, 3, 42, 42)

        // Círculo externo com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(3, 3, 42, 42)

        // Cruz central (+) com traço forte de 4.2px
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(12, 24, 36, 24)
        g2.drawLine(24, 12, 24, 36)

        g2.dispose()
        return img
    }

    // --- Ícone de Remover Duotone (-) ---
    private fun desenharIconeMenosDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Fundo circular translúcido
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fillOval(3, 3, 42, 42)

        // Círculo externo com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(3, 3, 42, 42)

        // Traço central (-) com traço forte de 4.2px
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(12, 24, 36, 24)

        g2.dispose()
        return img
    }

    // --- Ícone de Subir Duotone (Seta para cima) ---
    private fun desenharIconeSubirDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Ponta triangular translúcida
        val pontaTri = Path2D.Double().apply {
            moveTo(8.0, 20.0)
            lineTo(24.0, 4.0)
            lineTo(40.0, 20.0)
            closePath()
        }
        g2.color = Color(cor.red, cor.green, cor.blue, 55)
        g2.fill(pontaTri)

        // Haste e contorno da ponta em traço forte de 4.2px
        g2.color = cor
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(24, 44, 24, 5)

        val ponta = Path2D.Double().apply {
            moveTo(8.0, 20.0)
            lineTo(24.0, 4.0)
            lineTo(40.0, 20.0)
        }
        g2.draw(ponta)

        g2.dispose()
        return img
    }

    // --- Ícone de Descer Duotone (Seta para baixo) ---
    private fun desenharIconeDescerDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Ponta triangular translúcida
        val pontaTri = Path2D.Double().apply {
            moveTo(8.0, 28.0)
            lineTo(24.0, 44.0)
            lineTo(40.0, 28.0)
            closePath()
        }
        g2.color = Color(cor.red, cor.green, cor.blue, 55)
        g2.fill(pontaTri)

        // Haste e contorno da ponta em traço forte de 4.2px
        g2.color = cor
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(24, 4, 24, 43)

        val ponta = Path2D.Double().apply {
            moveTo(8.0, 28.0)
            lineTo(24.0, 44.0)
            lineTo(40.0, 28.0)
        }
        g2.draw(ponta)

        g2.dispose()
        return img
    }

    // --- Ícone de Confirmar Duotone (Círculo com Checkmark) ---
    private fun desenharIconeConfirmarDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Fundo circular translúcido
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fillOval(3, 3, 42, 42)

        // Círculo com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(3, 3, 42, 42)

        // Visto de confirmação com traço de 4.2px
        val check = Path2D.Double().apply {
            moveTo(13.0, 24.0)
            lineTo(21.0, 32.5)
            lineTo(35.5, 15.5)
        }
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(check)

        g2.dispose()
        return img
    }

    // --- Ícone de Cancelar Duotone (Círculo com X) ---
    private fun desenharIconeCancelarDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Fundo circular translúcido
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fillOval(3, 3, 42, 42)

        // Círculo com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(3, 3, 42, 42)

        // Traços do X com traço forte de 4.2px
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(15, 15, 33, 33)
        g2.drawLine(33, 15, 15, 33)

        g2.dispose()
        return img
    }

    // --- Ícone de Importar Duotone / Download ---
    private fun desenharIconeImportarDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Bandeja inferior com fundo translúcido
        val bandeja = Path2D.Double().apply {
            moveTo(4.5, 26.0)
            lineTo(4.5, 41.5)
            quadTo(4.5, 44.5, 7.5, 44.5)
            lineTo(40.5, 44.5)
            quadTo(43.5, 44.5, 43.5, 41.5)
            lineTo(43.5, 26.0)
        }
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fill(bandeja)

        // Contorno da bandeja com traço de 3.8px
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(bandeja)

        // Seta para baixo com traço forte de 4.2px
        g2.stroke = BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(24, 3, 24, 30)

        val seta = Path2D.Double().apply {
            moveTo(13.5, 20.0)
            lineTo(24.0, 30.5)
            lineTo(34.5, 20.0)
        }
        g2.draw(seta)

        g2.dispose()
        return img
    }

    // --- Ícone de Pesquisar Duotone (Lupa com Lente Iluminada) ---
    private fun desenharIconePesquisarDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        // Lente translúcida brilhante
        g2.color = Color(cor.red, cor.green, cor.blue, 55)
        g2.fillOval(4, 4, 26, 26)

        // Contorno da lente com traço forte de 4.0px
        g2.color = cor
        g2.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawOval(4, 4, 26, 26)

        // Cabo diagonal forte de 4.8px
        g2.stroke = BasicStroke(4.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(26, 26, 44, 44)

        g2.dispose()
        return img
    }

    // --- Ícone de Alerta Duotone (Triângulo) ---
    private fun desenharIconeAlertaTrianguloDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        val tri = Path2D.Double().apply {
            moveTo(24.0, 3.5)
            lineTo(45.0, 42.5)
            quadTo(46.0, 44.5, 43.5, 44.5)
            lineTo(4.5, 44.5)
            quadTo(2.0, 44.5, 3.0, 42.5)
            closePath()
        }

        // Fundo translúcido Duotone
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fill(tri)

        // Contorno forte de 3.8px
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(tri)

        // Exclamação sólida forte
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawLine(24, 15, 24, 28)
        g2.fillOval(21, 35, 6, 6)

        g2.dispose()
        return img
    }

    // --- Ícone de Confirmação Duotone (Balão com Pergunta) ---
    private fun desenharIconePerguntaBalaoDuotone(cor: Color): BufferedImage {
        val img = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        configurarRender(g2, cor)

        val balao = Path2D.Double().apply {
            moveTo(12.0, 4.0)
            lineTo(36.0, 4.0)
            quadTo(44.5, 4.0, 44.5, 12.5)
            lineTo(44.5, 27.5)
            quadTo(44.5, 36.0, 36.0, 36.0)
            lineTo(24.0, 36.0)
            lineTo(13.0, 44.5)
            lineTo(15.5, 36.0)
            lineTo(12.0, 36.0)
            quadTo(3.5, 36.0, 3.5, 27.5)
            lineTo(3.5, 12.5)
            quadTo(3.5, 4.0, 12.0, 4.0)
            closePath()
        }

        // Fundo translúcido Duotone
        g2.color = Color(cor.red, cor.green, cor.blue, 50)
        g2.fill(balao)

        // Contorno forte de 3.8f
        g2.color = cor
        g2.stroke = BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(balao)

        // Interrogação sólida forte
        val interrogacao = Path2D.Double().apply {
            moveTo(19.5, 15.0)
            quadTo(19.5, 11.5, 24.0, 11.5)
            quadTo(28.5, 11.5, 28.5, 15.0)
            quadTo(28.5, 18.5, 24.0, 20.5)
            lineTo(24.0, 24.0)
        }
        g2.stroke = BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.draw(interrogacao)
        g2.fillOval(21, 28, 6, 6)

        g2.dispose()
        return img
    }

    private fun configurarRender(g2: Graphics2D, cor: Color) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2.color = cor
    }

    private fun salvarIcone(nome: String, img: BufferedImage) {
        val file = File(outputDir, nome)
        ImageIO.write(img, "PNG", file)
    }

    private fun salvarIconeAlert(nome: String, img: BufferedImage) {
        val file = File(alertDir, nome)
        ImageIO.write(img, "PNG", file)
    }
}
