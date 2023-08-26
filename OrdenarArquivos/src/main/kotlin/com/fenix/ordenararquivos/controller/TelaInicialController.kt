package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.componentes.ImageViewZoom
import com.fenix.ordenararquivos.configuration.Configuracao.loadProperties
import com.fenix.ordenararquivos.model.Caminhos
import com.fenix.ordenararquivos.model.Capa
import com.fenix.ordenararquivos.model.Manga
import com.fenix.ordenararquivos.model.TipoCapa
import com.fenix.ordenararquivos.service.MangaServices
import com.jfoenix.controls.*
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.stage.DirectoryChooser
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.imageio.ImageIO

class TelaInicialController : Initializable {

    private val LOG = LoggerFactory.getLogger(TelaInicialController::class.java)

    @FXML
    private lateinit var apGlobal: AnchorPane

    @FXML
    private lateinit var btnLimparTudo: JFXButton

    @FXML
    private lateinit var btnProcessar: JFXButton

    @FXML
    private lateinit var btnCompactar: JFXButton

    @FXML
    private lateinit var btnGerarCapa: JFXButton

    @FXML
    private lateinit var txtSimularPasta: JFXTextField

    @FXML
    private lateinit var txtPastaOrigem: JFXTextField

    @FXML
    private lateinit var btnPesquisarPastaOrigem: JFXButton

    @FXML
    private lateinit var txtPastaDestino: JFXTextField

    @FXML
    private lateinit var btnPesquisarPastaDestino: JFXButton

    @FXML
    private lateinit var txtNomePastaManga: JFXTextField

    @FXML
    private lateinit var txtVolume: JFXTextField

    @FXML
    private lateinit var btnVolumeMenos: JFXButton

    @FXML
    private lateinit var btnVolumeMais: JFXButton

    @FXML
    private lateinit var txtNomeArquivo: JFXTextField

    @FXML
    private lateinit var txtNomePastaCapitulo: JFXTextField

    @FXML
    private lateinit var cbVerificaPaginaDupla: JFXCheckBox

    @FXML
    private lateinit var cbCompactarArquivo: JFXCheckBox

    @FXML
    private lateinit var cbMesclarCapaTudo: JFXCheckBox

    @FXML
    private lateinit var cbAjustarMargemCapa: JFXCheckBox

    @FXML
    private lateinit var lsVwListaImagens: JFXListView<String>

    @FXML
    private lateinit var txtGerarInicio: JFXTextField

    @FXML
    private lateinit var txtGerarFim: JFXTextField

    @FXML
    private lateinit var txtSeparador: JFXTextField

    @FXML
    private lateinit var txtAreaImportar: JFXTextArea

    @FXML
    private lateinit var btnLimpar: JFXButton

    @FXML
    private lateinit var btnImportar: JFXButton

    @FXML
    private lateinit var txtQuantidade: JFXTextField

    @FXML
    private lateinit var btnSubtrair: JFXButton

    @FXML
    private lateinit var btnSomar: JFXButton

    @FXML
    private lateinit var tbViewTabela: TableView<Caminhos>

    @FXML
    private lateinit var clCapitulo: TableColumn<Caminhos, String>

    @FXML
    private lateinit var clNumeroPagina: TableColumn<Caminhos, String>

    @FXML
    private lateinit var clNomePasta: TableColumn<Caminhos, String>

    @FXML
    private lateinit var lblAviso: Label

    @FXML
    private lateinit var lblAlerta: Label

    @FXML
    private lateinit var lblProgresso: Label

    @FXML
    private lateinit var pbProgresso: ProgressBar

    @FXML
    private lateinit var btnScrollSubir: JFXButton

    @FXML
    private lateinit var btnScrollDescer: JFXButton

    @FXML
    private lateinit var sliderTudo: JFXSlider

    @FXML
    private lateinit var imgTudo: ImageView

    @FXML
    private lateinit var sliderFrente: JFXSlider

    @FXML
    private lateinit var imgFrente: ImageView

    @FXML
    private lateinit var sliderTras: JFXSlider

    @FXML
    private lateinit var imgTras: ImageView


    private var lista: MutableList<Caminhos> = arrayListOf()
    private var obsLCaminhos: ObservableList<Caminhos> = FXCollections.observableArrayList(lista)
    private var obsLListaItens: ObservableList<String> = FXCollections.observableArrayList("")
    private var obsLImagesSelected: ObservableList<Capa> = FXCollections.observableArrayList()

    private var caminhoOrigem: File? = null
    private var caminhoDestino: File? = null
    private var selecionada: String? = null
    private val service = MangaServices()

    private fun limpaCampos() {
        limparCapas()
        lista = ArrayList()
        obsLCaminhos = FXCollections.observableArrayList(lista)
        tbViewTabela.items = obsLCaminhos
        caminhoOrigem = null
        caminhoDestino = null
        lblAlerta.text = ""
        lblAviso.text = ""
        manga = null
        txtSimularPasta.text = ""
        txtPastaOrigem.text = ""
        txtPastaDestino.text = ""
        txtNomePastaManga.text = "[JPN] Manga -"
        txtVolume.text = "Volume 01"
        txtNomePastaCapitulo.text = "Capítulo"
        txtSeparador.text = "-"
        onBtnLimpar()
        obsLListaItens = FXCollections.observableArrayList("")
        lsVwListaImagens.items = obsLListaItens
        selecionada = null
        lblProgresso.text = ""
        pbProgresso.progress = 0.0
    }

    private val filterNameFile: FilenameFilter
        get() = FilenameFilter { _: File?, name: String ->
            if (name.lastIndexOf('.') > 0) {
                val p = Pattern.compile(IMAGE_PATTERN)
                return@FilenameFilter p.matcher(name).matches()
            }
            false
        }

    @FXML
    private fun onBtnScrollSubir() {
        if (!lsVwListaImagens.items.isEmpty()) 
            lsVwListaImagens.scrollTo(0)
    }

    @FXML
    private fun onBtnScrollBaixo() {
        if (!lsVwListaImagens.items.isEmpty()) 
            lsVwListaImagens.scrollTo(lsVwListaImagens.items.size)
    }

    @FXML
    private fun onBtnLimparTudo() {
        limpaCampos()
    }

    @FXML
    private fun onBtnCompactar() {
        if (caminhoDestino!!.exists() && !txtNomeArquivo.text.isEmpty() && !LAST_PROCESS_FOLDERS.isEmpty())
            compactaArquivo(File(caminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }), LAST_PROCESS_FOLDERS)
    }

    @FXML
    private fun onBtnGerarCapa() {
    }

    @FXML
    private fun onBtnVolumeMenos() {
        // Matches retorna se toda a string for o patern, no caso utiliza-se o inicio
        // para mostrar que tenha em toda a string.
        if (txtVolume.text.matches(Regex(".*" + NUMBER_PATTERN))) {
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(NUMBER_PATTERN.toRegex(), "").toRegex(), "").trim { it <= ' ' }
            val padding = volume.length
            try {
                var number = Integer.valueOf(volume)
                texto = texto.substring(0, texto.lastIndexOf(volume))
                number = number - 1
                volume = texto + String.format("%0" + padding + "d", number)
                txtVolume.text = volume
                simulaNome()
                carregaManga()
            } catch (e: NumberFormatException) {
                try {
                    var number = java.lang.Double.valueOf(volume)
                    texto = texto.substring(0, texto.lastIndexOf(volume))
                    number = number - 1
                    volume = texto + String.format("%0$padding.1f", number).replace("\\.".toRegex(), "")
                        .replace("\\,".toRegex(), ".")
                    txtVolume.text = volume
                    simulaNome()
                    carregaManga()
                } catch (e1: NumberFormatException) {
                    LOG.info("Erro ao incrementar valor.", e)
                }
            }
        }
    }

    @FXML
    private fun onBtnVolumeMais() {
        if (txtVolume.text.matches(Regex(".*" + NUMBER_PATTERN))) {
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(NUMBER_PATTERN.toRegex(), "").toRegex(), "").trim { it <= ' ' }
            val padding = volume.length
            try {
                var number = Integer.valueOf(volume)
                texto = texto.substring(0, texto.lastIndexOf(volume))
                number = number + 1
                volume = texto + String.format("%0" + padding + "d", number)
                txtVolume.text = volume
                simulaNome()
                carregaManga()
            } catch (e: NumberFormatException) {
                try {
                    var number = java.lang.Double.valueOf(volume)
                    texto = texto.substring(0, texto.lastIndexOf(volume))
                    number = number + 1
                    volume = texto + String.format("%0$padding.1f", number).replace("\\.".toRegex(), "")
                        .replace("\\,".toRegex(), ".")
                    txtVolume.text = volume
                    simulaNome()
                    carregaManga()
                } catch (e1: NumberFormatException) {
                    LOG.info("Erro ao incrementar valor.", e)
                }
            }
        }
    }

    private fun desabilita() {
        btnLimparTudo.isDisable = true
        btnCompactar.isDisable = true
        txtPastaOrigem.isDisable = true
        btnPesquisarPastaOrigem.isDisable = true
        txtPastaDestino.isDisable = true
        btnPesquisarPastaDestino.isDisable = true
        txtNomePastaManga.isDisable = true
        txtVolume.isDisable = true
        btnImportar.isDisable = true
        btnLimpar.isDisable = true
        btnImportar.isDisable = true
        tbViewTabela.isDisable = true
    }

    private fun habilita() {
        btnLimparTudo.isDisable = false
        btnCompactar.isDisable = false
        txtPastaOrigem.isDisable = false
        btnPesquisarPastaOrigem.isDisable = false
        txtPastaDestino.isDisable = false
        btnPesquisarPastaDestino.isDisable = false
        txtNomePastaManga.isDisable = false
        txtVolume.isDisable = false
        btnImportar.isDisable = false
        btnLimpar.isDisable = false
        btnImportar.isDisable = false
        tbViewTabela.isDisable = false
        btnProcessar.accessibleTextProperty().set("PROCESSA")
        btnProcessar.text = "Processar"
        apGlobal.cursorProperty().set(null)
    }

    private fun validaCampos(): Boolean {
        var valida = true
        if (caminhoOrigem == null || !caminhoOrigem!!.exists()) {
            txtSimularPasta.text = "Origem não informado."
            txtPastaOrigem.unFocusColor = Color.RED
            valida = false
        }

        if (caminhoDestino == null || !caminhoDestino!!.exists()) {
            txtSimularPasta.text = "Destino não informado."
            txtPastaDestino.unFocusColor = Color.RED
            valida = false
        }

        if (lsVwListaImagens.selectionModel.selectedItem == null)
            lsVwListaImagens.selectionModel.select(0)

        if (obsLCaminhos.isEmpty())
            valida = false

        if (cbCompactarArquivo.isSelected && txtNomeArquivo.text.isEmpty()) {
            txtSimularPasta.text = "Não informado nome do arquivo."
            txtNomeArquivo.unFocusColor = Color.RED
            valida = false
        }

        if (cbCompactarArquivo.isSelected && (WINRAR == null || WINRAR!!.isEmpty())) {
            txtSimularPasta.text = "Winrar não configurado."
            valida = false
        }

        return valida
    }

    private var manga: Manga? = null
    private fun geraManga(id: Long): Manga {
        var nome = txtNomePastaManga.text
        if (nome.contains("]"))
            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }
        if (nome.substring(nome.length - 1).equals("-", ignoreCase = true)) nome =
            nome.substring(0, nome.length - 1).trim { it <= ' ' }

        val quantidade = if (obsLListaItens == null) 0 else obsLListaItens.size

        return Manga(id, nome, txtVolume.text, txtNomePastaCapitulo.text.trim { it <= ' ' },
            txtNomeArquivo.text.trim { it <= ' ' }, quantidade, txtAreaImportar.text, LocalDateTime.now()
        )
    }

    private fun carregaManga() {
        manga = service.find(geraManga(0))

        lblAviso.text = if (manga != null) "Manga localizado." else "Manga não localizado."

        manga?.let {
            txtNomePastaManga.text = "[JPN] " + it.nome + " - "
            txtVolume.text = it.volume
            txtNomePastaCapitulo.text = it.capitulo
            txtNomeArquivo.text = it.arquivo
            txtAreaImportar.text = it.capitulos

            val quantidade = if (obsLListaItens == null) 0 else obsLListaItens.size

            lblAlerta.text = if (it.quantidade.compareTo(quantidade) !== 0) "Difereça na quantidade de imagens." else ""

            lista = ArrayList(it.caminhos)
            obsLCaminhos = FXCollections.observableArrayList(lista)
            tbViewTabela.items = obsLCaminhos
        }
    }

    private fun salvaManga() {
        manga = if (manga == null) geraManga(0) else geraManga(manga!!.id)
        manga!!.caminhos.clear()
        for (caminho in lista)
            manga!!.addCaminhos(caminho)
        service.save(manga)
        Platform.runLater {
            lblAlerta.text = ""
            lblAviso.text = "Manga salvo."
        }
    }

    private fun criaPasta(caminho: String): File {
        val arquivo = File(caminho)
        if (!arquivo.exists())
            arquivo.mkdir()
        return arquivo
    }

    @Throws(IOException::class)
    private fun copiaItem(arquivo: File, destino: File, nome: String = arquivo.name): Path {
        val arquivoDestino = Paths.get(destino.toPath().toString() + "/" + nome)
        Files.copy(arquivo.toPath(), arquivoDestino, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        return arquivoDestino
    }

    @Throws(IOException::class)
    private fun renomeiaItem(arquivo: Path, nome: String): File {
        return Files.move(arquivo, arquivo.resolveSibling(nome), StandardCopyOption.REPLACE_EXISTING).toFile()
    }

    private fun deletaItem(item: String) {
        val arquivo = File(item)
        if (arquivo.exists()) arquivo.delete()
    }

    private fun verificaPaginaDupla(arquivo: File): Boolean {
        var result = false
        try {
            var img: BufferedImage? = null
            img = ImageIO.read(arquivo)
            result = img.width / img.height > 0.9
        } catch (e: IOException) {
            LOG.error("Erro ao verificar a página dupla.", e)
        }
        return result
    }

    val PASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private fun limparCapas() {
        imgTudo.image = null
        imgFrente.image = null
        imgTras.image = null
        obsLImagesSelected.clear()
        if (!PASTA_TEMPORARIA.exists())
            PASTA_TEMPORARIA.mkdir()
        else {
            for (item in PASTA_TEMPORARIA.listFiles())
                item.delete()
        }
    }

    private fun simularCapa(tipo: TipoCapa, imagem: Image?) {
        when (tipo) {
            TipoCapa.CAPA -> ImageViewZoom.configura(imagem, imgFrente, sliderFrente)
            TipoCapa.TRAS -> ImageViewZoom.configura(imagem, imgTras, sliderTras)
            TipoCapa.CAPA_COMPLETA -> ImageViewZoom.configura(imagem, imgTudo, sliderTudo)
            else -> {}
        }
    }

    private fun remCapa(arquivo: String) {
        val capa = obsLImagesSelected.stream().filter { it.nome.equals(arquivo, ignoreCase = true) }.findFirst()
        if (capa.isPresent) {
            obsLImagesSelected.remove(capa.get())
            if (capa.get().tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0) {
                val frente = obsLImagesSelected.stream()
                    .filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }
                    .findFirst()
                if (frente.isPresent) CompletableFuture.runAsync {
                    simularCapa(
                        capa.get().tipo,
                        carregaImagem(File(txtPastaOrigem.text + "\\" + frente.get().arquivo))
                    )
                    simularCapa(TipoCapa.TRAS, null)
                } else simularCapa(TipoCapa.CAPA_COMPLETA, null)
            } else simularCapa(capa.get().tipo, null)
        }
    }

    private fun addCapa(tipo: TipoCapa, arquivo: String) {
        val img = File(txtPastaOrigem.text + "\\" + arquivo)
        val isDupla = isPaginaDupla(img)
        if (tipo === TipoCapa.CAPA_COMPLETA) {
            var capas = obsLImagesSelected.stream().filter {  it.tipo.compareTo(tipo) == 0 && it.direita != null }.findFirst()

            if (capas.isEmpty)
                capas = obsLImagesSelected.stream().filter {  it.tipo.compareTo(tipo) == 0 }.findFirst()

            val frente = if (capas.isPresent) capas.get() else null
            val tras = if (capas.isPresent) capas.get().direita else null

            if (isDupla) {
                val nome = img.name.substring(0, img.name.lastIndexOf("."))
                val ext = img.name.substring(img.name.lastIndexOf("."))
                val direita = File(PASTA_TEMPORARIA.toString() + "\\" + nome + TRAS + ext)
                val esquerda = File(PASTA_TEMPORARIA.toString() + "\\" + nome + FRENTE + ext)
                obsLImagesSelected.removeIf {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }
                obsLImagesSelected.add(Capa(arquivo, esquerda.name, tipo, isDupla))
                obsLImagesSelected.removeIf {  it.tipo.compareTo(TipoCapa.TRAS) == 0 }
                obsLImagesSelected.add(Capa(arquivo, direita.name, TipoCapa.TRAS, false))
                CompletableFuture.runAsync {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA)
                        divideImagens(img, esquerda, direita)
                        simularCapa(tipo, carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + img.name)))
                        simularCapa(TipoCapa.TRAS, Image(direita.absolutePath))
                    } catch (e: IOException) {
                        LOG.warn("Erro ao processar imagem: Capa completa, pagina dupla.", e)
                    }
                }
            } else if (frente == null) {
                remCapa(arquivo)
                obsLImagesSelected.add(Capa(arquivo, img.name, tipo, isDupla))
                CompletableFuture.runAsync {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA)
                        simularCapa(tipo, carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + img.name)))
                    } catch (e: IOException) {
                        LOG.warn("Erro ao processar imagem: Capa completa frente.", e)
                    }
                }
            } else if (tras == null) {
                frente.direita = Capa(arquivo, img.name, tipo, isDupla)
                obsLImagesSelected.add(frente.direita)
                obsLImagesSelected.removeIf {  it.tipo.compareTo(TipoCapa.TRAS) == 0 }
                obsLImagesSelected.add(Capa(arquivo, img.name, TipoCapa.TRAS, false))
                CompletableFuture.runAsync {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA)
                        val imagem = File(PASTA_TEMPORARIA.toString() + "\\" + img.name)
                        simularCapa(tipo, carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + frente.arquivo), imagem))
                        simularCapa(TipoCapa.TRAS, Image(imagem.absolutePath))
                    } catch (e: IOException) {
                        LOG.warn("Erro ao processar imagem: Capa completa trazeira.", e)
                    }
                }
            }
        } else {
            val capa = obsLImagesSelected.stream().filter {  it.tipo.compareTo(tipo) == 0 }.findFirst().orElse(Capa())
            capa.tipo = tipo
            capa.nome = arquivo
            capa.arquivo = img.name
            capa.isDupla = isDupla
            obsLImagesSelected.remove(capa)
            obsLImagesSelected.add(capa)
            CompletableFuture.runAsync {
                try {
                    copiaItem(img, PASTA_TEMPORARIA)
                    simularCapa(tipo, carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + img.name)))
                } catch (e: IOException) {
                    LOG.warn("Erro ao processar imagem: Capa Tipo $tipo.", e)
                }
            }
        }
    }

    private fun reloadCapa() {
        if (obsLImagesSelected.isEmpty()) return
        CompletableFuture.runAsync {
            for (capa in obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) != 0 }.toList()) {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + capa.nome), PASTA_TEMPORARIA)
                    simularCapa(capa.tipo, carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + capa.arquivo)))
                } catch (e: IOException) {
                    LOG.warn("Erro ao reprocessar imagem: " + capa.tipo + ".", e)
                }
            }
            var completa = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }.findFirst()
            if (completa.isEmpty)
                completa = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }.findFirst()

            if (completa.isPresent) if (completa.get().direita != null) {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().nome), PASTA_TEMPORARIA)
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().direita!!.nome), PASTA_TEMPORARIA)
                    simularCapa(TipoCapa.CAPA_COMPLETA, carregaImagem(
                            File(PASTA_TEMPORARIA.toString() + "\\" + completa.get().arquivo),
                            File(PASTA_TEMPORARIA.toString() + "\\" + completa.get().direita!!.arquivo)
                        )
                    )
                } catch (e: IOException) {
                    LOG.warn("Erro ao reprocessar imagem: " + completa.get().tipo + ".", e)
                }
            } else {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().nome), PASTA_TEMPORARIA)
                    simularCapa(TipoCapa.CAPA_COMPLETA,
                        carregaImagem(File(PASTA_TEMPORARIA.toString() + "\\" + completa.get().arquivo))
                    )
                } catch (e: IOException) {
                    LOG.warn("Erro ao reprocessar imagem: " + completa.get().tipo + ".", e)
                }
            }
        }
    }

    private var CANCELAR = false
    private val FRENTE = " Frente"
    private val TUDO = " Tudo"
    private val TRAS = " Tras"
    private val SUMARIO = " zSumário"
    
    private fun processar() {
        val movimentaArquivos: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                try {
                    salvaManga()
                    if (lsVwListaImagens.selectionModel.selectedItem != null) 
                        selecionada = lsVwListaImagens.selectionModel.selectedItem
                    
                    CANCELAR = false
                    var i = 0
                    val max: Int = caminhoOrigem!!.listFiles(filterNameFile).size
                    val pastasCompactar: MutableList<File> = ArrayList()
                    LAST_PROCESS_FOLDERS.clear()
                    val arquivoZip = caminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }
                    val mesclarCapaTudo = cbMesclarCapaTudo.isSelected
                    val gerarArquivo = cbCompactarArquivo.isSelected
                    val verificaPagDupla = cbVerificaPaginaDupla.isSelected
                    updateProgress(i.toLong(), max.toLong())
                    updateMessage("Criando diretórios...")
                    val nomePasta = (caminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' })
                    updateMessage("Criando diretórios - $nomePasta Capa\\")
                    val destinoCapa = criaPasta("$nomePasta Capa\\")
                    pastasCompactar.add(destinoCapa)
                    if (!obsLImagesSelected.isEmpty()) {
                        var nome = txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' }
                        if (nome.contains("]"))
                            nome = nome.substring(nome.indexOf(']') + 1).trim { it <= ' ' }

                        val capa = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA) == 0 }.findFirst()

                        if (capa.isPresent)
                            limpaMargemImagens(renomeiaItem(copiaItem(File(caminhoOrigem!!.path + "\\" + capa.get().nome), destinoCapa), nome + FRENTE + capa.get().nome.substring(capa.get().nome.lastIndexOf("."))), false)

                        val tras = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.TRAS) == 0 }.findFirst()

                        if (tras.isPresent)
                            limpaMargemImagens(renomeiaItem(copiaItem(File(caminhoOrigem!!.path + "\\" + tras.get().nome), destinoCapa), nome + TRAS + tras.get().nome.substring(tras.get().nome.lastIndexOf("."))), false)

                        val sumario = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.SUMARIO) == 0 }.findFirst()

                        if (sumario.isPresent) renomeiaItem(
                            copiaItem(
                                File(caminhoOrigem!!.path + "\\" + sumario.get().nome),
                                destinoCapa
                            ), nome + SUMARIO + sumario.get().nome.substring(sumario.get().nome.lastIndexOf("."))
                        )
                        if (obsLImagesSelected.stream().anyMatch {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }) {
                            val tudo = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }.findFirst()

                            if (mesclarCapaTudo) {
                                copiaItem(File(caminhoOrigem!!.path + "\\" + tudo.get().nome), PASTA_TEMPORARIA)
                                copiaItem(File(caminhoOrigem!!.path + "\\" + tudo.get().direita!!.nome), PASTA_TEMPORARIA)
                                val esquerda = File(PASTA_TEMPORARIA, tudo.get().nome)
                                val direita = File(PASTA_TEMPORARIA, tudo.get().direita!!.nome)
                                limpaMargemImagens(esquerda, true)
                                limpaMargemImagens(direita, true)
                                mesclarImagens(File(destinoCapa.path + "\\" + nome + TUDO + ".png"), esquerda, direita)
                            } else {
                                val arquivo = File(caminhoOrigem!!.path + "\\" + nome + TUDO + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                                renomeiaItem(copiaItem(File(caminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                                limpaMargemImagens(arquivo, true)
                            }
                        } else if (obsLImagesSelected.stream().anyMatch {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla } || obsLImagesSelected.stream().anyMatch {  it.tipo.compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla }) {
                            var tudo = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla }.findFirst()
                            if (tudo.isEmpty) tudo = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla }.findFirst()
                            val arquivo = File(caminhoOrigem!!.path + "\\" + nome + TUDO + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                            renomeiaItem(copiaItem(File(caminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                            limpaMargemImagens(arquivo, true)
                            if (tras.isEmpty || capa.isEmpty) {
                                val esquerda = File(PASTA_TEMPORARIA, tudo.get().nome)
                                val direita = File(PASTA_TEMPORARIA, tudo.get().direita!!.nome)
                                if (divideImagens(arquivo, esquerda, direita)) {
                                    renomeiaItem(copiaItem(esquerda, destinoCapa), "$nome$FRENTE.png")
                                    renomeiaItem(copiaItem(direita, destinoCapa), "$nome$TRAS.png")
                                }
                            }
                        } else {
                            val tudo = obsLImagesSelected.stream().filter {  it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }.findFirst()
                            if (tudo.isPresent) {
                                val arquivo = File(caminhoOrigem!!.path + "\\" + nome + TUDO + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                                renomeiaItem(copiaItem(File(caminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                                limpaMargemImagens(arquivo, true)
                            }
                        }
                    }
                    var pagina = 0
                    var proxCapitulo = 0
                    var contadorCapitulo = 0
                    var contar = false
                    var destino = criaPasta(nomePasta + " " + lista[pagina].nomePasta + "\\")
                    pastasCompactar.add(destino)
                    contadorCapitulo = Integer.valueOf(lista[pagina].numeroPagina)
                    pagina++
                    if (lista.size > 1)
                        proxCapitulo = lista[pagina].numero

                    for (arquivos in caminhoOrigem!!.listFiles(filterNameFile)) {
                        if (CANCELAR)
                            return true

                        LOG.info("Contar: " + contar + " - Contador: " + contadorCapitulo + " - Prox cap: " + proxCapitulo + " - Nome Imagem: " + arquivos.name)

                        if (arquivos.name.equals(selecionada, ignoreCase = true))
                            contar = true

                        if (contar && verificaPagDupla) {
                            if (verificaPaginaDupla(arquivos))
                                contadorCapitulo++
                        }

                        if (contadorCapitulo >= proxCapitulo && pagina < lista.size) {
                            updateMessage("Criando diretório - " + nomePasta + " " + lista[pagina].nomePasta + "\\")
                            destino = criaPasta(nomePasta + " " + lista[pagina].nomePasta + "\\")
                            pastasCompactar.add(destino)
                            pagina++
                            if (pagina < lista.size)
                                proxCapitulo = Integer.valueOf(lista[pagina].numeroPagina)
                        }
                        i++
                        updateProgress(i.toLong(), max.toLong())
                        updateMessage("Processando item " + i + " de " + max + ". Copiando - " + arquivos.absolutePath)
                        copiaItem(arquivos, destino)

                        if (contar)
                            contadorCapitulo++

                        if (!btnProcessar.accessibleTextProperty().value.equals("CANCELA", ignoreCase = true))
                            break
                    }
                    if (gerarArquivo) {
                        updateMessage("Compactando arquivo: $arquivoZip")
                        destino = File(arquivoZip)

                        if (destino.exists())
                            destino.delete()

                        LAST_PROCESS_FOLDERS = pastasCompactar
                        if (!compactaArquivo(destino, pastasCompactar))
                            Platform.runLater { txtSimularPasta.setText("Erro ao gerar o arquivo, necessário compacta-lo manualmente.") }
                    }
                } catch (e: Exception) {
                    LOG.error("Erro ao processar.", e)
                    val a = Alert(AlertType.NONE)
                    a.alertType = AlertType.ERROR
                    a.contentText = e.toString()
                    a.show()
                }
                return true
            }

            override fun succeeded() {
                value
                updateMessage("Arquivos movidos com sucesso.")
                pbProgresso.progressProperty().unbind()
                lblProgresso.textProperty().unbind()
                habilita()
            }

            override fun failed() {
                super.failed()
                updateMessage("Erro ao mover os arquivos.")
                val a = Alert(AlertType.NONE)
                a.alertType = AlertType.ERROR
                a.contentText = "Erro ao mover arquivos."
                a.show()
                habilita()
            }
        }
        pbProgresso.progressProperty().bind(movimentaArquivos.progressProperty())
        lblProgresso.textProperty().bind(movimentaArquivos.messageProperty())
        val t = Thread(movimentaArquivos)
        t.isDaemon = true
        t.start()
    }

    private var proc: Process? = null
    private fun compactaArquivo(rar: File, arquivos: File): Boolean {
        var success = true
        var comando = ("rar a -ma4 -ep1 " + '"' + rar.path + '"' + " " + '"' + arquivos.path + '"')
        LOG.info(comando)
        comando = "cmd.exe /C cd \"" + WINRAR + "\" &&" + comando
        proc = null
        return try {
            val rt = Runtime.getRuntime()
            proc = rt.exec(comando)
            Platform.runLater {
                try {
                    LOG.info("Resultado: " + proc!!.waitFor())
                } catch (e: InterruptedException) {
                    LOG.error("Erro ao executar o comando cmd.", e)
                }
            }
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc!!.getInputStream()))
            var s: String?
            while (stdInput.readLine().also { s = it } != null) resultado += """
     $s
     
     """.trimIndent()
            if (!resultado.isEmpty()) LOG.info("Output comand:\n$resultado")
            s = null
            resultado = ""
            val stdError = BufferedReader(InputStreamReader(proc!!.getErrorStream()))
            while (stdError.readLine().also { s = it } != null) resultado += """
     $s
     
     """.trimIndent()
            if (!resultado.isEmpty()) {
                success = false
                LOG.info("""
                    Error comand:
                    $resultado
                    Necessário adicionar o rar no path e reiniciar a aplicação.
                    """.trimIndent()
                )
            }
            success
        } catch (e: Exception) {
            LOG.error("Erro ao compactar o arquivo.", e)
            false
        } finally {
            if (proc != null)
                proc!!.destroy()
        }
    }

    private fun compactaArquivo(rar: File, arquivos: List<File>): Boolean {
        var success = true
        var compactar = ""
        for (arquivo in arquivos) compactar += '"'.toString() + arquivo.path + '"' + ' '
        var comando = "rar a -ma4 -ep1 " + '"' + rar.path + '"' + " " + compactar
        LOG.info(comando)
        comando = "cmd.exe /C cd \"" + WINRAR + "\" &&" + comando
        return try {
            val rt = Runtime.getRuntime()
            proc = rt.exec(comando)
            Platform.runLater {
                try {
                    LOG.info("Resultado: " + proc!!.waitFor())
                } catch (e: InterruptedException) {
                    LOG.error("Erro ao executar o comando.", e)
                }
            }
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc!!.getInputStream()))
            var s: String?
            while (stdInput.readLine().also { s = it } != null) resultado += """
     $s
     
     """.trimIndent()
            if (!resultado.isEmpty()) LOG.info("Output comand:\n$resultado")
            s = null
            resultado = ""
            val stdError = BufferedReader(InputStreamReader(proc!!.getErrorStream()))
            while (stdError.readLine().also { s = it } != null) resultado += """
     $s
     
     """.trimIndent()
            if (!resultado.isEmpty()) {
                success = false
                LOG.info(
                    """
                    Error comand:
                    $resultado
                    Necessário adicionar o rar no path e reiniciar a aplicação.
                    """.trimIndent()
                )
            }
            success
        } catch (e: Exception) {
            LOG.error("Erro ao compactar o arquivo.", e)
            false
        } finally {
            if (proc != null)
                proc!!.destroy()
        }
    }

    private fun mesclarImagens(arquivoDestino: File?, frente: File?, tras: File?): Boolean {
        if (arquivoDestino == null || frente == null || tras == null) return false
        val img1: BufferedImage
        val img2: BufferedImage
        try {
            img1 = ImageIO.read(frente)
            img2 = ImageIO.read(tras)
            val offset = 0
            val width = img1.width + img2.width + offset
            val height = Math.max(img1.height, img2.height) + offset
            val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g2 = newImage.createGraphics()
            val oldColor = g2.color
            g2.paint = java.awt.Color.WHITE
            g2.fillRect(0, 0, width, height)
            g2.color = oldColor
            if (img1.height > img2.height) {
                val diff = (img1.height - img2.height) / 2
                g2.drawImage(img1, null, 0, 0)
                g2.drawImage(img2, null, img1.width + offset, diff)
            } else if (img1.height < img2.height) {
                val diff = (img2.height - img1.height) / 2
                g2.drawImage(img1, null, 0, diff)
                g2.drawImage(img2, null, img1.width + offset, 0)
            } else {
                g2.drawImage(img1, null, 0, 0)
                g2.drawImage(img2, null, img1.width + offset, 0)
            }
            g2.dispose()
            return ImageIO.write(newImage, "png", arquivoDestino)
        } catch (e: IOException) {
            LOG.error("Erro ao mesclar as imagens.", e)
        }
        return false
    }

    private fun divideImagens(arquivo: File?, destinoFrente: File?, destinoTras: File?): Boolean {
        if (arquivo == null || destinoFrente == null || destinoTras == null) return false
        val image: BufferedImage
        try {
            image = ImageIO.read(arquivo)
            val width = image.width / 2
            val height = image.height
            val frente = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val grFrente = frente.createGraphics()
            val colorFrente = grFrente.color
            grFrente.paint = java.awt.Color.WHITE
            grFrente.fillRect(0, 0, width, height)
            grFrente.color = colorFrente
            grFrente.drawImage(image, null, 0, 0)
            grFrente.dispose()
            val tras = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val grTras = tras.createGraphics()
            val colorTras = grTras.color
            grTras.paint = java.awt.Color.WHITE
            grTras.fillRect(0, 0, width, height)
            grTras.color = colorTras
            grTras.drawImage(image, null, -width, 0)
            grTras.dispose()
            return ImageIO.write(frente, "png", destinoFrente) && ImageIO.write(tras, "png", destinoTras)
        } catch (e: IOException) {
            LOG.error("Erro ao dividir as imagens.", e)
        }
        return false
    }

    private fun limpaMargemImagens(arquivo: File?, clearTopBottom: Boolean): File? {
        if (arquivo == null || !cbAjustarMargemCapa.isSelected) return arquivo
        val image: BufferedImage
        try {
            image = ImageIO.read(arquivo)
            val branco = java.awt.Color.WHITE.rgb
            var startX = 0
            var endX = image.width
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    if (image.getRGB(x, y) != branco) {
                        startX = x
                        break
                    }
                }
                if (startX > 0) break
            }
            for (x in image.width - 1 downTo 0) {
                for (y in 0 until image.height) {
                    if (image.getRGB(x, y) != branco) {
                        endX = x
                        break
                    }
                }
                if (endX < image.width) break
            }
            var startY = 0
            var endY = image.height
            if (clearTopBottom) {
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        if (image.getRGB(x, y) != branco) {
                            startY = y
                            break
                        }
                    }
                    if (startY > 0) break
                }
                for (y in image.height - 1 downTo 0) {
                    for (x in 0 until image.width) {
                        if (image.getRGB(x, y) != branco) {
                            endY = y
                            break
                        }
                    }
                    if (endY < image.height) break
                }
            }
            val width = endX - startX
            val height = endY - startY
            val frente = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val grFrente = frente.createGraphics()
            val colorFrente = grFrente.color
            grFrente.paint = java.awt.Color.WHITE
            grFrente.fillRect(0, 0, width, height)
            grFrente.color = colorFrente
            grFrente.drawImage(image, null, -startX, -startY)
            grFrente.dispose()
            ImageIO.write(frente, "png", arquivo)
        } catch (e: IOException) {
            LOG.error("Erro ao dividir as imagens.", e)
        }
        return arquivo
    }

    fun isPaginaDupla(arquivo: File?): Boolean {
        if (arquivo == null || !arquivo.exists()) return false
        val image: BufferedImage
        try {
            image = ImageIO.read(arquivo)
            return image.width / image.height > 0.9
        } catch (e: IOException) {
            LOG.error("Erro ao verificar imagem.", e)
        }
        return false
    }

    fun carregaImagem(esquerda: File?, direita: File?): Image? {
        if (direita == null || esquerda == null || !direita.exists() || !esquerda.exists()) return null
        try {
            limpaMargemImagens(direita, true)
            limpaMargemImagens(esquerda, true)
            val img = File(PASTA_TEMPORARIA, "tudo.png")
            if (img.exists()) img.delete()
            img.createNewFile()
            if (cbMesclarCapaTudo.isSelected) mesclarImagens(img, esquerda, direita) else copiaItem(
                esquerda,
                PASTA_TEMPORARIA,
                img.name
            )
            return Image(img.absolutePath)
        } catch (e: IOException) {
            LOG.error("Erro ao verificar imagem.", e)
        }
        return null
    }

    fun carregaImagem(arquivo: File?): Image? {
        if (arquivo == null || !arquivo.exists()) return null
        limpaMargemImagens(arquivo, false)
        return Image(arquivo.absolutePath)
    }

    @FXML
    private fun onBtnProcessa() {
        if (validaCampos()) {
            if (btnProcessar.accessibleTextProperty().value.equals("PROCESSA", ignoreCase = true)) {
                btnProcessar.accessibleTextProperty().set("CANCELA")
                btnProcessar.text = "Cancelar"
                apGlobal.cursorProperty().set(Cursor.WAIT)
                desabilita()
                processar()
            } else CANCELAR = true
        }
    }

    @FXML
    private fun onBtnCarregarPastaOrigem() {
        caminhoOrigem = selecionaPasta(txtPastaOrigem.text)
        if (caminhoOrigem != null) txtPastaOrigem.text = caminhoOrigem!!.absolutePath else txtPastaOrigem.text = ""
        listaItens()
    }

    private fun carregaPastaOrigem() {
        caminhoOrigem = File(txtPastaOrigem.text)
        limparCapas()
        listaItens()
    }

    @FXML
    private fun onBtnCarregarPastaDestino() {
        caminhoDestino = selecionaPasta(txtPastaDestino.text)
        if (txtPastaDestino != null) txtPastaDestino.text =
            caminhoDestino!!.absolutePath else txtPastaDestino.setText("")
        simulaNome()
    }

    private fun carregaPastaDestino() {
        caminhoDestino = File(txtPastaDestino.text)
        simulaNome()
    }

    private fun listaItens() {
        obsLListaItens =
            if (caminhoOrigem != null && caminhoOrigem!!.list() != null) FXCollections.observableArrayList(
                *caminhoOrigem!!.list(
                    filterNameFile
                )
            ) else FXCollections.observableArrayList("")
        lsVwListaImagens.setItems(obsLListaItens)
        limparCapas()
        selecionada = obsLListaItens.get(0)
    }

    private fun simulaNome() {
        txtSimularPasta.text =
            (txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' } + " "
                    + txtNomePastaCapitulo.text.trim { it <= ' ' } + " 00")
        val nome = if (txtNomePastaManga.text.contains("]")) txtNomePastaManga.text.substring(
            txtNomePastaManga.text.indexOf("]") + 1
        ).trim { it <= ' ' } else txtNomePastaManga.text.trim { it <= ' ' }
        val posFix = if (txtNomePastaManga.text.contains("[JPN]")) " (Jap)" else ""
        txtNomeArquivo.text = nome + " " + txtVolume.text.trim { it <= ' ' } + posFix + ".cbr"
    }

    private fun selecionaPasta(pasta: String): File {
        val fileChooser = DirectoryChooser()
        fileChooser.title = "Selecione o arquivo."
        if (!pasta.isEmpty()) {
            val defaultDirectory = File(pasta)
            fileChooser.initialDirectory = defaultDirectory
        }
        return fileChooser.showDialog(null)
    }

    @FXML
    private fun onBtnLimpar() {
        lista = ArrayList()
        obsLCaminhos = FXCollections.observableArrayList(lista)
        tbViewTabela.items = obsLCaminhos
    }

    @FXML
    private fun onBtnSubtrair() {
        if (!txtQuantidade.text.isEmpty()) modificaNumeroPaginas(Integer.valueOf(txtQuantidade.text) * -1)
    }

    @FXML
    private fun onBtnSomar() {
        if (!txtQuantidade.text.isEmpty()) modificaNumeroPaginas(Integer.valueOf(txtQuantidade.text))
    }

    private fun modificaNumeroPaginas(quantidade: Int) {
        for (caminho in lista) {
            var qtde = caminho.numero + quantidade
            if (qtde < 1) qtde = 1
            caminho.numero = qtde
        }
        obsLCaminhos = FXCollections.observableArrayList(lista)
        tbViewTabela.items = obsLCaminhos
        txtQuantidade.text = ""
    }

    private fun limpaCampo() {
        txtGerarInicio.requestFocus()
    }

    @FXML
    private fun onBtnImporta() {
        if (!txtAreaImportar.text.trim { it <= ' ' }.isEmpty()) {
            var nomePasta = ""
            var separador = txtSeparador.text.trim { it <= ' ' }
            if (separador.isEmpty()) separador = " "
            txtSeparador.text = separador
            val linhas = txtAreaImportar.text.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var linha: Array<String>
            lista = ArrayList()
            for (ls in linhas) {
                linha = ls.split(txtSeparador.text.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                nomePasta = if (txtNomePastaCapitulo.text.trim { it <= ' ' }.equals("Capítulo", ignoreCase = true)
                    && linha[0].uppercase(Locale.getDefault()).contains("EXTRA")
                ) linha[0].trim { it <= ' ' } else txtNomePastaCapitulo.text.trim { it <= ' ' } + " " + linha[0].trim { it <= ' ' }
                lista.add(Caminhos(linha[0], linha[1], nomePasta))
            }
            obsLCaminhos = FXCollections.observableArrayList(lista)
            tbViewTabela.setItems(obsLCaminhos)
            tbViewTabela.refresh()
        }
    }

    @FXML
    private fun onBtnGerarCapitulos() {
        if (!txtGerarInicio.text.trim { it <= ' ' }.isEmpty() && !txtGerarFim.text.trim { it <= ' ' }.isEmpty()) {
            if (manga == null)
                manga = geraManga(0)
            val inicio = txtGerarInicio.text.trim { it <= ' ' }.toInt()
            val fim = txtGerarFim.text.trim { it <= ' ' }.toInt()
            if (inicio <= fim) {
                var texto = "" // txtAreaImportar.getText();
                // if (!texto.isEmpty())
                // texto += "\r\n";
                val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3")
                        + "d")
                for (i in inicio..fim) texto += String.format(padding, i) + "-" + if (i < fim) "\r\n" else ""
                txtAreaImportar.text = texto
            } else txtGerarInicio.unFocusColor = Color.GRAY
        } else {
            if (txtGerarInicio.text.trim { it <= ' ' }.isEmpty()) txtGerarInicio.unFocusColor = Color.GRAY
            if (txtGerarFim.text.trim { it <= ' ' }.isEmpty()) txtGerarFim.unFocusColor = Color.GRAY
        }
    }

    val mostraFinalTexto: MutableSet<TextField> = HashSet()
    private fun textFieldMostraFinalTexto(txt: JFXTextField) {
        mostraFinalTexto.add(txt)
        val onFocus: MutableSet<TextField> = HashSet()
        val overrideNextCaratChange: MutableSet<TextField> = HashSet()
        val onLoseFocus = ChangeListener<Boolean> { observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean ->
                val property = observable as ReadOnlyProperty<out Boolean>
                val tf = property.bean as TextField
                if (oldValue && onFocus.contains(tf))
                    onFocus.remove(tf)
                if (newValue)
                    onFocus.add(tf)
                if (!newValue)
                    overrideNextCaratChange.add(tf)
            }
        val onCaratChange = ChangeListener { observable: ObservableValue<out Number?>, oldValue: Number?, newValue: Number? ->
                val property = observable as ReadOnlyProperty<out Number?>
                val tf = property.bean as TextField
                if (overrideNextCaratChange.contains(tf)) {
                    tf.end()
                    overrideNextCaratChange.remove(tf)
                } else if (!onFocus.contains(tf) && mostraFinalTexto.contains(tf))
                    tf.end()
            }
        txt.focusedProperty().addListener(onLoseFocus)
        txt.caretPositionProperty().addListener(onCaratChange)
    }

    fun contemTipoSelecionado(tipo: TipoCapa, caminho: String?): Boolean {
        return if (obsLImagesSelected.isEmpty()) false else obsLImagesSelected.stream().anyMatch { capa: Capa -> capa.tipo == tipo && capa.nome.equals(caminho, ignoreCase = true) }
    }

    private var dellaySubir: Timer? = null
    private var dellayDescer: Timer? = null
    private fun selecionaImagens() {
        obsLImagesSelected = FXCollections.observableArrayList()
        lsVwListaImagens.addEventFilter(ScrollEvent.ANY) { e: ScrollEvent ->
            if (e.deltaY > 0) {
                if (e.deltaY > 10) {
                    btnScrollSubir.isVisible = true
                    btnScrollSubir.isDisable = false
                    if (dellaySubir != null)
                        dellaySubir!!.cancel()
                    dellaySubir = Timer()
                    dellaySubir!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollSubir.isVisible = false
                            btnScrollSubir.isDisable = true
                            dellaySubir = null
                        }
                    }, 3000)
                }
            } else {
                if (e.deltaY < 10) {
                    btnScrollDescer.isVisible = true
                    btnScrollDescer.isDisable = false
                    if (dellayDescer != null)
                        dellayDescer!!.cancel()
                    dellayDescer = Timer()
                    dellayDescer!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollDescer.isVisible = false
                            btnScrollDescer.isDisable = true
                            dellayDescer = null
                        }
                    }, 3000)
                }
            }
        }
        lsVwListaImagens.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1) {
                if (click.isControlDown) limparCapas() else {
                    val item = lsVwListaImagens.selectionModel.selectedItem
                    if (item != null) {
                        if (obsLImagesSelected.stream().anyMatch { e: Capa -> e.nome.equals(item, ignoreCase = true) })
                            remCapa(item)
                        else {
                            val tipo = if (click.isShiftDown)
                                TipoCapa.SUMARIO
                            else if (click.isAltDown)
                                TipoCapa.CAPA_COMPLETA
                            else
                                TipoCapa.CAPA
                            addCapa(tipo, item)
                        }
                    }
                }
            }
        }
        val capaSelected = PseudoClass.getPseudoClass("capaSelected")
        val capaCompletaSelected = PseudoClass.getPseudoClass("capaCompletaSelected")
        val sumarioSelected = PseudoClass.getPseudoClass("sumarioSelected")
        lsVwListaImagens.setCellFactory { lv: ListView<String> ->
            val cell: JFXListCell<String> = object : JFXListCell<String>() {
                override fun updateItem(images: String?, empty: Boolean) {
                    super.updateItem(images, empty)
                    text = images
                }
            }
            val listenerCapa = InvalidationListener { obs: Observable? ->
                cell.pseudoClassStateChanged(
                    capaSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.CAPA, cell.item)
                )
            }
            val listenerCapaCompleta = InvalidationListener { obs: Observable? ->
                cell.pseudoClassStateChanged(
                    capaCompletaSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.CAPA_COMPLETA, cell.item)
                )
            }
            val listenerSumario = InvalidationListener { obs: Observable? ->
                cell.pseudoClassStateChanged(
                    sumarioSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.SUMARIO, cell.item)
                )
            }
            cell.itemProperty().addListener(listenerCapa)
            cell.itemProperty().addListener(listenerCapaCompleta)
            cell.itemProperty().addListener(listenerSumario)
            obsLImagesSelected.addListener(listenerCapa)
            obsLImagesSelected.addListener(listenerCapaCompleta)
            obsLImagesSelected.addListener(listenerSumario)
            cell
        }
    }

    private fun editaColunas() {
        clCapitulo.cellFactory = TextFieldTableCell.forTableColumn()
        clCapitulo.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].capitulo = e.newValue
            e.tableView.items[e.tablePosition.row]
                .nomePasta = txtNomePastaCapitulo.text.trim { it <= ' ' } + " " + e.newValue
        }
        clNumeroPagina.setCellFactory(TextFieldTableCell.forTableColumn())
        clNumeroPagina.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].setNumero(e.getNewValue())
        }
        clNomePasta.setCellFactory(TextFieldTableCell.forTableColumn())
        clNomePasta.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].nomePasta = e.newValue
        }
    }

    private fun linkaCelulas() {
        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")
        clNumeroPagina.cellValueFactory = PropertyValueFactory("numeroPagina")
        clNomePasta.cellValueFactory = PropertyValueFactory("nomePasta")
        editaColunas()
        selecionaImagens()
    }

    private var pastaAnterior = ""
    private fun configuraTextEdit() {
        textFieldMostraFinalTexto(txtSimularPasta)
        textFieldMostraFinalTexto(txtPastaOrigem)

        txtPastaOrigem.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean ->
                if (newPropertyValue)
                    pastaAnterior = txtPastaOrigem.text

                if (oldPropertyValue && txtPastaOrigem.text.compareTo(pastaAnterior, ignoreCase = true) != 0)
                    carregaPastaOrigem()

                txtPastaOrigem.unFocusColor = Color.GRAY
            }
        txtPastaOrigem.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER)
                txtVolume.requestFocus()
            else if (e.code == KeyCode.TAB && !e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                txtPastaDestino.requestFocus()
                e.consume()
            }
        }

        txtPastaDestino.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean? ->
                if (oldPropertyValue) carregaPastaDestino()
                txtPastaDestino.unFocusColor = Color.GRAY
            }
        txtPastaDestino.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER)
                txtNomePastaManga.requestFocus()
            else if (e.code == KeyCode.TAB && !e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                txtNomePastaManga.requestFocus()
                e.consume()
            }
        }

        txtNomePastaManga.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean? -> if (oldPropertyValue) simulaNome() }
        txtNomePastaManga.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }

        txtNomeArquivo.focusedProperty().addListener { arg0: ObservableValue<out Boolean?>?, oldPropertyValue: Boolean?, newPropertyValue: Boolean? ->
                txtPastaDestino.unFocusColor = Color.GRAY
            }
        txtNomeArquivo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtNomeArquivo.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean? ->
                if (oldPropertyValue && manga == null)
                    manga = geraManga(0)

            }

        txtVolume.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean? ->
                if (oldPropertyValue) {
                    simulaNome()
                    carregaManga()
                }
            }
        txtVolume.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER)
                txtGerarInicio.requestFocus()
            else if (e.code == KeyCode.TAB && !e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                txtGerarInicio.requestFocus()
                e.consume()
            }
        }

        txtNomePastaCapitulo.focusedProperty().addListener { arg0: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean? -> if (oldPropertyValue) simulaNome() }
        txtNomePastaCapitulo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code.toString() == "ENTER") clickTab() }

        txtGerarInicio.focusedProperty().addListener { arg0: ObservableValue<out Boolean?>?, oldPropertyValue: Boolean?, newPropertyValue: Boolean? ->
                txtPastaDestino.unFocusColor = Color.GRAY
            }
        txtGerarInicio.textProperty().addListener { obs: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
                if (newValue != null && !newValue.matches(NUMBER_REGEX))
                    txtGerarInicio.text = oldValue else if (newValue != null && newValue.isEmpty()) txtGerarInicio.text = "0"
            }
        txtGerarInicio.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code.toString() == "ENTER") clickTab() }

        txtGerarFim.focusedProperty().addListener { arg0: ObservableValue<out Boolean?>?, oldPropertyValue: Boolean?, newPropertyValue: Boolean? ->
                txtPastaDestino.unFocusColor = Color.GRAY
            }
        txtGerarFim.textProperty().addListener { obs: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
                if (newValue != null && !newValue.matches(NUMBER_REGEX))
                    txtGerarFim.text = oldValue else if (newValue != null && newValue.isEmpty()) txtGerarFim.text = "0"
            }
        txtGerarFim.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER) {
                onBtnGerarCapitulos()
                txtAreaImportar.requestFocus()
                val position = txtAreaImportar.text.indexOf('-') + 1
                txtAreaImportar.positionCaret(position)
                e.consume()
            } else if (e.code == KeyCode.TAB && !e.isShiftDown) {
                txtAreaImportar.requestFocus()
                e.consume()
            }
        }

        txtAreaImportar.onKeyPressed = EventHandler { e: KeyEvent -> if (e.isControlDown && e.code == KeyCode.ENTER) onBtnImporta() }

        txtQuantidade.focusedProperty().addListener { arg0: ObservableValue<out Boolean?>?, oldPropertyValue: Boolean?, newPropertyValue: Boolean? ->
                txtPastaDestino.unFocusColor = Color.GRAY
            }
        txtQuantidade.textProperty().addListener { obs: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
                if (newValue != null && !newValue.matches(NUMBER_REGEX))
                    txtGerarFim.text = oldValue
            }

        cbMesclarCapaTudo.selectedProperty().addListener { obs: ObservableValue<out Boolean?>?, oldValue: Boolean?, newValue: Boolean? -> reloadCapa() }
        cbAjustarMargemCapa.selectedProperty().addListener { obs: ObservableValue<out Boolean?>?, oldValue: Boolean?, newValue: Boolean? -> reloadCapa() }
    }

    fun configurarAtalhos(scene: Scene) {
        val kcInicioFocus: KeyCombination = KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN)
        val kcFimFocus: KeyCombination = KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
        val kcImportFocus: KeyCombination = KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN)
        val kcImportar: KeyCombination = KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)
        val kcProcessar: KeyCombination = KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN)

        val mnProcessar = Mnemonic(btnProcessar, kcProcessar)
        scene.addMnemonic(mnProcessar)

        val kcProcessarAlter: KeyCombination = KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN)
        val mnProcessarAlter = Mnemonic(btnProcessar, kcProcessarAlter)
        scene.addMnemonic(mnProcessarAlter)

        val kcCompactar: KeyCombination = KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN)
        val mnCompactar = Mnemonic(btnCompactar, kcCompactar)
        scene.addMnemonic(mnCompactar)

        scene.addEventFilter(KeyEvent.KEY_PRESSED) { ke: KeyEvent ->
            if (ke.isControlDown && lsVwListaImagens.selectionModel.selectedItem != null)
                selecionada = lsVwListaImagens.selectionModel.selectedItem

            if (kcInicioFocus.match(ke))
                txtGerarInicio.requestFocus()

            if (kcFimFocus.match(ke))
                txtGerarFim.requestFocus()

            if (kcImportFocus.match(ke))
                txtAreaImportar.requestFocus()

            if (kcImportar.match(ke))
                btnImportar.fire()

            if (kcProcessar.match(ke) || kcProcessarAlter.match(ke))
                btnProcessar.fire()

            if (kcCompactar.match(ke))
                btnCompactar.fire()
        }
    }

    private fun configuraImagens() {

        /*imgTudo.setPreserveRatio(true);
        imgTudo.scaleXProperty().bind(sliderTudo.valueProperty());
        imgTudo.scaleYProperty().bind(sliderTudo.valueProperty());

        imgFrente.setPreserveRatio(true);
        imgFrente.setSmooth(true);
        imgFrente.scaleXProperty().bind(sliderFrente.valueProperty());
        imgFrente.scaleYProperty().bind(sliderFrente.valueProperty());



        imgFrente.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                sliderFrente.setValue(1);
        });

        imgFrente.setOnScroll(e -> {
            if (sliderFrente.getValue() >= sliderFrente.getMin() || sliderFrente.getValue() <= sliderFrente.getMax()) {
                if (e.getDeltaY() > 0)
                    sliderFrente.setValue(sliderFrente.getValue() * 1.1);
                else if (e.getDeltaY() < 0)
                    sliderFrente.setValue(sliderFrente.getValue() / 1.1);
            }
        });


        imgTras.setPreserveRatio(true);
        imgTras.scaleXProperty().bind(sliderTras.valueProperty());
        imgTras.scaleYProperty().bind(sliderTras.valueProperty());*/
    }

    private fun clickTab() {
        val robot = Robot()
        robot.keyPress(KeyCode.TAB)
    }

    @Synchronized
    override fun initialize(arg0: URL, arg1: ResourceBundle?) {
        linkaCelulas()
        limpaCampos()
        configuraTextEdit()
        configuraImagens()
        try {
            WINRAR = loadProperties()!!.getProperty("caminho_winrar")
        } catch (e: Exception) {
            LOG.error("Erro ao obter o caminho do winrar.", e)
        }
    }

    companion object {
        private var WINRAR: String? = null
        private const val IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$"
        private var LAST_PROCESS_FOLDERS: MutableList<File> = ArrayList()
        private const val NUMBER_PATTERN = "[\\d.]+$"
        private val NUMBER_REGEX = Regex("\\d*")

        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/TelaInicial.fxml")
        val iconLocate: String get() = "/images/icoProcessar_512.png"
    }
}