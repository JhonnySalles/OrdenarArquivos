package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.animation.Animacao
import com.fenix.ordenararquivos.components.TextAreaTableCell
import com.fenix.ordenararquivos.exceptions.LibException
import com.fenix.ordenararquivos.model.*
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Capa
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.Processar
import com.fenix.ordenararquivos.model.entities.comet.CoMet
import com.fenix.ordenararquivos.model.entities.comicinfo.*
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.model.enums.Notificacao
import com.fenix.ordenararquivos.model.enums.Tipo
import com.fenix.ordenararquivos.model.enums.TipoCapa
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Ocr
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.SincronizacaoServices
import com.fenix.ordenararquivos.util.Utils
import com.jfoenix.controls.*
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.Unmarshaller
import javafx.animation.Interpolator
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.css.PseudoClass
import javafx.event.Event
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Point2D
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.stage.DirectoryChooser
import javafx.util.Duration
import net.kurobako.gesturefx.GesturePane
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.*
import java.math.RoundingMode
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.properties.Delegates


class TelaInicialController : Initializable {

    private val mLOG = LoggerFactory.getLogger(TelaInicialController::class.java)

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apGlobal: AnchorPane

    @FXML
    private lateinit var rootStackPane: StackPane

    @FXML
    private lateinit var root: AnchorPane

    @FXML
    private lateinit var tbTabRoot: JFXTabPane

    @FXML
    private lateinit var tbTabArquivo: Tab

    @FXML
    private lateinit var tbTabCapa: Tab

    @FXML
    private lateinit var tbTabComicInfo: Tab

    @FXML
    private lateinit var btnCompartilhamento: JFXButton

    @FXML
    private lateinit var imgCompartilhamento: ImageView

    @FXML
    private lateinit var btnLimparTudo: JFXButton

    @FXML
    private lateinit var btnProcessar: JFXButton

    @FXML
    private lateinit var btnCompactar: JFXButton

    @FXML
    private lateinit var btnGerarCapa: JFXButton

    @FXML
    private lateinit var btnAjustarNomes: JFXButton

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
    private lateinit var cbGerarCapitulo: JFXCheckBox

    @FXML
    private lateinit var lsVwListaImagens: JFXListView<String>

    @FXML
    private lateinit var txtGerarInicio: JFXTextField

    @FXML
    private lateinit var txtGerarFim: JFXTextField

    @FXML
    private lateinit var txtSeparadorPagina: JFXTextField

    @FXML
    private lateinit var txtSeparadorCapitulo: JFXTextField

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
    private lateinit var clTag: TableColumn<Caminhos, String>

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

    //<--------------------------  CAPA   -------------------------->

    @FXML
    private lateinit var rootTudo: AnchorPane

    @FXML
    private lateinit var sliderTudo: JFXSlider

    @FXML
    private lateinit var rootFrente: AnchorPane

    @FXML
    private lateinit var sliderFrente: JFXSlider

    @FXML
    private lateinit var rootTras: AnchorPane

    @FXML
    private lateinit var sliderTras: JFXSlider

    //<--------------------------  COMIC INFO   -------------------------->
    @FXML
    private lateinit var txtIdMal: JFXTextField

    @FXML
    private lateinit var cbAgeRating: JFXComboBox<AgeRating>

    @FXML
    private lateinit var cbLanguage: JFXComboBox<Linguagem>

    @FXML
    private lateinit var txtTitle: JFXTextField

    @FXML
    private lateinit var txtSeries: JFXTextField

    @FXML
    private lateinit var txtAlternateSeries: JFXTextField

    @FXML
    private lateinit var txtSeriesGroup: JFXTextField

    @FXML
    private lateinit var txtPublisher: JFXTextField

    @FXML
    private lateinit var txtStoryArc: JFXTextField

    @FXML
    private lateinit var txtImprint: JFXTextField

    @FXML
    private lateinit var txtGenre: JFXTextField

    @FXML
    private lateinit var txtNotes: JFXTextArea

    //<--------------------------  MAL SEARCH   -------------------------->

    @FXML
    private lateinit var txtMalId: JFXTextField

    @FXML
    private lateinit var txtMalNome: JFXTextField

    @FXML
    private lateinit var btnMalAplicar: JFXButton

    @FXML
    private lateinit var btnMalConsultar: JFXButton

    @FXML
    private lateinit var btnGravarComicInfo: JFXButton

    @FXML
    private lateinit var tbViewMal: TableView<Mal>

    @FXML
    private lateinit var clMalId: TableColumn<Mal, String>

    @FXML
    private lateinit var clMalNome: TableColumn<Mal, String>

    @FXML
    private lateinit var clMalSite: TableColumn<Mal, JFXButton?>

    @FXML
    private lateinit var clMalImagem: TableColumn<Mal, ImageView?>

    //<--------------------------  OCR Capítulos   -------------------------->
    @FXML
    private lateinit var txtPastaOcr: JFXTextField

    @FXML
    private lateinit var btnPesquisarPastaOcr: JFXButton

    @FXML
    private lateinit var btnOcrProcessar: JFXButton

    @FXML
    private lateinit var tbViewOcr: TableView<Processar>

    @FXML
    private lateinit var clOCRArquivo: TableColumn<Processar, String>

    @FXML
    private lateinit var clOCRSerie: TableColumn<Processar, String>

    @FXML
    private lateinit var clOCRTitulo: TableColumn<Processar, String>

    @FXML
    private lateinit var clOCREditora: TableColumn<Processar, String>

    @FXML
    private lateinit var clOCRPublicacao: TableColumn<Processar, String>

    @FXML
    private lateinit var clOCRTags: TableColumn<Processar, String>

    @FXML
    private lateinit var clProcessarOCR: TableColumn<Processar, JFXButton?>

    @FXML
    private lateinit var clSalvarOCR: TableColumn<Processar, JFXButton?>


    private val mSugestao: JFXAutoCompletePopup<String> = JFXAutoCompletePopup<String>()

    private var mListaCaminhos: MutableList<Caminhos> = arrayListOf()
    private var mObsListaCaminhos: ObservableList<Caminhos> = FXCollections.observableArrayList(mListaCaminhos)
    private var mObsListaItens: ObservableList<String> = FXCollections.observableArrayList("")
    private var mObsListaImagesSelected: ObservableList<Capa> = FXCollections.observableArrayList()
    private var mObsListaMal: ObservableList<Mal> = FXCollections.observableArrayList()
    private var mObsListaOCR: ObservableList<Processar> = FXCollections.observableArrayList()

    private var mCaminhoOrigem: File? = null
    private var mCaminhoDestino: File? = null
    private var mSelecionado: String? = null
    private var mComicInfo by Delegates.observable(ComicInfo()) { _, _, newValue -> carregaComicInfo(newValue) }
    private val mServiceManga = MangaServices()
    private val mServiceComicInfo = ComicInfoServices()

    private lateinit var mImagemFrente: ImageView
    private lateinit var mGestureFrente: GesturePane
    private lateinit var mImagemTras: ImageView
    private lateinit var mGestureTras: GesturePane
    private lateinit var mImagemTudo: ImageView
    private lateinit var mGestureTudo: GesturePane

    private val animacao = Animacao()
    private var sincronizacao = SincronizacaoServices(this)

    @FXML
    private fun onBtnCompartilhamento() = compartilhamento()

    private fun limpaCampos() {
        limparCapas()
        mListaCaminhos = ArrayList()
        mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
        tbViewTabela.items = mObsListaCaminhos
        mCaminhoOrigem = null
        mCaminhoDestino = null
        lblAlerta.text = ""
        lblAviso.text = ""
        mManga = null
        txtSimularPasta.text = ""
        txtPastaOrigem.text = ""
        txtPastaDestino.text = ""
        txtNomePastaManga.text = "[JPN] Manga -"
        txtVolume.text = "Volume 01"
        txtNomePastaCapitulo.text = "Capítulo"
        txtSeparadorPagina.text = "-"
        txtSeparadorCapitulo.text = "|"
        onBtnLimpar()
        mObsListaItens = FXCollections.observableArrayList("")
        lsVwListaImagens.items = mObsListaItens
        mSelecionado = null
        lblProgresso.text = ""
        pbProgresso.progress = 0.0

        mObsListaMal = FXCollections.observableArrayList()
        tbViewMal.items = mObsListaMal
        mComicInfo = ComicInfo()
        txtMalId.text = ""
        txtMalNome.text = ""

        mObsListaOCR = FXCollections.observableArrayList()
        tbViewOcr.items = mObsListaOCR
    }

    private val mFilterNomeArquivo: FilenameFilter
        get() = FilenameFilter { _: File?, name: String ->
            if (name.lastIndexOf('.') > 0) {
                val p = Pattern.compile(IMAGE_PATTERN)
                return@FilenameFilter p.matcher(name).matches()
            }
            false
        }

    @FXML
    private fun onBtnScrollSubir() {
        if (!lsVwListaImagens.items.isNullOrEmpty())
            lsVwListaImagens.scrollTo(0)
    }

    @FXML
    private fun onBtnScrollBaixo() {
        if (!lsVwListaImagens.items.isNullOrEmpty())
            lsVwListaImagens.scrollTo(lsVwListaImagens.items.size - 1)
    }

    @FXML
    private fun onBtnLimparTudo() {
        limpaCampos()
    }

    @FXML
    private fun onBtnCompactar() {
        if (mCaminhoDestino!!.exists() && txtNomeArquivo.text.isNotEmpty() && LAST_PROCESS_FOLDERS.isNotEmpty())
            compactaArquivo(
                File(mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }),
                LAST_PROCESS_FOLDERS
            )
    }

    @FXML
    private fun onBtnGerarCapa() {
        if (!validaCampos(isCapa = true))
            return

        CompletableFuture.runAsync {
            val nomePasta = (mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' })
            gerarCapa(nomePasta, cbMesclarCapaTudo.isSelected)

            Platform.runLater {
                lblAviso.text = "Imagens de capa gerada com sucesso."
            }
        }
    }

    @FXML
    private fun onAjustarNomes() {
        if (!validaCampos(isAjusteNome = true))
            return

        CompletableFuture.runAsync {
            var padding = 3
            var ajustado = false

            for (arquivo in mCaminhoOrigem!!.listFiles()!!)
                if (arquivo.isFile) {
                    if (arquivo.nameWithoutExtension.length > padding)
                        padding = arquivo.nameWithoutExtension.length
                }

            for (arquivo in mCaminhoOrigem!!.listFiles()!!)
                if (arquivo.isFile) {
                    if (arquivo.nameWithoutExtension.length < padding) {
                        ajustado = true
                        val nome = arquivo.nameWithoutExtension.padStart(padding, '0') + "." + arquivo.extension
                        renomeiaItem(arquivo, nome)
                    }
                }
            Platform.runLater {
                if (ajustado)
                    carregaPastaOrigem()

                lblAviso.text = if (ajustado) "Nomes ajustado com sucesso com sucesso." else "Nenhum arquivo com problemas encontrado."
            }
        }
    }

    @FXML
    private fun onBtnVolumeMenos() {
        // Matches retorna se toda a string for o patern, no caso utiliza-se o inicio
        // para mostrar que tenha em toda a string.
        if (txtVolume.text.matches(Regex(".*$NUMBER_PATTERN$"))) {
            val oldVolume = txtVolume.text
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(NUMBER_PATTERN.toRegex(), ""), "").trim { it <= ' ' }
            val padding = volume.length
            try {
                var number = Integer.valueOf(volume)
                texto = texto.substring(0, texto.lastIndexOf(volume))
                number -= 1
                volume = texto + String.format("%0" + padding + "d", number)
                txtVolume.text = volume
                simulaNome()
                if (!carregaManga())
                    incrementaCapitulos(txtVolume.text, oldVolume)
            } catch (e: NumberFormatException) {
                try {
                    var number = java.lang.Double.valueOf(volume)
                    texto = texto.substring(0, texto.lastIndexOf(volume))
                    number -= 1
                    volume = texto + String.format("%0$padding.1f", number).replace("\\.".toRegex(), "").replace("\\,".toRegex(), ".")
                    txtVolume.text = volume
                    simulaNome()
                    if (!carregaManga())
                        incrementaCapitulos(txtVolume.text, oldVolume)
                } catch (e1: NumberFormatException) {
                    mLOG.info("Erro ao incrementar valor.", e)
                }
            }
        }
    }

    @FXML
    private fun onBtnVolumeMais() {
        if (txtVolume.text.matches(Regex(".*$NUMBER_PATTERN$"))) {
            val oldVolume = txtVolume.text
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(NUMBER_PATTERN.toRegex(), ""), "").trim { it <= ' ' }
            val padding = volume.length
            try {
                var number = Integer.valueOf(volume)
                texto = texto.substring(0, texto.lastIndexOf(volume))
                number += 1
                volume = texto + String.format("%0" + padding + "d", number)
                txtVolume.text = volume
                simulaNome()
                if (!carregaManga())
                    incrementaCapitulos(txtVolume.text, oldVolume)
            } catch (e: NumberFormatException) {
                try {
                    var number = java.lang.Double.valueOf(volume)
                    texto = texto.substring(0, texto.lastIndexOf(volume))
                    number += 1
                    volume = texto + String.format("%0$padding.1f", number).replace("\\.".toRegex(), "").replace("\\,".toRegex(), ".")
                    txtVolume.text = volume
                    simulaNome()
                    carregaManga()
                    if (!carregaManga())
                        incrementaCapitulos(txtVolume.text, oldVolume)
                } catch (e1: NumberFormatException) {
                    mLOG.info("Erro ao incrementar valor.", e)
                }
            }
        }
    }

    @FXML
    private fun onBtnMalAplicar() {
        if (tbViewMal.items.isNotEmpty())
            carregaMal(tbViewMal.selectionModel.selectedItem)
    }

    @FXML
    private fun onBtnMalConsultar() {
        tbTabRoot.selectionModel.select(tbTabComicInfo)
        consultarMal()
    }

    @FXML
    private fun onBtnGravarComicInfo() {
        mServiceComicInfo.save(mComicInfo)

        val comicInfo = File(mCaminhoDestino!!.path.trim { it <= ' ' }, "ComicInfo.xml")
        if (comicInfo.exists())
            comicInfo.delete()

        try {
            mLOG.info("Salvando xml do ComicInfo.")
            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(comicInfo)
            marshaller.marshal(mComicInfo, out)
            out.close()

            val arquivoZip = mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }
            if (compactaArquivo(File(arquivoZip), comicInfo))
                Notificacoes.notificacao(Notificacao.SUCESSO, "ComicInfo", "ComicInfo gerado e compactado.")
            else {
                txtSimularPasta.text = "Erro ao compactar o ComicInfo, necessário compacta-lo manualmente."
                Notificacoes.notificacao(Notificacao.ALERTA, "ComicInfo", "Erro ao compactar o ComicInfo, necessário compacta-lo manualmente.")
            }
        } catch (e: Exception) {
            mLOG.error("Erro ao gerar o xml do ComicInfo.", e)
        }

        try {
            mLOG.info("Salvando xml do CoMet.")

            val file = File(mCaminhoDestino!!.path.trim { it <= ' ' }, "CoMet.xml")
            val comet: CoMet = if (file.exists()) {
                try {
                    val unmarshaller: Unmarshaller = JAXBContext.newInstance(CoMet::class.java).createUnmarshaller()
                    unmarshaller.unmarshal(file) as CoMet
                } catch (e: Exception) {
                    mLOG.error(e.message, e)
                    CoMet(mComicInfo)
                }
            } else
                CoMet(mComicInfo)

            comet.toCoMet(mComicInfo)

            val marshaller = JAXBContext.newInstance(CoMet::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(file)
            marshaller.marshal(comet, out)
            out.close()

            val arquivoZip = mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }
            compactaArquivo(File(arquivoZip), file)
        } catch (e: Exception) {
            mLOG.error("Erro ao gerar o xml do CoMet.", e)
        }
    }

    @FXML
    private fun onBtnOcrProcessar() {
        carregarItensOcr()
    }

    @FXML
    private fun onBtnCarregarPastaOcr() {
        val caminho = selecionaPasta(txtPastaOcr.text)
        if (caminho != null)
            txtPastaOcr.text = caminho.absolutePath
        else
            txtPastaOcr.text = ""
        carregarItensOcr()
    }

    private fun desabilita() {
        btnGerarCapa.isDisable = true
        btnLimparTudo.isDisable = true
        btnCompactar.isDisable = true
        btnAjustarNomes.isDisable = true
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
        btnGerarCapa.isDisable = false
        btnLimparTudo.isDisable = false
        btnCompactar.isDisable = false
        btnAjustarNomes.isDisable = false
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

    private fun validaCampos(isCapa: Boolean = false, isAjusteNome: Boolean = false): Boolean {
        var valida = true
        if (mCaminhoOrigem == null || !mCaminhoOrigem!!.exists()) {
            txtSimularPasta.text = "Origem não informado."
            txtPastaOrigem.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Origem não informado.")
            valida = false
        }

        if (isAjusteNome)
            return valida

        if (mCaminhoDestino == null || !mCaminhoDestino!!.exists()) {
            txtSimularPasta.text = "Destino não informado."
            txtPastaDestino.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Destino não informado.")
            valida = false
        }

        if (mObsListaCaminhos.isEmpty())
            valida = false

        if (isCapa)
            return valida

        if (lsVwListaImagens.selectionModel.selectedItem == null)
            lsVwListaImagens.selectionModel.select(0)

        if (cbCompactarArquivo.isSelected && txtNomeArquivo.text.isEmpty()) {
            txtSimularPasta.text = "Não informado nome do arquivo."
            txtNomeArquivo.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Não informado nome do arquivo.")
            valida = false
        }

        if (cbLanguage.value == null) {
            cbLanguage.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Necessário informar uma linguagem.")
            valida = false
        }

        if (mCaminhoOrigem != null && mObsListaImagesSelected.isNotEmpty()) {
            var itens = ""
            for (capa in mObsListaImagesSelected) {
                if (capa.nome.isNotEmpty() && !File(mCaminhoOrigem!!.path + "\\" + capa.nome).exists())
                    itens += capa.nome + ", "

                if (capa.direita != null && capa.direita!!.nome.isNotEmpty() && !File(mCaminhoOrigem!!.path + "\\" + capa.direita!!.nome).exists())
                    itens += capa.nome + ", "
            }

            if (itens.isNotEmpty()) {
                AlertasPopup.alertaModal("Alerta", "Alguns arquivos selecionados não foram encontrados, verifique os arquivos na pasta de origem.\n" + itens.substringBeforeLast(", ") + ".")
                valida = false
            }
        }

        return valida
    }

    private fun carregaComicInfo(comic: ComicInfo) {
        txtIdMal.text = if (comic.idMal != null) comic.idMal.toString() else ""
        cbAgeRating.selectionModel.select(comic.ageRating)
        val lingua = Linguagem.getEnum(comic.languageISO) ?: cbLanguage.value
        cbLanguage.selectionModel.select(lingua)
        txtTitle.text = comic.title
        txtSeries.text = comic.series
        txtAlternateSeries.text = comic.alternateSeries
        txtSeriesGroup.text = comic.seriesGroup
        txtPublisher.text = comic.publisher
        txtStoryArc.text = comic.storyArc
        txtImprint.text = comic.imprint
        txtGenre.text = comic.genre
        txtNotes.text = comic.notes
    }

    fun atualizaComicInfo(comic: ComicInfo) {
        comic.idMal = if (txtIdMal.text.isNotEmpty()) txtIdMal.text.toLong() else null
        comic.ageRating = cbAgeRating.value
        comic.languageISO = cbLanguage.value.sigla
        comic.title = txtTitle.text
        comic.series = txtSeries.text
        comic.alternateSeries = txtAlternateSeries.text.ifEmpty { null }
        comic.seriesGroup = txtSeriesGroup.text.ifEmpty { null }
        comic.publisher = txtPublisher.text.ifEmpty { null }
        comic.storyArc = txtStoryArc.text.ifEmpty { null }
        comic.imprint = txtImprint.text.ifEmpty { null }
        comic.genre = txtGenre.text.ifEmpty { null }
        comic.notes = txtNotes.text.ifEmpty { null }
    }

    private fun carregaMal(mal: Mal) {
        val comic = ComicInfo(mComicInfo)
        mServiceComicInfo.updateMal(comic, mal, cbLanguage.value ?: Linguagem.JAPANESE)
        mComicInfo = comic
    }

    private fun ocrSumario(sumario : File) {
        remSugestao()
        val isJapanese = txtNomePastaManga.text.contains("[JPN]", true)
        val ocr: Task<Void> = object : Task<Void>() {
            override fun call(): Void? {
                try {
                    if (!Ocr.mGemini && !Ocr.mLibs)
                        throw LibException("Bibliotecas OCR não instânciadas.")

                    Ocr.prepare(isJapanese)
                    val sugestao = Ocr.process(sumario, txtSeparadorPagina.text, txtSeparadorCapitulo.text)
                    mLOG.info("OCR processado: $sugestao")
                    if (sugestao.isNotEmpty())
                        Platform.runLater {
                            addSugestao(sugestao)
                        }
                } catch (e: Exception) {
                    mLOG.info("Erro ao realizar o OCR do arquivo de sumário.", e)
                } finally {
                    Ocr.clear()
                }
                return null
            }
            override fun succeeded() { }
        }

        Thread(ocr).start()
    }

    private fun addSugestao(textos : String) {
        mLOG.info("Sugestão: $textos")
        if (textos.isNotEmpty()) {
            mSugestao.fixedCellSize = 24.0 + (if (textos.contains('\n')) ((textos.count { it == '\n' } - 1) * 18.0) else 0.0)
            mSugestao.suggestions.clear()
            mSugestao.suggestions.add(textos)
            mSugestao.show(txtAreaImportar)
            mSugestao.setSelectionHandler {
                txtAreaImportar.text = it.`object` ?: ""
            }
        }
    }

    private fun remSugestao() {
        mSugestao.suggestions.clear()
        mSugestao.hide()
    }

    private fun consultarMal() {
        if (txtMalId.text.isNotEmpty() || txtMalNome.text.isNotEmpty()) {
            val id : Long? = if (txtMalId.text.isNotEmpty()) txtMalId.text.toLong() else null
            val nome = txtMalNome.text

            btnMalConsultar.isDisable = true
            val consulta: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mServiceComicInfo.getMal(id, nome)
                        mObsListaMal = FXCollections.observableArrayList(lista)
                        Platform.runLater {
                            tbViewMal.items = mObsListaMal
                            if (lista.isEmpty())
                                Notificacoes.notificacao(Notificacao.ALERTA, "My Anime List", "Nenhum item encontrado.")
                            else if (id != null && lista.size == 1)
                                carregaMal(lista.first())
                        }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao realizar a consulta do MyAnimeList.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "My Anime List", "Erro ao realizar a consulta do MyAnimeList. " + e.message)
                        }
                    }
                    return null
                }
                override fun succeeded() {
                    Platform.runLater {
                        btnMalConsultar.isDisable = false
                    }
                }
            }

            Thread(consulta).start()
        } else {
            AlertasPopup.alertaModal("Alerta", "Necessário informar um id ou nome.")
            txtMalNome.requestFocus()
        }
    }

    private var mManga: Manga? = null
    private fun geraManga(id: Long): Manga {
        var nome = txtNomePastaManga.text
        if (nome.contains("]"))
            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }

        if (nome.substring(nome.length - 1).equals("-", ignoreCase = true))
            nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }

        val quantidade = mObsListaItens.size

        return Manga(
            id, nome, txtVolume.text, txtNomePastaCapitulo.text.trim { it <= ' ' },
            txtNomeArquivo.text.trim { it <= ' ' }, quantidade, txtAreaImportar.text, LocalDateTime.now()
        )
    }

    private fun carregaManga(): Boolean {
        mManga = mServiceManga.find(geraManga(0))

        lblAviso.text = if (mManga != null) "Manga localizado." else "Manga não localizado."

        mManga?.let {
            txtNomePastaManga.text = "[JPN] " + it.nome + " - "
            txtVolume.text = it.volume
            txtNomePastaCapitulo.text = it.capitulo
            txtNomeArquivo.text = it.arquivo
            txtAreaImportar.text = it.capitulos

            val quantidade = mObsListaItens.size
            lblAlerta.text = if (it.quantidade != quantidade) "Difereça na quantidade de imagens." else ""

            if (it.quantidade != quantidade)
                Notificacoes.notificacao(Notificacao.ALERTA, "Diferença", "Difereça na quantidade de imagens.")

            mListaCaminhos = ArrayList(it.caminhos)
            mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
            tbViewTabela.items = mObsListaCaminhos

            try {
                var min = 0
                var max = 0

                if (it.caminhos.isNotEmpty()) {
                    for (caminho in it.caminhos) {
                        val capitulo = caminho.capitulo
                        if (capitulo.trim().isEmpty() || !ONLY_NUMBER_REGEX.containsMatchIn(capitulo))
                            continue

                        val cap = capitulo.replace(Regex("\\D"), "").toInt()
                        if (min == 0 || min > cap)
                            min = cap
                        else
                            max = cap
                    }
                } else {
                    val linhas = it.capitulos.split("\n")
                    for (linha in linhas) {
                        val numero = if (linha.contains("-")) linha.substringBeforeLast("-") else linha
                        if (numero.trim().isNotEmpty() && ONLY_NUMBER_REGEX.containsMatchIn(numero)) {
                            val capitulo = getNumber(numero) ?: continue
                            if (min == 0 || min > capitulo)
                                min = capitulo.toInt()
                            else
                                max = capitulo.toInt()
                        }
                    }
                }

                txtGerarInicio.text = min.toString()
                txtGerarFim.text = max.toString()
            } catch (e: Exception) {
                mLOG.error("Erro ao carregar os capítulos de inicio e fim.", e)
            }
        }

        return mManga != null
    }

    private fun carregaMangaAnterior(): Boolean {
        val psq = geraManga(0)
        val manga = mServiceManga.find(psq, anterior = true)
        lblAviso.text = lblAviso.text + " -- " + if (manga != null) "Volume anterior localizado." else "Volume anterior não localizado."

        manga?.let {
            txtNomePastaManga.text = "[JPN] " + it.nome + " - "
            txtNomePastaCapitulo.text = it.capitulo

            val linhas = it.capitulos.split("\n")
            var min = 0.0
            var max = 0.0

            for (linha in linhas) {
                val numero = if (linha.contains("-")) linha.substringBeforeLast("-") else linha
                if (numero.trim().isNotEmpty() && ONLY_NUMBER_REGEX.containsMatchIn(numero)) {
                    val capitulo = getNumber(numero) ?: continue
                    if (min == 0.0 || min > capitulo)
                        min = capitulo
                    else
                        max = capitulo
                }
            }

            if (min > 0 && max > 0) {
                val vol = try {
                    (getNumber(psq.volume) ?: 0).toInt() - (getNumber(it.volume) ?: 0).toInt()
                } catch (e : Exception) {
                    0
                }

                // Arredonda para cima, ou seja, em caso de 19.1, arredonda para 20.
                val dif = DecimalFormat("#").apply { roundingMode = RoundingMode.CEILING }.format(max - min).toInt()
                val initial = if (vol > 1) ((dif * vol) + max + 1) else (max + 1)
                txtGerarInicio.text = initial.toInt().toString()
                txtGerarFim.text = (initial + dif).toInt().toString()
                onBtnGerarCapitulos()
            }
        }

        return mManga != null
    }

    private fun salvaManga() {
        mManga = if (mManga == null) geraManga(0) else geraManga(mManga!!.id)
        mManga!!.caminhos.clear()
        for (caminho in mListaCaminhos)
            mManga!!.addCaminhos(caminho)
        mServiceManga.save(mManga!!)
        Platform.runLater {
            lblAlerta.text = ""
            lblAviso.text = "Manga salvo."
            Notificacoes.notificacao(Notificacao.SUCESSO, "Sucesso", "Manga salvo.")
        }
    }

    private fun carregaComicInfo() {
        var nome = txtNomePastaManga.text
        if (nome.contains("]"))
            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }

        if (nome.substring(nome.length - 1).equals("-", ignoreCase = true))
            nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }

        mComicInfo = mServiceComicInfo.find(nome, cbLanguage.value.sigla) ?: ComicInfo(null, null, nome, nome)

        if (mComicInfo.id == null) {
            mLOG.info("Gerando novo ComicInfo.")
            txtMalId.text = ""
        } else {
            mLOG.info("ComicInfo localizado: " + mComicInfo.title)
            txtMalId.text = mComicInfo.idMal.toString()
        }

        if (mComicInfo.comic.isEmpty())
            mComicInfo.comic = nome

        txtMalNome.text = mComicInfo.comic
        consultarMal()
    }

    private fun criaPasta(caminho: String): File {
        val arquivo = File(caminho)
        if (!arquivo.exists())
            arquivo.mkdir()
        return arquivo
    }

    @Throws(IOException::class)
    private fun copiaItem(arquivo: File, destino: File, nome: String = arquivo.name): File {
        val path = Paths.get(destino.toPath().toString() + "/" + nome)
        return Files.copy(arquivo.toPath(), path, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING).toFile()
    }

    @Throws(IOException::class)
    private fun renomeiaItem(arquivo: File, nome: String): File {
        val path = arquivo.toPath()
        return Files.move(path, path.resolveSibling(nome), StandardCopyOption.REPLACE_EXISTING).toFile()
    }

    private fun deletaItem(item: String) {
        val arquivo = File(item)
        if (arquivo.exists()) arquivo.delete()
    }

    private fun verificaPaginaDupla(arquivo: File): Boolean {
        var result = false
        try {
            val img: BufferedImage? = ImageIO.read(arquivo)
            result = img!!.width / img.height > 0.9
        } catch (e: IOException) {
            mLOG.error("Erro ao verificar a página dupla.", e)
        }
        return result
    }

    private val mPASTA_TEMPORARIA = File(System.getProperty("user.dir"), "temp/")
    private fun limparCapas() {
        mImagemTudo.image = null
        mImagemFrente.image = null
        mImagemTras.image = null
        mObsListaImagesSelected.clear()
        if (!mPASTA_TEMPORARIA.exists())
            mPASTA_TEMPORARIA.mkdir()
        else {
            for (item in mPASTA_TEMPORARIA.listFiles())
                item.delete()
        }
        remSugestao()
    }

    private fun simularCapa(tipo: TipoCapa, imagem: Image?) {
        when (tipo) {
            TipoCapa.CAPA -> {
                mImagemFrente.image = imagem
                imagem?.let { mGestureFrente.zoomTo(0.1, Point2D(it.width / 2, it.height / 2)) }
            }
            TipoCapa.TRAS -> {
                mImagemTras.image = imagem
                imagem?.let { mGestureTras.zoomTo(0.1, Point2D(it.width / 2, it.height / 2)) }
            }
            TipoCapa.CAPA_COMPLETA -> {
                mImagemTudo.image = imagem
                imagem?.let { mGestureTudo.zoomTo(0.1, Point2D(it.width / 2, it.height / 2)) }
            }
            else -> {}
        }
    }

    private fun remCapa(arquivo: String) {
        val capa = mObsListaImagesSelected.stream().filter { it.nome.equals(arquivo, ignoreCase = true) }.findFirst()
        if (capa.isPresent)
            mObsListaImagesSelected.remove(capa.get())
    }

    private fun addCapa(tipo: TipoCapa, arquivo: String) {
        val img = File(txtPastaOrigem.text + "\\" + arquivo)

        if (!img.exists())
            return

        val isDupla = isPaginaDupla(img)
        if (tipo === TipoCapa.CAPA_COMPLETA) {
            var capas = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(tipo) == 0 && it.direita != null }.findFirst()

            if (capas.isEmpty)
                capas = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(tipo) == 0 }.findFirst()

            val frente = if (capas.isPresent) capas.get() else null
            val tras = if (capas.isPresent) capas.get().direita else null

            if (isDupla) {
                val nome = img.name.substring(0, img.name.lastIndexOf("."))
                val ext = img.name.substring(img.name.lastIndexOf("."))
                val direita = File(mPASTA_TEMPORARIA.toString() + "\\" + nome + TRAS + ext)
                val esquerda = File(mPASTA_TEMPORARIA.toString() + "\\" + nome + FRENTE + ext)
                copiaItem(img, mPASTA_TEMPORARIA)
                divideImagens(img, esquerda, direita)
                mObsListaImagesSelected.removeIf { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }
                mObsListaImagesSelected.add(Capa(arquivo, esquerda.name, tipo, isDupla))
                mObsListaImagesSelected.removeIf { it.tipo.compareTo(TipoCapa.TRAS) == 0 }
                mObsListaImagesSelected.add(Capa(arquivo, direita.name, TipoCapa.TRAS, false))
            } else if (frente == null) {
                remCapa(arquivo)
                mObsListaImagesSelected.add(Capa(arquivo, img.name, tipo, isDupla))
                copiaItem(img, mPASTA_TEMPORARIA)
            } else if (tras == null) {
                frente.direita = Capa(arquivo, img.name, tipo, isDupla)
                mObsListaImagesSelected.add(frente.direita)
                mObsListaImagesSelected.removeIf { it.tipo.compareTo(TipoCapa.TRAS) == 0 }
                mObsListaImagesSelected.add(Capa(arquivo, img.name, TipoCapa.TRAS, false))
                copiaItem(img, mPASTA_TEMPORARIA)
            }
        } else {
            val capa = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(tipo) == 0 }.findFirst().orElse(Capa())
            capa.tipo = tipo
            capa.nome = arquivo
            capa.arquivo = img.name
            capa.isDupla = isDupla
            mObsListaImagesSelected.remove(capa)
            mObsListaImagesSelected.add(capa)
            copiaItem(img, mPASTA_TEMPORARIA)

            if (tipo == TipoCapa.SUMARIO)
                ocrSumario(File(mPASTA_TEMPORARIA.toString() + "\\" + arquivo))
        }
    }

    private fun reloadCapa() {
        if (mObsListaImagesSelected.isEmpty())
            return
        CompletableFuture.runAsync {
            for (capa in mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) != 0 }.toList()) {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + capa.nome), mPASTA_TEMPORARIA)
                    simularCapa(capa.tipo, carregaImagem(File(mPASTA_TEMPORARIA.toString() + "\\" + capa.arquivo)))
                } catch (e: IOException) {
                    mLOG.warn("Erro ao reprocessar imagem: " + capa.tipo + ".", e)
                }
            }
            var completa = mObsListaImagesSelected.stream()
                .filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }.findFirst()
            if (completa.isEmpty)
                completa = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }.findFirst()

            if (completa.isPresent) if (completa.get().direita != null) {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().nome), mPASTA_TEMPORARIA)
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().direita!!.nome), mPASTA_TEMPORARIA)
                    simularCapa(
                        TipoCapa.CAPA_COMPLETA, carregaImagem(
                            File(mPASTA_TEMPORARIA.toString() + "\\" + completa.get().arquivo),
                            File(mPASTA_TEMPORARIA.toString() + "\\" + completa.get().direita!!.arquivo)
                        )
                    )
                } catch (e: IOException) {
                    mLOG.warn("Erro ao reprocessar imagem: " + completa.get().tipo + ".", e)
                }
            } else {
                try {
                    copiaItem(File(txtPastaOrigem.text + "\\" + completa.get().nome), mPASTA_TEMPORARIA)
                    simularCapa(
                        TipoCapa.CAPA_COMPLETA,
                        carregaImagem(File(mPASTA_TEMPORARIA.toString() + "\\" + completa.get().arquivo))
                    )
                } catch (e: IOException) {
                    mLOG.warn("Erro ao reprocessar imagem: " + completa.get().tipo + ".", e)
                }
            }
        }
    }

    private var mCANCELAR = false
    private val FRENTE = " Frente"
    private val TUDO = " Tudo"
    private val TRAS = " Tras"
    private val SUMARIO = " zSumário"

    private fun gerarCapa(nomePasta: String, mesclarCapaTudo: Boolean): File {
        val destinoCapa = criaPasta("$nomePasta Capa\\")
        if (!mObsListaImagesSelected.isEmpty()) {
            mLOG.info("Processando imagens de capa.")
            var nome = txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' }
            if (nome.contains("]"))
                nome = nome.substring(nome.indexOf(']') + 1).trim { it <= ' ' }

            val capa = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA) == 0 }.findFirst()

            if (capa.isPresent)
                limpaMargemImagens(
                    renomeiaItem(
                        copiaItem(
                            File(mCaminhoOrigem!!.path + "\\" + capa.get().nome),
                            destinoCapa
                        ), nome + FRENTE + capa.get().nome.substring(capa.get().nome.lastIndexOf("."))
                    ), false
                )
            mLOG.info("Gerando capa da frente... " + if (capa.isPresent) (" processado. Imagem: " + capa.get().nome) else " não localizado.")

            val tras = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.TRAS) == 0 }.findFirst()
            if (tras.isPresent)
                limpaMargemImagens(
                    renomeiaItem(
                        copiaItem(
                            File(mCaminhoOrigem!!.path + "\\" + tras.get().nome),
                            destinoCapa
                        ), nome + TRAS + tras.get().nome.substring(tras.get().nome.lastIndexOf("."))
                    ), true
                )
            mLOG.info("Gerando capa de tras... " + if (tras.isPresent) (" processado. Imagem: " + tras.get().nome) else " não localizado.")

            val sumario = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.SUMARIO) == 0 }.findFirst()

            if (sumario.isPresent)
                renomeiaItem(
                    copiaItem(
                        File(mCaminhoOrigem!!.path + "\\" + sumario.get().nome),
                        destinoCapa
                    ), nome + SUMARIO + sumario.get().nome.substring(sumario.get().nome.lastIndexOf("."))
                )
            mLOG.info("Gerando sumário... " + if (sumario.isPresent) (" processado. Imagem: " + sumario.get().nome) else " não localizado.")

            if (mObsListaImagesSelected.stream().anyMatch { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }) {
                val tudo = mObsListaImagesSelected.stream()
                    .filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.direita != null }
                    .findFirst()

                if (mesclarCapaTudo) {
                    mLOG.info("Gerando capa completa...  mesclando arquivos....")
                    mLOG.info("Imagem frente: " + tudo.get().nome)
                    mLOG.info("Imagem trazeira: " + tudo.get().direita!!.nome)
                    copiaItem(File(mCaminhoOrigem!!.path + "\\" + tudo.get().nome), mPASTA_TEMPORARIA)
                    copiaItem(File(mCaminhoOrigem!!.path + "\\" + tudo.get().direita!!.nome), mPASTA_TEMPORARIA)
                    val esquerda = File(mPASTA_TEMPORARIA, tudo.get().nome)
                    val direita = File(mPASTA_TEMPORARIA, tudo.get().direita!!.nome)
                    val destino = File(destinoCapa.path + "\\" + nome + TUDO + ".png")
                    mesclarImagens(destino, esquerda, direita)
                    limpaMargemImagens(destino, true)
                    mLOG.info("Mesclagem concluida.")
                } else {
                    mLOG.info("Gerando capa completa...  copiando arquivo....")
                    mLOG.info("Imagem: " + tudo.get().nome)
                    val arquivo = File(
                        destinoCapa, nome + TUDO + tudo.get().nome.substring(
                            tudo.get().nome.lastIndexOf(".")
                        )
                    )
                    renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                    limpaMargemImagens(arquivo, true)
                    mLOG.info("Cópia concluída.")
                }

                if (tras.isEmpty || capa.isEmpty) {
                    mLOG.info("Copiando capa de frente e traz...")
                    val esquerda = File(mPASTA_TEMPORARIA, nome + FRENTE + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                    val direita = File(mPASTA_TEMPORARIA, nome + TRAS + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))

                    if (capa.isEmpty)
                        renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().nome), destinoCapa), esquerda.name)

                    if (tras.isEmpty)
                        renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().direita!!.nome), destinoCapa), direita.name)

                    mLOG.info("Copiando concluída.")
                }
            } else if (mObsListaImagesSelected.stream()
                    .anyMatch { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla } || mObsListaImagesSelected.stream()
                    .anyMatch { it.tipo.compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla }
            ) {
                var tudo = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla }.findFirst()
                if (tudo.isEmpty)
                    tudo = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla }.findFirst()

                mLOG.info("Gerando capa completa... copiando arquivo....")
                mLOG.info("Imagem: " + tudo.get().nome)
                val arquivo = File(
                    destinoCapa, nome + TUDO + tudo.get().nome.substring(
                        tudo.get().nome.lastIndexOf(".")
                    )
                )
                renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                limpaMargemImagens(arquivo, true)
                mLOG.info("Cópia concluída.")

                if (tras.isEmpty || capa.isEmpty) {
                    mLOG.info("Dividindo a capa completa para gerar a capa de frente e traz...")
                    copiaItem(File(mCaminhoOrigem!!.path + "\\" + tudo.get().nome), mPASTA_TEMPORARIA)
                    val temp = File(mPASTA_TEMPORARIA, tudo.get().nome)
                    val esquerda = File(mPASTA_TEMPORARIA, nome + FRENTE + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                    val direita = File(mPASTA_TEMPORARIA, nome + TRAS + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))
                    if (divideImagens(temp, esquerda, direita)) {
                        if (capa.isEmpty)
                            copiaItem(esquerda, destinoCapa)

                        if (tras.isEmpty)
                            copiaItem(direita, destinoCapa)
                    }
                    mLOG.info("Divisão concluída.")
                }
            } else {
                val tudo = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }.findFirst()
                if (tudo.isPresent) {
                    mLOG.info("Copiando capa completa...")
                    val arquivo = File(
                        destinoCapa, nome + TUDO + tudo.get().nome.substring(
                            tudo.get().nome.lastIndexOf(".")
                        )
                    )
                    renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().nome), destinoCapa), arquivo.name)
                    limpaMargemImagens(arquivo, true)
                    mLOG.info("Cópia concluída.")

                    if (capa.isEmpty) {
                        mLOG.info("Copiando capa de frente e traz...")
                        val esquerda = File(mPASTA_TEMPORARIA, nome + FRENTE + tudo.get().nome.substring(tudo.get().nome.lastIndexOf(".")))

                        if (capa.isEmpty)
                            renomeiaItem(copiaItem(File(mCaminhoOrigem, tudo.get().nome), destinoCapa), esquerda.name)

                        mLOG.info("Copiando concluída.")
                    }
                }
            }
        }

        return destinoCapa
    }

    private fun processar() {
        val movimentaArquivos: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                try {
                    salvaManga()
                    if (lsVwListaImagens.selectionModel.selectedItem != null)
                        mSelecionado = lsVwListaImagens.selectionModel.selectedItem

                    mCANCELAR = false
                    var i = 0
                    var max: Int = mCaminhoOrigem!!.listFiles(mFilterNomeArquivo).size
                    val pastasCompactar: MutableList<File> = ArrayList()
                    LAST_PROCESS_FOLDERS.clear()
                    val arquivoZip = mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }
                    val mesclarCapaTudo = cbMesclarCapaTudo.isSelected
                    val gerarArquivo = cbCompactarArquivo.isSelected
                    val verificaPagDupla = cbVerificaPaginaDupla.isSelected
                    val pastasComic = mutableMapOf<String, File>()
                    updateProgress(i.toLong(), max.toLong())

                    updateMessage("Criando diretórios...")
                    val nomePasta = (mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' })
                    updateMessage("Criando diretórios - $nomePasta Capa\\")
                    pastasCompactar.add(gerarCapa(nomePasta, mesclarCapaTudo))
                    pastasComic["000"] = pastasCompactar[0]

                    var pagina = 0
                    var proxCapitulo = 0
                    var contar = false
                    var destino = criaPasta(nomePasta + " " + mListaCaminhos[pagina].nomePasta + "\\")
                    pastasCompactar.add(destino)
                    pastasComic[mListaCaminhos[pagina].capitulo] = destino

                    var contadorCapitulo = Integer.valueOf(mListaCaminhos[pagina].numeroPagina)
                    pagina++
                    if (mListaCaminhos.size > 1)
                        proxCapitulo = mListaCaminhos[pagina].numero

                    for (arquivos in mCaminhoOrigem!!.listFiles(mFilterNomeArquivo).sorted()) {
                        if (mCANCELAR)
                            return true

                        mLOG.info("Contar: " + contar + " - Contador: " + contadorCapitulo + " - Prox cap: " + proxCapitulo + " - Nome Imagem: " + arquivos.name)

                        if (arquivos.name.equals(mSelecionado, ignoreCase = true))
                            contar = true

                        if (contar && verificaPagDupla) {
                            if (verificaPaginaDupla(arquivos))
                                contadorCapitulo++
                        }

                        if (contadorCapitulo >= proxCapitulo && pagina < mListaCaminhos.size) {
                            updateMessage("Criando diretório - " + nomePasta + " " + mListaCaminhos[pagina].nomePasta + "\\")
                            destino = criaPasta(nomePasta + " " + mListaCaminhos[pagina].nomePasta + "\\")
                            pastasCompactar.add(destino)
                            pastasComic[mListaCaminhos[pagina].capitulo] = destino
                            pagina++
                            if (pagina < mListaCaminhos.size)
                                proxCapitulo = Integer.valueOf(mListaCaminhos[pagina].numeroPagina)
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


                    updateMessage("Gerando o comic info..")
                    val isJapanese = nomePasta.contains("[JPN]")
                    val imagens = ".*\\.(jpg|jpeg|bmp|gif|png|webp)$".toRegex()

                    i = 0
                    max = 0
                    for (key in pastasComic.keys) {
                        if (pastasComic[key]!!.listFiles() != null)
                            for (capa in pastasComic[key]!!.listFiles()!!)
                                max++
                    }

                    pagina = 0
                    val pages = mutableListOf<Pages>()
                    var capitulo = ""
                    for (key in pastasComic.keys) {
                        val pasta = pastasComic[key]!!
                        if (pasta.listFiles() != null)
                            for (capa in pasta.listFiles()!!) {
                                if (!capa.name.lowercase(Locale.getDefault()).matches(imagens))
                                    continue

                                mLOG.info("Gerando pagina do ComicInfo: " + capa.name)
                                i++
                                updateProgress(i.toLong(), max.toLong())
                                updateMessage("Processando item " + i + " de " + max + ". Gerando ComicInfo - Capítulo $key | " + capa.name)

                                val page = Pages()
                                val imagem: String = capa.name.lowercase(Locale.getDefault())
                                if (imagem.contains("frente")) {
                                    page.bookmark = "Cover"
                                    page.type = ComicPageType.FrontCover
                                } else if (imagem.contains("tras")) {
                                    page.bookmark = "Back"
                                    page.type = ComicPageType.BackCover
                                } else if (imagem.contains("tudo")) {
                                    page.bookmark = "All cover"
                                    page.doublePage = true
                                    page.type = ComicPageType.Other
                                } else if (imagem.contains("zsumário") || imagem.contains("zsumario")) {
                                    page.bookmark = "Sumary"
                                    page.type = ComicPageType.InnerCover
                                } else {
                                    if (!capitulo.equals(key, true) && !key.equals("000", true)  && !key.lowercase().contains("extra")) {
                                        capitulo = key
                                        val tag = if (cbGerarCapitulo.isSelected) {
                                            val caminho = mListaCaminhos.stream().filter { it.capitulo.equals(key, ignoreCase = true) }.findFirst()
                                            if (caminho.isPresent) " - " + caminho.get().tag else ""
                                        } else
                                            ""
                                        page.bookmark = if (isJapanese)
                                            "第${capitulo}話$tag"
                                        else
                                            "Capítulo $capitulo$tag"
                                    }
                                }

                                try {
                                    val input = FileInputStream(capa)
                                    val image = Image(input)
                                    page.imageWidth = image.width.toInt()
                                    page.imageHeight = image.height.toInt()
                                    input.close()
                                } catch (e: IOException) {
                                    mLOG.error("Erro ao obter os tamanhos da imagem.", e)
                                }
                                if (page.imageWidth != null && page.imageHeight != null && page.imageHeight!! > 0)
                                    if (page.imageWidth!! / page.imageHeight!! > 0.9)
                                        page.doublePage = true

                                page.imageSize = capa.length()
                                page.image = pagina++
                                pages.add(page)
                            }
                    }

                    val comic = mComicInfo
                    comic.pages = pages
                    comic.pageCount = pages.size

                    comic.let {
                        if (it.comic.isEmpty())
                            it.comic = mManga!!.nome
                        if (it.title.isEmpty())
                            it.title = mManga!!.nome

                        it.number = mManga!!.volume.replace(Regex("\\D"), "").toFloat()
                        it.volume = it.number.toInt()
                        it.count = it.volume

                        it.languageISO = if (isJapanese) "ja" else "pt"
                    }

                    mServiceComicInfo.save(comic)

                    val comicInfo = File(mCaminhoDestino!!.path.trim { it <= ' ' }, "ComicInfo.xml")
                    if (comicInfo.exists())
                        comicInfo.delete()

                    try {
                        mLOG.info("Salvando xml do ComicInfo.")
                        val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
                        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                        val out = FileOutputStream(comicInfo)
                        marshaller.marshal(comic, out)
                        out.close()
                        pastasCompactar.add(comicInfo)
                    } catch (e: Exception) {
                        mLOG.error("Erro ao gerar o xml do ComicInfo.", e)
                    }

                    val comet = File(mCaminhoDestino!!.path.trim { it <= ' ' }, "CoMet.xml")
                    if (comet.exists())
                        comet.delete()

                    val paths = mutableListOf<String>()
                    for (pasta in pastasComic) {
                        val path = pasta.value.path.substringAfter(txtPastaDestino.text).replace("\\", "/")
                        if (pasta.value.listFiles() != null) {
                            for (image in pasta.value.listFiles()!!)
                                paths.add(path + "/" + image.name)
                        }
                    }

                    try {
                        mLOG.info("Salvando xml do CoMet.")
                        val marshaller = JAXBContext.newInstance(CoMet::class.java).createMarshaller()
                        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                        val out = FileOutputStream(comet)
                        marshaller.marshal(CoMet(mComicInfo, paths), out)
                        out.close()
                        pastasCompactar.add(comet)
                    } catch (e: Exception) {
                        mLOG.error("Erro ao gerar o xml do CoMet.", e)
                    }

                    pastasComic.clear()

                    if (gerarArquivo) {
                        updateMessage("Compactando arquivo: $arquivoZip")
                        destino = File(arquivoZip)

                        if (destino.exists())
                            destino.delete()

                        LAST_PROCESS_FOLDERS = pastasCompactar
                        if (!compactaArquivo(destino, pastasCompactar))
                            Platform.runLater { txtSimularPasta.text = "Erro ao gerar o arquivo, necessário compacta-lo manualmente." }
                    }
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar.", e)
                    Platform.runLater { AlertasPopup.erroModal("Erro ao processar", e.stackTrace.toString()) }
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
                AlertasPopup.erroModal("Erro ao mover os arquivos", super.getMessage())
                habilita()
            }
        }
        pbProgresso.progressProperty().bind(movimentaArquivos.progressProperty())
        lblProgresso.textProperty().bind(movimentaArquivos.messageProperty())
        val t = Thread(movimentaArquivos)
        t.isDaemon = true
        t.start()
    }

    private var mProcess: Process? = null
    private fun compactaArquivo(rar: File, arquivos: File): Boolean {
        var success = true
        val comando = ("rar a -ma4 -ep1 " + '"' + rar.path + '"' + " " + '"' + arquivos.path + '"')
        mLOG.info(comando)
        mProcess = null
        return try {
            val rt = Runtime.getRuntime()
            mProcess = rt.exec(comando)
            Platform.runLater {
                try {
                    mLOG.info("Resultado: " + mProcess!!.waitFor())
                } catch (e: InterruptedException) {
                    mLOG.error("Erro ao executar o comando cmd.", e)
                }
            }
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(mProcess!!.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty())
                mLOG.info("Output comand:\n$resultado")
            s = null
            resultado = ""
            val stdError = BufferedReader(InputStreamReader(mProcess!!.errorStream))
            while (stdError.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty()) {
                success = false
                mLOG.info("Error comand: $resultado Necessário adicionar o rar no path e reiniciar a aplicação.")
            }
            success
        } catch (e: Exception) {
            mLOG.error("Erro ao compactar o arquivo.", e)
            false
        } finally {
            if (mProcess != null)
                mProcess!!.destroy()
        }
    }

    private fun compactaArquivo(rar: File, arquivos: List<File>): Boolean {
        var success = true
        var compactar = ""
        for (arquivo in arquivos)
            compactar += '"'.toString() + arquivo.path + '"' + ' '
        val comando = "rar a -ma4 -ep1 " + '"' + rar.path + '"' + " " + compactar
        mLOG.info(comando)
        return try {
            val rt = Runtime.getRuntime()
            mProcess = rt.exec(comando)
            Platform.runLater {
                try {
                    mLOG.info("Resultado: " + mProcess!!.waitFor())
                } catch (e: InterruptedException) {
                    mLOG.error("Erro ao executar o comando.", e)
                }
            }
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(mProcess!!.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty())
                mLOG.info("Output comand:\n$resultado")
            s = null
            resultado = ""
            val stdError = BufferedReader(InputStreamReader(mProcess!!.errorStream))
            while (stdError.readLine().also { s = it } != null) resultado += "$s"
            if (resultado.isNotEmpty()) {
                success = false
                mLOG.info("Error comand: $resultado Necessário adicionar o rar no path e reiniciar a aplicação. ".trimIndent())
            }
            success
        } catch (e: Exception) {
            mLOG.error("Erro ao compactar o arquivo.", e)
            false
        } finally {
            if (mProcess != null)
                mProcess!!.destroy()
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
            mLOG.error("Erro ao mesclar as imagens.", e)
        } catch (e: Exception) {
            mLOG.error("Erro ao mesclar as imagens.", e)
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
            mLOG.error("Erro ao dividir as imagens.", e)
        } catch (e: Exception) {
            mLOG.error("Erro ao dividir as imagens.", e)
        }
        return false
    }

    private fun limpaMargemImagens(arquivo: File?, clearTopBottom: Boolean): File? {
        if (arquivo == null || !cbAjustarMargemCapa.isSelected)
            return arquivo
        val image: BufferedImage
        try {
            image = ImageIO.read(arquivo)
            val border = image.getRGB(0, 0)
            val branco = java.awt.Color.WHITE.rgb
            val width = image.width
            val height = image.height

            var startX = 0
            var endX = width - 1
            loop@ for (x in 0 until width) {
                for (y in 0 until height) {
                    if (image.getRGB(x, y) != branco) {
                        startX = x
                        break@loop
                    }
                }
            }

            loop@ for (x in endX downTo 0) {
                for (y in 0 until height) {
                    if (image.getRGB(x, y) != branco) {
                        endX = x
                        break@loop
                    }
                }
            }

            mLOG.info("Corte X: $startX - $endX")

            var startY = 0
            var endY = height - 1
            if (clearTopBottom) {
                loop@ for (y in 0 until height) {
                    for (x in 0 until width) {
                        if (image.getRGB(x, y) != branco) {
                            startY = y
                            break@loop
                        }
                    }
                }

                loop@ for (y in endY downTo 0) {
                    for (x in 0 until width) {
                        if (image.getRGB(x, y) != branco) {
                            endY = y
                            break@loop
                        }
                    }
                }

                mLOG.info("Corte Y: $startY - $endY")
            }

            val frente = BufferedImage(endX - startX, endY - startY, BufferedImage.TYPE_INT_ARGB)
            val grFrente = frente.createGraphics()
            val colorFrente = grFrente.color
            grFrente.paint = java.awt.Color.WHITE
            grFrente.fillRect(0, 0, width, height)
            grFrente.color = colorFrente
            grFrente.drawImage(image, null, -startX, -startY)
            grFrente.dispose()
            ImageIO.write(frente, "png", arquivo)
        } catch (e: IOException) {
            mLOG.error("Erro ao limpar as imagens.", e)
        } catch (e: Exception) {
            mLOG.error("Erro ao limpar as imagens.", e)
        }
        return arquivo
    }

    private fun isPaginaDupla(arquivo: File?): Boolean {
        if (arquivo == null || !arquivo.exists()) return false
        val image: BufferedImage
        try {
            image = ImageIO.read(arquivo)
            return image.width / image.height > 0.9
        } catch (e: IOException) {
            mLOG.error("Erro ao verificar imagem.", e)
        }
        return false
    }

    private fun carregaImagem(esquerda: File?, direita: File?): Image? {
        if (direita == null || esquerda == null || !direita.exists() || !esquerda.exists())
            return null
        try {
            limpaMargemImagens(direita, true)
            limpaMargemImagens(esquerda, true)
            val img = File(mPASTA_TEMPORARIA, "tudo.png")
            if (img.exists()) img.delete()
            img.createNewFile()
            if (cbMesclarCapaTudo.isSelected)
                mesclarImagens(img, esquerda, direita)
            else
                copiaItem(esquerda, mPASTA_TEMPORARIA, img.name)
            return Image(img.absolutePath)
        } catch (e: IOException) {
            mLOG.error("Erro ao verificar imagem.", e)
        }
        return null
    }

    private fun carregaImagem(arquivo: File?): Image? {
        if (arquivo == null || !arquivo.exists())
            return null
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
            } else
                mCANCELAR = true
        }
    }

    @FXML
    private fun onBtnCarregarPastaOrigem() {
        mCaminhoOrigem = selecionaPasta(txtPastaOrigem.text)
        if (mCaminhoOrigem != null)
            txtPastaOrigem.text = mCaminhoOrigem!!.absolutePath
        else
            txtPastaOrigem.text = ""
        listaItens()
    }

    private fun carregaPastaOrigem() {
        mCaminhoOrigem = File(txtPastaOrigem.text)
        limparCapas()
        listaItens()
        tbTabRoot.selectionModel.select(0)
    }

    @FXML
    private fun onBtnCarregarPastaDestino() {
        mCaminhoDestino = selecionaPasta(txtPastaDestino.text)
        if (txtPastaDestino != null)
            txtPastaDestino.text = mCaminhoDestino!!.absolutePath
        else
            txtPastaDestino.text = ""
        simulaNome()
    }

    private fun carregaPastaDestino() {
        mCaminhoDestino = File(txtPastaDestino.text)
        simulaNome()
    }

    private fun listaItens() {
        mObsListaItens = if (mCaminhoOrigem != null && !mCaminhoOrigem!!.list().isNullOrEmpty())
            FXCollections.observableArrayList(mCaminhoOrigem!!.list(mFilterNomeArquivo)?.sorted())
        else
            FXCollections.observableArrayList("")
        lsVwListaImagens.items = mObsListaItens
        limparCapas()
        mSelecionado = mObsListaItens[0]
    }

    private fun simulaNome() {
        txtSimularPasta.text = (txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' } + " " + txtNomePastaCapitulo.text.trim { it <= ' ' } + " 00")
        val nome = if (txtNomePastaManga.text.contains("]"))
            txtNomePastaManga.text.substring(txtNomePastaManga.text.indexOf("]") + 1).trim { it <= ' ' }
        else
            txtNomePastaManga.text.trim { it <= ' ' }
        val isJapanese = txtNomePastaManga.text.contains("[JPN]")
        val tudo = mObsListaImagesSelected.stream().filter { it.tipo.compareTo(TipoCapa.CAPA_COMPLETA) == 0 }.findFirst()
        val semCapa = if (tudo.isEmpty) {
            if (isJapanese) " Sem capa" else " (Sem capa)"
        } else ""
        val posFix = if (isJapanese) " (Jap)$semCapa" else semCapa
        txtNomeArquivo.text = nome + " " + txtVolume.text.trim { it <= ' ' } + posFix + ".cbr"
    }

    private fun getNumber(texto: String): Double? {
        val numero = texto.trim { it <= ' ' }.replace(texto.replace(NUMBER_PATTERN.toRegex(), ""), "").trim { it <= ' ' }
        return try {
            java.lang.Double.valueOf(numero)
        } catch (e1: NumberFormatException) {
            null
        }
    }

    private fun incrementaCapitulos(volumeAtual: String, volumeAnterior: String) {
        if (volumeAnterior == volumeAtual || txtGerarFim.text.isNullOrEmpty() || txtGerarInicio.text.isNullOrEmpty())
            return

        val volAtual = getNumber(volumeAtual) ?: return
        val volAnterior = getNumber(volumeAnterior) ?: return

        val capInicio = getNumber(txtGerarInicio.text) ?: return
        val capFim = getNumber(txtGerarFim.text) ?: return

        val diferenca = volAtual - volAnterior
        val capitulos = capFim - capInicio

        val inicio = capInicio + (diferenca * capitulos) + (1 * diferenca)
        if (inicio <= 0)
            return

        txtGerarInicio.text = inicio.toInt().toString()
        txtGerarFim.text = (inicio + capitulos).toInt().toString()
        onBtnGerarCapitulos()
    }

    private fun selecionaPasta(pasta: String): File? {
        val fileChooser = DirectoryChooser()
        fileChooser.title = "Selecione o arquivo."
        if (pasta.isNotEmpty()) {
            val defaultDirectory = File(pasta)
            fileChooser.initialDirectory = defaultDirectory
        }
        return fileChooser.showDialog(null)
    }

    @FXML
    private fun onBtnLimpar() {
        mListaCaminhos = ArrayList()
        mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
        tbViewTabela.items = mObsListaCaminhos
    }

    @FXML
    private fun onBtnSubtrair() {
        if (txtQuantidade.text.isNotEmpty())
            modificaNumeroPaginas(Integer.valueOf(txtQuantidade.text) * -1)
    }

    @FXML
    private fun onBtnSomar() {
        if (txtQuantidade.text.isNotEmpty())
            modificaNumeroPaginas(Integer.valueOf(txtQuantidade.text))
    }

    private fun modificaNumeroPaginas(quantidade: Int) {
        for (caminho in mListaCaminhos) {
            var qtde = caminho.numero + quantidade
            if (qtde < 1) qtde = 1
            caminho.numero = qtde
        }
        mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
        tbViewTabela.items = mObsListaCaminhos
        txtQuantidade.text = ""
    }

    private fun limpaCampo() {
        txtGerarInicio.requestFocus()
    }

    @FXML
    private fun onBtnImporta() {
        if (txtAreaImportar.text.trim { it <= ' ' }.isNotEmpty()) {
            var nomePasta = ""
            var pipe = txtSeparadorPagina.text.trim { it <= ' ' }
            if (pipe.isEmpty())
                pipe = "-"
            txtSeparadorPagina.text = pipe

            var separador = txtSeparadorCapitulo.text.trim { it <= ' ' }
            if (separador.isEmpty())
                separador = "|"
            txtSeparadorCapitulo.text = separador

            val linhas = txtAreaImportar.text.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var linha: Array<String>
            mListaCaminhos = ArrayList()

            val pasta = txtNomePastaCapitulo.text.trim { it <= ' ' }
            for (ls in linhas) {
                val texto = if (ls.contains(separador)) ls.substringAfter(separador) else ls
                linha = texto.split(txtSeparadorPagina.text.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                nomePasta = if (pasta.equals("Capítulo", ignoreCase = true) && linha[0].uppercase(Locale.getDefault()).contains("EXTRA"))
                    linha[0].trim { it <= ' ' }
                else
                    pasta + " " + linha[0].trim { it <= ' ' }

                val tag = if (cbGerarCapitulo.isSelected && ls.contains(separador)) ls.substringBefore(separador) else ""
                mListaCaminhos.add(Caminhos(linha[0], linha[1].trim(), nomePasta, tag))
            }
            mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
            tbViewTabela.items = mObsListaCaminhos
            tbViewTabela.refresh()
        }
    }

    @FXML
    private fun onBtnGerarCapitulos() {
        if (txtGerarInicio.text.trim { it <= ' ' }.isNotEmpty() && txtGerarFim.text.trim { it <= ' ' }.isNotEmpty()) {
            if (mManga == null)
                mManga = geraManga(0)
            val inicio = getNumber(txtGerarInicio.text)?.toInt() ?: return
            val fim = getNumber(txtGerarFim.text)?.toInt() ?: return
            if (inicio <= fim) {
                var texto = ""
                val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3") + "d")
                for (i in inicio..fim)
                    texto += String.format(padding, i) + txtSeparadorPagina.text + if (i < fim) "\r\n" else ""
                txtAreaImportar.text = texto
            } else txtGerarInicio.unFocusColor = Color.GRAY
        } else {
            if (txtGerarInicio.text.trim { it <= ' ' }.isEmpty()) txtGerarInicio.unFocusColor = Color.GRAY
            if (txtGerarFim.text.trim { it <= ' ' }.isEmpty()) txtGerarFim.unFocusColor = Color.GRAY
        }
    }

    @FXML
    private fun onSelectCapaChanged(event: Event) {
        if (tbTabCapa.isSelected) {
            mImagemTudo.image = null
            mImagemFrente.image = null
            mImagemTras.image = null

            if (mObsListaImagesSelected.isNotEmpty()) {
                mObsListaImagesSelected.stream().forEach {
                    if (it.tipo == TipoCapa.CAPA_COMPLETA)
                        CompletableFuture.runAsync {
                            try {
                                if (it.direita != null || mObsListaImagesSelected.none { ls -> ls.tipo == TipoCapa.CAPA_COMPLETA && !ls.arquivo.equals(it.arquivo, ignoreCase = true) }) {
                                    if (it.direita != null)
                                        simularCapa(it.tipo, carregaImagem(File(mPASTA_TEMPORARIA.toString() + "\\" + it.nome), File(mPASTA_TEMPORARIA.toString() + "\\" + it.direita!!.nome)))
                                    else
                                        simularCapa(it.tipo, carregaImagem(File(mPASTA_TEMPORARIA.toString() + "\\" + it.nome)))
                                    if (mObsListaImagesSelected.none { ls -> ls.tipo == TipoCapa.CAPA })
                                        simularCapa(TipoCapa.CAPA, Image(mPASTA_TEMPORARIA.toString() + "\\" + it.arquivo))
                                }
                            } catch (e: IOException) {
                                mLOG.warn("Erro ao processar imagem: Capa ${it.tipo}.", e)
                            }
                        }
                    else
                        CompletableFuture.runAsync {
                            try {
                                simularCapa(it.tipo, carregaImagem(File(mPASTA_TEMPORARIA.toString() + "\\" + it.arquivo)))
                            } catch (e: IOException) {
                                mLOG.warn("Erro ao processar imagem: Capa ${it.tipo}.", e)
                            }
                        }
                }
            }
        }
    }

    private fun extraiInfo(arquivo: File): File? {
        var comicInfo : File? = null
        var proc: Process? = null
        val comando = "rar e -ma4 -y " + '"' + arquivo.path + '"' + " " + '"' + Utils.getCaminho(arquivo.path) + '"' + " " + '"' + COMICINFO + '"'
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"
            if (resultado.isEmpty() && error.isNotEmpty())
                mLOG.info("Error comand: $resultado Não foi possível extrair o arquivo $COMICINFO.")
            else
                comicInfo = File(Utils.getCaminho(arquivo.path) + '\\' + COMICINFO)
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
        return comicInfo
    }

    private fun insereInfo(arquivo: File, info: File) {
        val comando = "rar a -ma4 -ep1 " + '"' + arquivo.path + '"' + " " + '"' + info.path + '"'
        var proc: Process? = null
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String? = null
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            if (resultado.isNotEmpty())
                mLOG.info("Output comand:\n$resultado")
            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"

            if (resultado.isEmpty() && error.isNotEmpty()) {
                info.renameTo(File(arquivo.path + Utils.getNome(arquivo.name) + Utils.getExtenssao(info.name)))
                mLOG.info("Error comand:\n$resultado\nNecessário adicionar o rar no path e reiniciar a aplicação.")
            } else
                info.delete()
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
    }

    private fun carregarItensOcr() {
        val pasta = File(txtPastaOcr.text)
        if (txtPastaOcr.text.isNotEmpty() && pasta.exists()) {
            btnOcrProcessar.isDisable = true

            val ocr: Task<Void> = object : Task<Void>() {
                override fun call(): Void? {
                    try {
                        val lista = mutableListOf<Processar>()

                        val jaxb = JAXBContext.newInstance(ComicInfo::class.java)
                        for (arquivo in pasta.listFiles()!!) {
                            if (!Utils.isRar(arquivo.name))
                                continue

                            val info: File = extraiInfo(arquivo) ?: continue
                            val comic: ComicInfo = try {
                                val unmarshaller = jaxb.createUnmarshaller()
                                unmarshaller.unmarshal(info) as ComicInfo
                            } catch (e: Exception) {
                                mLOG.error(e.message, e)
                                continue
                            }

                            val bookMarks = comic.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + SEPARADOR_IMAGEM + it.bookmark }?.toSet() ?: emptySet()
                            val processar = JFXButton("Processar")
                            val salvar = JFXButton("Salvar")
                            val tags = bookMarks.joinToString(separator = "\n")
                            val item = Processar(arquivo.name, tags, arquivo, comic, processar, salvar)

                            processar.styleClass.add("background-White1")
                            processar.setOnAction { ocrItem(item) }
                            salvar.styleClass.add("background-White1")
                            salvar.setOnAction { salvarItem(item) }

                            lista.add(item)
                        }

                        mObsListaOCR = FXCollections.observableArrayList(lista)
                        Platform.runLater { tbViewOcr.items = mObsListaOCR }
                    } catch (e: Exception) {
                        mLOG.info("Erro ao realizar a gerar itens para processamento de OCR.", e)
                        Platform.runLater {
                            Notificacoes.notificacao(Notificacao.ERRO, "OCR Capítulos", "Erro ao realizar a gerar itens para processamento de OCR. " + e.message)
                        }
                    }
                    return null
                }
                override fun succeeded() {
                    Platform.runLater {
                        btnOcrProcessar.isDisable = false
                    }
                }
            }

            Thread(ocr).start()
        } else {
            AlertasPopup.alertaModal("Alerta", "Necessário informar uma pasta para processar.")
            txtPastaOcr.requestFocus()
        }
    }


    private fun salvarItem(item: Processar) {
        try {
            val info = File(item.file!!.parent, "ComicInfo.xml")
            if (info.exists())
                info.delete()

            val tags = item.tags.split("\n")
            for (tag in tags) {
                val imagem = tag.substringBefore(SEPARADOR_IMAGEM)
                var capitulo = tag.substringAfter(SEPARADOR_IMAGEM).trim()
                if (capitulo.isEmpty())
                    continue

                if (capitulo.endsWith("-"))
                    capitulo = capitulo.substringBeforeLast("-").trim()

                val page = item.comicInfo?.pages?.firstOrNull { it.image.toString() == imagem } ?: continue
                page.bookmark = capitulo
            }

            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            val out = FileOutputStream(info)
            marshaller.marshal(item.comicInfo, out)
            out.close()
            insereInfo(item.file!!, info)
            mObsListaOCR.remove(item)
        } catch (e: Exception) {
            mLOG.error(e.message, e)
            AlertasPopup.alertaModal("Erro", "Erro ao salvar o arquivo ComicInfo.xml. " + e.message)
        }
    }

    private fun extraiSumario(arquivo: File): File? {
        var sumario : File? = null
        var proc: Process? = null

        for (arquivos in mPASTA_TEMPORARIA.listFiles()!!) {
            if (arquivos.name.contains("zSumário", ignoreCase = true))
                arquivos.delete()
        }

        val comando = "rar e -ma4 -y " + '"' + arquivo.path + '"' + " " + '"' + mPASTA_TEMPORARIA.path + '"' + " " + '"' + "*zSumário.*" + '"'
        try {
            val rt: Runtime = Runtime.getRuntime()
            proc = rt.exec(comando)
            var resultado = ""
            val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
            var s: String?
            while (stdInput.readLine().also { s = it } != null)
                resultado += "$s"

            s = null
            var error = ""
            val stdError = BufferedReader(InputStreamReader(proc.errorStream))
            while (stdError.readLine().also { s = it } != null)
                error += "$s"
            if (resultado.isEmpty() && error.isNotEmpty())
                mLOG.info("Error comand: $resultado Não foi possível extrair o sumário.")
            else {
                for (arquivos in mPASTA_TEMPORARIA.listFiles()!!) {
                    if (arquivos.name.contains("zSumário", ignoreCase = true)) {
                        sumario = arquivos
                        break
                    }
                }
            }
        } catch (e: Exception) {
            mLOG.error(e.message, e)
        } finally {
            proc?.destroy()
        }
        return sumario
    }

    private fun ocrItem(item: Processar) {
        val sumario = extraiSumario(item.file!!) ?: return
        val capitulos = Ocr.processaGemini(sumario, txtSeparadorCapitulo.text).split("\n")
        val newTag = mutableSetOf<String>()
        val tags = item.comicInfo!!.pages?.filter { !it.bookmark.isNullOrEmpty() }?.map { it.image.toString() + SEPARADOR_IMAGEM + it.bookmark }?.toList() ?: emptyList()

        var index = -1
        for (tag in tags) {
            if (tag.contains("capítulo", ignoreCase = true) || tag.contains("第", ignoreCase = true)) {
                index++
                val capitulo = if (index < capitulos.size) capitulos[index] else ""
                newTag.add("$tag - $capitulo")
            } else
                newTag.add(tag)
        }

        if (index < capitulos.size) {
            for (i in index + 1 until capitulos.size)
                newTag.add("0$SEPARADOR_IMAGEM${capitulos[i]}")
        }

        item.tags = if (newTag.isNotEmpty()) newTag.joinToString(separator = "\n") else item.tags
        tbViewOcr.refresh()
    }

    fun setLog(texto: String, isError : Boolean = false) {
        if (isError) {
            lblAlerta.text = texto
            lblAviso.text = ""
        } else {
            lblAlerta.text = ""
            lblAviso.text = texto
        }
    }

    private fun compartilhamento() {
        val compartilha: Task<Boolean> = object : Task<Boolean>() {
            override fun call(): Boolean {
                sincronizacao.consultar()
                return sincronizacao.sincroniza()
            }

            override fun succeeded() {
                if (!value)
                    setLog("Não foi possível sincronizar os dados com a cloud", true)
                else
                    setLog("Sincronização de dados com a cloud concluída com sucesso.")
            }
        }

        val t = Thread(compartilha)
        t.start()
    }

    fun animacaoSincronizacao(isProcessando: Boolean, isErro: Boolean) {
        Platform.runLater {
            if (isProcessando)
                animacao.tmSincronizacao.play()
            else {
                animacao.tmSincronizacao.stop()
                if (isErro)
                    imgCompartilhamento.image = imgAnimaCompartilhaErro
                else
                    imgCompartilhamento.image = imgAnimaCompartilhaEnvio
            }
        }
    }

    private val mMostraFinalTexto: MutableSet<TextField> = HashSet()
    private fun textFieldMostraFinalTexto(txt: JFXTextField) {
        mMostraFinalTexto.add(txt)
        val onFocus: MutableSet<TextField> = HashSet()
        val overrideNextCaratChange: MutableSet<TextField> = HashSet()
        val onLoseFocus =
            ChangeListener<Boolean> { observable: ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean ->
                val property = observable as ReadOnlyProperty<out Boolean>
                val tf = property.bean as TextField
                if (oldValue && onFocus.contains(tf))
                    onFocus.remove(tf)
                if (newValue)
                    onFocus.add(tf)
                if (!newValue)
                    overrideNextCaratChange.add(tf)
            }
        val onCaratChange =
            ChangeListener { observable: ObservableValue<out Number?>, _: Number?, _: Number? ->
                val property = observable as ReadOnlyProperty<out Number?>
                val tf = property.bean as TextField
                if (overrideNextCaratChange.contains(tf)) {
                    tf.end()
                    overrideNextCaratChange.remove(tf)
                } else if (!onFocus.contains(tf) && mMostraFinalTexto.contains(tf))
                    tf.end()
            }
        txt.focusedProperty().addListener(onLoseFocus)
        txt.caretPositionProperty().addListener(onCaratChange)
    }

    private fun contemTipoSelecionado(tipo: TipoCapa, caminho: String?): Boolean {
        return if (mObsListaImagesSelected.isEmpty()) false else mObsListaImagesSelected.stream()
            .anyMatch { capa: Capa -> capa.tipo == tipo && capa.nome.equals(caminho, ignoreCase = true) }
    }

    private var mDellaySubir: Timer? = null
    private var mDellayDescer: Timer? = null
    private fun selecionaImagens() {
        mObsListaImagesSelected = FXCollections.observableArrayList()
        lsVwListaImagens.addEventFilter(ScrollEvent.ANY) { e: ScrollEvent ->
            if (e.deltaY > 0) {
                if (e.deltaY > 10) {
                    btnScrollSubir.isVisible = true
                    btnScrollSubir.isDisable = false
                    if (mDellaySubir != null)
                        mDellaySubir!!.cancel()
                    mDellaySubir = Timer()
                    mDellaySubir!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollSubir.isVisible = false
                            btnScrollSubir.isDisable = true
                            mDellaySubir = null
                        }
                    }, 3000)
                }
            } else {
                if (e.deltaY < 10) {
                    btnScrollDescer.isVisible = true
                    btnScrollDescer.isDisable = false
                    if (mDellayDescer != null)
                        mDellayDescer!!.cancel()
                    mDellayDescer = Timer()
                    mDellayDescer!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollDescer.isVisible = false
                            btnScrollDescer.isDisable = true
                            mDellayDescer = null
                        }
                    }, 3000)
                }
            }
        }
        lsVwListaImagens.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1) {
                if (click.isControlDown)
                    limparCapas()
                else {
                    val item = lsVwListaImagens.selectionModel.selectedItem
                    if (item != null) {
                        if (mObsListaImagesSelected.stream().anyMatch { e: Capa -> e.nome.equals(item, ignoreCase = true) })
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
        lsVwListaImagens.setCellFactory {
            val cell: JFXListCell<String> = object : JFXListCell<String>() {
                override fun updateItem(images: String?, empty: Boolean) {
                    super.updateItem(images, empty)
                    text = images
                }
            }
            val listenerCapa = InvalidationListener {
                cell.pseudoClassStateChanged(
                    capaSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.CAPA, cell.item)
                )
            }
            val listenerCapaCompleta = InvalidationListener {
                cell.pseudoClassStateChanged(
                    capaCompletaSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.CAPA_COMPLETA, cell.item)
                )
                simulaNome()
            }
            val listenerSumario = InvalidationListener {
                cell.pseudoClassStateChanged(
                    sumarioSelected,
                    cell.item != null && contemTipoSelecionado(TipoCapa.SUMARIO, cell.item)
                )
            }
            cell.itemProperty().addListener(listenerCapa)
            cell.itemProperty().addListener(listenerCapaCompleta)
            cell.itemProperty().addListener(listenerSumario)
            mObsListaImagesSelected.addListener(listenerCapa)
            mObsListaImagesSelected.addListener(listenerCapaCompleta)
            mObsListaImagesSelected.addListener(listenerSumario)
            cell
        }
    }

    private fun editaColunas() {
        clCapitulo.cellFactory = TextFieldTableCell.forTableColumn()
        clCapitulo.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].capitulo = e.newValue
            e.tableView.items[e.tablePosition.row].nomePasta = txtNomePastaCapitulo.text.trim { it <= ' ' } + " " + e.newValue
        }
        clNumeroPagina.cellFactory = TextFieldTableCell.forTableColumn()
        clNumeroPagina.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].addNumero(e.newValue)
        }
        clNomePasta.cellFactory = TextFieldTableCell.forTableColumn()
        clNomePasta.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].nomePasta = e.newValue
        }

        clTag.cellFactory = TextFieldTableCell.forTableColumn()
        clTag.setOnEditCommit { e: TableColumn.CellEditEvent<Caminhos, String> ->
            e.tableView.items[e.tablePosition.row].tag = e.newValue
        }

        clOCRTags.cellFactory = TextAreaTableCell.forTableColumn()
        clOCRTags.setOnEditCommit { e: TableColumn.CellEditEvent<Processar, String> ->
            e.tableView.items[e.tablePosition.row].tags = e.newValue
        }
    }

    private fun linkaCelulas() {
        clCapitulo.cellValueFactory = PropertyValueFactory("capitulo")
        clNumeroPagina.cellValueFactory = PropertyValueFactory("numeroPagina")
        clNomePasta.cellValueFactory = PropertyValueFactory("nomePasta")
        clTag.cellValueFactory = PropertyValueFactory("tag")

        clMalId.cellValueFactory = PropertyValueFactory("idVisual")
        clMalNome.cellValueFactory = PropertyValueFactory("nome")
        clMalSite.cellValueFactory = PropertyValueFactory("site")
        clMalImagem.cellValueFactory = PropertyValueFactory("imagem")

        clOCRArquivo.cellValueFactory = PropertyValueFactory("arquivo")
        clOCRSerie.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.series)
            else
                SimpleStringProperty("")
        }

        clOCRTitulo.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.title)
            else
                SimpleStringProperty("")
        }

        clOCREditora.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null)
                SimpleStringProperty(item.comicInfo!!.publisher)
            else
                SimpleStringProperty("")
        }

        clOCRPublicacao.setCellValueFactory { param ->
            val item = param.value
            if (item.comicInfo != null && item.comicInfo!!.year != null)
                SimpleStringProperty("${item.comicInfo!!.day}/${item.comicInfo!!.month}/${item.comicInfo!!.year}")
            else
                SimpleStringProperty("")
        }

        clOCRTags.cellValueFactory = PropertyValueFactory("tags")
        clProcessarOCR.cellValueFactory = PropertyValueFactory("processar")
        clSalvarOCR.cellValueFactory = PropertyValueFactory("salvar")

        editaColunas()
        selecionaImagens()

        tbViewMal.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1 && tbViewMal.items.isNotEmpty())
                carregaMal(tbViewMal.selectionModel.selectedItem)
        }
    }

    private fun addCapitulo(capitulo: String) : String {
        if (capitulo.isEmpty())
            return ""
        val numero = getNumber(capitulo) ?: return ""
        return numero.plus(1).toInt().toString()
    }

    private fun minCapitulo(capitulo: String) : String {
        if (capitulo.isEmpty())
            return ""
        var numero = getNumber(capitulo) ?: return ""
        numero = if (numero <= 1)
            1.0
        else
            numero.minus(1)
        return numero.toInt().toString()
    }

    private var mPastaAnterior = ""
    private var mNomePastaAnterior = ""
    private fun configuraTextEdit() {
        txtSeparadorPagina.isDisable = true
        txtSeparadorCapitulo.isDisable = true
        textFieldMostraFinalTexto(txtSimularPasta)
        textFieldMostraFinalTexto(txtPastaOrigem)

        txtPastaOrigem.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean ->
            if (newPropertyValue)
                mPastaAnterior = txtPastaOrigem.text

            if (oldPropertyValue && txtPastaOrigem.text.compareTo(mPastaAnterior, ignoreCase = true) != 0)
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

        txtPastaDestino.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, _: Boolean? ->
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

        txtNomePastaManga.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean ->
                if (oldPropertyValue) {
                    txtNomePastaManga.text?.let {
                        if (it.isNotEmpty() && it.contains("  "))
                            txtNomePastaManga.text = it.replace("  ", " ")
                    }

                    simulaNome()
                    if (mNomePastaAnterior != txtNomePastaManga.text)
                        carregaComicInfo()
                }

                if (newPropertyValue)
                    mNomePastaAnterior = txtNomePastaManga.text
        }
        txtNomePastaManga.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }

        txtNomeArquivo.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            txtPastaDestino.unFocusColor = Color.GRAY
        }
        txtNomeArquivo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtNomeArquivo.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, _: Boolean? ->
            if (oldPropertyValue && mManga == null)
                mManga = geraManga(0)
        }

        var volumeAnterior = ""
        txtVolume.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, newPropertyValue: Boolean ->
            if (oldPropertyValue) {
                simulaNome()
                if (!carregaManga() && !carregaMangaAnterior())
                    incrementaCapitulos(txtVolume.text, volumeAnterior)
            }

            if (newPropertyValue)
                volumeAnterior = txtVolume.text
        }

        txtVolume.onKeyPressed = EventHandler { e: KeyEvent ->
            when(e.code) {
                KeyCode.ENTER -> txtGerarInicio.requestFocus()
                KeyCode.TAB -> {
                    if (!e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                        txtGerarInicio.requestFocus()
                        e.consume()
                    }
                }
                KeyCode.UP -> onBtnVolumeMais()
                KeyCode.DOWN -> onBtnVolumeMenos()
                else -> { }
            }

            if (e.code == KeyCode.UP || e.code == KeyCode.DOWN) {
                txtVolume.positionCaret(txtVolume.text.length)
                e.consume()
            }
        }

        txtNomePastaCapitulo.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, _: Boolean? -> if (oldPropertyValue) simulaNome() }
        txtNomePastaCapitulo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code.toString() == "ENTER") clickTab() }

        txtGerarInicio.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            txtPastaDestino.unFocusColor = Color.GRAY

            if (!txtGerarInicio.text.isNullOrEmpty() && !txtGerarFim.text.isNullOrEmpty()) {
                val inicio = getNumber(txtGerarInicio.text)?.toInt() ?: return@addListener
                val fim = getNumber(txtGerarFim.text)?.toInt() ?: return@addListener
                if (inicio > fim)
                    txtGerarFim.text = inicio.plus(1).toString()
            }
        }
        txtGerarInicio.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(NUMBER_REGEX))
                txtGerarInicio.text = oldValue
        }
        txtGerarInicio.onKeyPressed = EventHandler { e: KeyEvent ->
            when(e.code) {
                KeyCode.ENTER -> {
                    clickTab()
                    e.consume()
                }
                KeyCode.UP -> {
                    txtGerarInicio.text = addCapitulo(txtGerarInicio.text)
                    txtGerarInicio.positionCaret(txtGerarInicio.text.length)
                    e.consume()
                }
                KeyCode.DOWN -> {
                    txtGerarInicio.text = minCapitulo(txtGerarInicio.text)
                    txtGerarInicio.positionCaret(txtGerarInicio.text.length)
                    e.consume()
                }
                else -> { }
            }

        }

        txtGerarFim.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            txtPastaDestino.unFocusColor = Color.GRAY
        }
        txtGerarFim.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(NUMBER_REGEX))
                txtGerarFim.text = oldValue
        }
        txtGerarFim.onKeyPressed = EventHandler { e: KeyEvent ->
            when(e.code) {
                KeyCode.ENTER -> {
                    onBtnGerarCapitulos()
                    txtAreaImportar.requestFocus()
                    val position = txtAreaImportar.text.indexOf(txtSeparadorPagina.text) + 1
                    txtAreaImportar.positionCaret(position)
                    e.consume()
                }
                KeyCode.TAB  -> {
                    if (!e.isShiftDown) {
                        txtAreaImportar.requestFocus()
                        e.consume()
                    }
                }
                KeyCode.UP -> {
                    txtGerarFim.text = addCapitulo(txtGerarFim.text)
                    txtGerarFim.positionCaret(txtGerarFim.text.length)
                    e.consume()
                }
                KeyCode.DOWN -> {
                    txtGerarFim.text = minCapitulo(txtGerarFim.text)
                    txtGerarFim.positionCaret(txtGerarFim.text.length)
                    e.consume()
                }
                else -> { }
            }
        }

        var lastCaretPos = 0
        txtAreaImportar.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.isControlDown && !e.isShiftDown) {
                when (e.code) {
                    KeyCode.ENTER -> onBtnImporta()
                    KeyCode.D,
                    KeyCode.E,
                    in (KeyCode.NUMPAD0 .. KeyCode.NUMPAD9),
                    in (KeyCode.DIGIT0 .. KeyCode.DIGIT9) -> {
                        if (txtAreaImportar.text.isEmpty())
                            return@EventHandler

                        val txt = txtAreaImportar.text
                        val scroll = txtAreaImportar.scrollTopProperty().value

                        var before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        before = before.substringBeforeLast(line)

                        val pipe = txtSeparadorPagina.text
                        val separador = txtSeparadorCapitulo.text
                        val tag = if (line.contains(separador)) line.substringBefore(separador) + separador else ""
                        val texto = if (line.contains(separador)) line.substringAfter(separador) + separador else line
                        val page = if (line.contains(pipe)) line.substringAfter(pipe) else ""

                        val newLine = when (e.code) {
                            KeyCode.E -> {
                                if (texto.contains("extra", true)) {
                                    val fim = getNumber(txtGerarFim.text)?.toInt() ?: 0
                                    val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3") + "d")
                                    var sequence = txt.split("\n").last { !it.contains("extra", ignoreCase = true) }
                                    sequence = if (sequence.contains(pipe)) sequence.substringBefore(pipe) else sequence
                                    tag + (getNumber(sequence)?.toInt()?.let { "${String.format(padding, it+1)}$pipe$page" } ?: sequence)
                                } else {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    "${tag}Extra ${String.format("%02d", count + 1)}$pipe$page"
                                }
                            }
                            KeyCode.D ->  {
                                if (line.contains("extra", true) && last.isEmpty()) {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    line + "\n" + "${tag}Extra ${String.format("%02d", count + 1)}$pipe$page"
                                } else
                                    line + "\n" + line
                            }
                            in (KeyCode.NUMPAD0 .. KeyCode.NUMPAD9),
                            in (KeyCode.DIGIT0 .. KeyCode.DIGIT9) -> {
                                if (texto.contains("extra", true)) {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    val number = if (e.code == KeyCode.DIGIT0 || e.code == KeyCode.NUMPAD0) count else e.text.toInt()
                                    "${tag}Extra ${String.format("%02d", number)}$pipe$page"
                                } else {
                                    val chapter = if (texto.contains("."))
                                        texto.substringBefore(".")
                                    else if (texto.contains(pipe))
                                        texto.substringBefore(pipe)
                                    else
                                        texto
                                    val number = if (e.code == KeyCode.DIGIT0 || e.code == KeyCode.NUMPAD0) "" else "." + e.text
                                    "$tag$chapter$number$pipe$page"
                                }
                            }
                            else -> line
                        }

                        val newText = before + newLine + last

                        txtAreaImportar.text = newText
                        lastCaretPos = before.length + newLine.length
                        txtAreaImportar.positionCaret(lastCaretPos)
                        txtAreaImportar.scrollTop = scroll
                    }
                    else -> {}
                }
            } else if (e.isControlDown && e.isShiftDown) {
                when (e.code) {
                    KeyCode.UP,
                    KeyCode.DOWN -> {
                        if (txtAreaImportar.text.isEmpty() || !txtAreaImportar.text.contains("\n"))
                            return@EventHandler

                        val txt = txtAreaImportar.text ?: ""
                        val lines = txt.split("\n")
                        val scroll = txtAreaImportar.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        var newText = ""

                        if (e.code == KeyCode.UP) {
                            var replaced = false
                            lines.forEachIndexed { index, ln ->
                                if (!replaced) {
                                    val next = index + 1
                                    if (next < lines.size)
                                        if (lines[next] == line) {
                                            replaced = true
                                            newText += line + "\n"
                                        }
                                }

                                if (ln != line)
                                    newText += ln + "\n"
                            }
                            if (!replaced)
                                newText = line + "\n" + newText
                        } else if (e.code == KeyCode.DOWN) {
                            var replaced = false
                            var nexIndex = -1
                            lines.forEachIndexed { index, ln ->
                                if (index == nexIndex)
                                    return@forEachIndexed

                                if (ln != line)
                                    newText += ln + "\n"
                                else {
                                    val next = index + 1
                                    if (next < lines.size) {
                                        replaced = true
                                        newText += lines[next] + "\n"
                                        newText += line + "\n"
                                        nexIndex = next
                                    }
                                }
                            }

                            if (!replaced)
                                newText += line + "\n"
                        }

                        txtAreaImportar.text = newText.substringBeforeLast("\n")
                        txtAreaImportar.positionCaret(txtAreaImportar.text.indexOf(line))
                        txtAreaImportar.scrollTop = scroll
                    }
                    else -> {}
                }
            }
            lastCaretPos = txtAreaImportar.caretPosition
        }

        txtQuantidade.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> txtPastaDestino.unFocusColor = Color.GRAY }
        txtQuantidade.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(NUMBER_REGEX))
                txtGerarFim.text = oldValue
        }

        cbMesclarCapaTudo.selectedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> reloadCapa() }
        cbAjustarMargemCapa.selectedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> reloadCapa() }

        cbAgeRating.items.addAll(AgeRating.values())
        cbLanguage.items.addAll(Linguagem.JAPANESE, Linguagem.ENGLISH, Linguagem.PORTUGUESE)
        cbLanguage.selectionModel.select(Linguagem.JAPANESE)

        cbLanguage.focusedProperty().addListener { _, _, newPropertyValue ->
            if (newPropertyValue)
                cbLanguage.setUnFocusColor(Color.web("#4059a9"))
            else {
                if (cbLanguage.value == null)
                    cbLanguage.setUnFocusColor(Color.RED)
                else
                    cbLanguage.setUnFocusColor(Color.web("#4059a9"))
            }
        }

        txtIdMal.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        cbAgeRating.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        cbLanguage.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtTitle.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtAlternateSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtSeriesGroup.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtPublisher.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtStoryArc.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtImprint.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }
        txtGenre.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) clickTab() }

        txtMalId.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }
        txtMalNome.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }

        txtIdMal.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(NUMBER_REGEX))
                txtIdMal.text = oldValue
        }

        txtMalId.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isNotEmpty() && !newValue.matches(NUMBER_REGEX))
                txtMalId.text = oldValue
        }
    }

    fun configurarAtalhos(scene: Scene) {
        val kcInicioFocus: KeyCombination = KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN)
        val kcFimFocus: KeyCombination = KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
        val kcImportFocus: KeyCombination = KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN)
        val kcImportar: KeyCombination = KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)
        val kcComicInfo: KeyCombination = KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN)
        val kcArquivos: KeyCombination = KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN)

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
                mSelecionado = lsVwListaImagens.selectionModel.selectedItem

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

            if (kcComicInfo.match(ke)) {
                if (tbTabRoot.selectionModel.selectedItem == tbTabComicInfo) {
                    if (txtMalId.text.isEmpty() && txtMalNome.text.isEmpty()) {
                        var nome = txtNomePastaManga.text
                        if (nome.contains("]"))
                            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }

                        if (nome.substring(nome.length - 1).equals("-", ignoreCase = true))
                            nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }

                        txtMalNome.text = nome
                    }
                    btnMalConsultar.fire()
                } else
                    tbTabRoot.selectionModel.select(tbTabComicInfo)
            }

            if (kcArquivos.match(ke)) {
                if (tbTabRoot.selectionModel.selectedItem != tbTabArquivo)
                    tbTabRoot.selectionModel.select(tbTabArquivo)
            }
        }
    }

    private fun configuraZoom(root: AnchorPane, imageView: ImageView, slider: JFXSlider): GesturePane {
        val pane = GesturePane(imageView)
        root.children.add(0, pane)
        AnchorPane.setTopAnchor(pane, 0.0)
        AnchorPane.setLeftAnchor(pane, 0.0)
        AnchorPane.setRightAnchor(pane, 0.0)
        AnchorPane.setBottomAnchor(pane, 0.0)

        pane.minScale = -0.1
        var zoomUpdate = false
        slider.valueProperty().addListener { _: Observable? ->
            if (zoomUpdate)
                return@addListener
            try {
                zoomUpdate = true
                if (slider.value <= 0)
                    pane.zoomTo(0.0, pane.targetPointAtViewportCentre())
                else
                    pane.zoomTo(slider.value, pane.targetPointAtViewportCentre())
            } finally {
                zoomUpdate = false
            }
        }

        pane.currentScaleProperty().addListener { _, _, value ->
            if (zoomUpdate)
                return@addListener
            try {
                zoomUpdate = true
                slider.value = value.toDouble()
            } finally {
                zoomUpdate = false
            }
        }

        pane.setOnMouseClicked { e ->
            if (e.clickCount >= 2) {
                val pivotOnTarget: Point2D = pane.targetPointAt(Point2D(e.x, e.y)).orElse(pane.targetPointAtViewportCentre())
                if (e.button === MouseButton.PRIMARY) {
                    pane.animate(Duration.millis(200.0))
                        .interpolateWith(Interpolator.EASE_BOTH)
                        .zoomBy(pane.currentScale, pivotOnTarget)
                } else if (e.button === MouseButton.SECONDARY) {
                    pane.animate(Duration.millis(200.0))
                        .interpolateWith(Interpolator.EASE_BOTH)
                        .zoomTo(pane.currentScale * 0.5, pivotOnTarget)
                }
            }
        }

        return pane
    }

    private fun configuraImagens() {
        mImagemFrente = ImageView()
        mGestureFrente = configuraZoom(rootFrente, mImagemFrente, sliderFrente)

        mImagemTras = ImageView()
        mGestureTras = configuraZoom(rootTras, mImagemTras, sliderTras)

        mImagemTudo = ImageView()
        mGestureTudo = configuraZoom(rootTudo, mImagemTudo, sliderTudo)
    }

    private fun clickTab() {
        val robot = Robot()
        robot.keyPress(KeyCode.TAB)
    }

    @Synchronized
    override fun initialize(arg0: URL, arg1: ResourceBundle?) {
        configuraImagens()
        linkaCelulas()
        limpaCampos()
        configuraTextEdit()

        /* Setando as variáveis para o alerta padrão. */
        AlertasPopup.rootStackPane = rootStackPane
        AlertasPopup.nodeBlur = root
        Notificacoes.rootAnchorPane = apGlobal

        animacao.animaSincronizacao(imgCompartilhamento, imgAnimaCompartilha, imgAnimaCompartilhaEspera)

        sincronizacao.setObserver { observable: ListChangeListener.Change<out Pair<Tipo, Int>> ->
            if (!sincronizacao.isSincronizando()) {
                var sinc = ""
                for (item in observable.list)
                    sinc += observable.list.size.toString() + " ${if(item.first == Tipo.MANGA) "(Manga)" else "(ComicInfo)"} "

                Platform.runLater {
                    if (sinc.isNotEmpty()) {
                        lblAviso.text = "Pendente registro(s) para envio: ${sinc.trim()}."
                        imgCompartilhamento.image = imgAnimaCompartilhaEspera
                    } else
                        imgCompartilhamento.image = imgAnimaCompartilha
                }
            }
        }

        if (!sincronizacao.isConfigurado())
            imgCompartilhamento.image = imgAnimaCompartilhaErro
        else if (sincronizacao.listSize() > 0) {
            lblAviso.text = "Pendente de envio " + sincronizacao.listSize() + " registro(s)."
            imgCompartilhamento.image = imgAnimaCompartilhaEspera
        } else
            imgCompartilhamento.image = imgAnimaCompartilha

        mSugestao.cellLimit = 1
    }

    companion object {
        private val COMICINFO = "ComicInfo.xml"
        private val SEPARADOR_IMAGEM = ";"

        private const val IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$"
        private var LAST_PROCESS_FOLDERS: MutableList<File> = ArrayList()
        private const val NUMBER_PATTERN = "[\\d.]+"
        private val NUMBER_REGEX = Regex("\\d*")
        private val ONLY_NUMBER_REGEX = Regex("^\\d+")

        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/TelaInicial.fxml")
        val iconLocate: String get() = "/images/icoProcessar_512.png"

        val imgAnimaCompartilha = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamento_48.png"))
        val imgAnimaCompartilhaEspera = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEspera_48.png"))
        val imgAnimaCompartilhaErro = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoErro_48.png"))
        val imgAnimaCompartilhaEnvio = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEnvio_48.png"))
    }
}
