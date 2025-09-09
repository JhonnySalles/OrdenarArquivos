package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.animation.Animacao
import com.fenix.ordenararquivos.exceptions.LibException
import com.fenix.ordenararquivos.model.*
import com.fenix.ordenararquivos.model.entities.Caminhos
import com.fenix.ordenararquivos.model.entities.Capa
import com.fenix.ordenararquivos.model.entities.Historico
import com.fenix.ordenararquivos.model.entities.Manga
import com.fenix.ordenararquivos.model.entities.comet.CoMet
import com.fenix.ordenararquivos.model.entities.comicinfo.*
import com.fenix.ordenararquivos.model.enums.*
import com.fenix.ordenararquivos.notification.AlertasPopup
import com.fenix.ordenararquivos.notification.Notificacoes
import com.fenix.ordenararquivos.process.Compactar
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
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import net.kurobako.gesturefx.GesturePane
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.*
import java.math.RoundingMode
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.SQLException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.properties.Delegates


class AbaArquivoController : Initializable {

    private val mLOG = LoggerFactory.getLogger(AbaArquivoController::class.java)

    //<--------------------------  PRINCIPAL   -------------------------->

    @FXML
    private lateinit var apRoot: AnchorPane

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
    private lateinit var cbOcrSumario: JFXCheckBox

    @FXML
    private lateinit var cbGerarCapitulo: JFXCheckBox

    @FXML
    private lateinit var lsVwImagens: JFXListView<String>

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
    private lateinit var btnScrollSubir: JFXButton

    @FXML
    private lateinit var btnScrollDescer: JFXButton

    @FXML
    private lateinit var acdArquivos: Accordion

    @FXML
    private lateinit var ttpArquivos: TitledPane

    @FXML
    private lateinit var ttpHistorico: TitledPane

    @FXML
    private lateinit var lsVwHistorico: JFXListView<Historico>

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
    private lateinit var cbLinguagem: JFXComboBox<Linguagem>

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
    private lateinit var btnAmazonConsultar: JFXButton

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

    private lateinit var controller: TelaInicialController
    var controllerPai: TelaInicialController
        get() = controller
        set(controller) {
            this.controller = controller
        }

    private val mSugestao: JFXAutoCompletePopup<String> = JFXAutoCompletePopup<String>()
    private var mAutoComplete: JFXAutoCompletePopup<String> = JFXAutoCompletePopup<String>()

    private var mListaCaminhos: MutableList<Caminhos> = arrayListOf()
    private var mObsListaCaminhos: ObservableList<Caminhos> = FXCollections.observableArrayList(mListaCaminhos)
    private var mObsListaItens: ObservableList<String> = FXCollections.observableArrayList("")
    private var mObsListaImagesSelected: ObservableList<Capa> = FXCollections.observableArrayList()
    private var mObsListaMal: ObservableList<Mal> = FXCollections.observableArrayList()

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

    private val mAnimacao = Animacao()
    private val mSincronizacao = SincronizacaoServices(this)

    @FXML
    private fun onBtnCompartilhamento() = compartilhamento()

    fun limpaCampos() {
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
        txtSeparadorPagina.text = Utils.SEPARADOR_PAGINA
        txtSeparadorCapitulo.text = Utils.SEPARADOR_CAPITULO
        onBtnLimpar()
        mObsListaItens = FXCollections.observableArrayList("")
        lsVwImagens.items = mObsListaItens
        mSelecionado = null

        mObsListaMal = FXCollections.observableArrayList()
        tbViewMal.items = mObsListaMal
        mComicInfo = ComicInfo()
        txtMalId.text = ""
        txtMalNome.text = ""

        controllerPai.clearProgress()
    }

    private val mFilterNomeArquivo: FilenameFilter
        get() = FilenameFilter { _: File?, name: String ->
            if (name.lastIndexOf('.') > 0) {
                val p = Pattern.compile(Utils.IMAGE_PATTERN)
                return@FilenameFilter p.matcher(name).matches()
            }
            false
        }

    @FXML
    private fun onBtnScrollSubir() {
        if (!lsVwImagens.items.isNullOrEmpty())
            lsVwImagens.scrollTo(0)
    }

    @FXML
    private fun onBtnScrollBaixo() {
        if (!lsVwImagens.items.isNullOrEmpty())
            lsVwImagens.scrollTo(lsVwImagens.items.size - 1)
    }

    @FXML
    private fun onBtnLimparTudo() {
        limpaCampos()
    }

    @FXML
    private fun onBtnCompactar() {
        if (mCaminhoDestino!!.exists() && txtNomeArquivo.text.isNotEmpty() && LAST_PROCESS_FOLDERS.isNotEmpty())
            compactaArquivo(File(mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }), LAST_PROCESS_FOLDERS)
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
        if (txtVolume.text.matches(Regex(".*${Utils.NUMBER_PATTERN}$"))) {
            val oldVolume = txtVolume.text
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(Utils.NUMBER_PATTERN.toRegex(), ""), "").trim { it <= ' ' }
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
        if (txtVolume.text.matches(Regex(".*${Utils.NUMBER_PATTERN}$"))) {
            val oldVolume = txtVolume.text
            var texto = txtVolume.text.trim { it <= ' ' }
            var volume = texto.replace(texto.replace(Utils.NUMBER_PATTERN.toRegex(), ""), "").trim { it <= ' ' }
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
    private fun onBtnAmazonConsultar() {
        val callback: Callback<ComicInfo, Boolean> = Callback<ComicInfo, Boolean> { param ->
            mComicInfo = param
            null
        }
        PopupAmazon.abreTelaAmazon(controllerPai.rootStack, controllerPai.rootTab, callback, mComicInfo, cbLinguagem.value)
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
        lsVwHistorico.isDisable = true
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
        lsVwHistorico.isDisable = false
        btnProcessar.accessibleTextProperty().set("PROCESSA")
        btnProcessar.text = "Processar"
        controllerPai.setCursor(null)
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

        if (lsVwImagens.selectionModel.selectedItem == null)
            lsVwImagens.selectionModel.select(0)

        if (cbCompactarArquivo.isSelected && txtNomeArquivo.text.isEmpty()) {
            txtSimularPasta.text = "Não informado nome do arquivo."
            txtNomeArquivo.unFocusColor = Color.RED
            AlertasPopup.alertaModal("Alerta", "Não informado nome do arquivo.")
            valida = false
        }

        if (cbLinguagem.value == null) {
            cbLinguagem.unFocusColor = Color.RED
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
        val lingua = Linguagem.getEnum(comic.languageISO) ?: cbLinguagem.value
        cbLinguagem.selectionModel.select(lingua)
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
        comic.languageISO = cbLinguagem.value.sigla
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
        mServiceComicInfo.updateMal(comic, mal, cbLinguagem.value ?: Linguagem.JAPANESE)
        mComicInfo = comic

        val selecionado = when {
            mComicInfo.idMal != null -> Selecionado.SELECIONADO
            mObsListaMal.isNotEmpty() -> Selecionado.SELECIONAR
            else -> Selecionado.VAZIO
        }
        Selecionado.setTabColor(tbTabComicInfo, selecionado)
        tbTabComicInfo.text = "Comic Info" + (if (selecionado == Selecionado.SELECIONADO) " (" + mComicInfo.comic + ")" else "")
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
                    val sugestao = Ocr.process(sumario, Utils.SEPARADOR_PAGINA, Utils.SEPARADOR_CAPITULO)
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
                txtAreaImportar.replaceText(0, txtAreaImportar.length, it.`object` ?: "")
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

                            val selecionado = when {
                                id != null && lista.size == 1 -> Selecionado.SELECIONADO
                                lista.isNotEmpty() -> Selecionado.SELECIONAR
                                else -> Selecionado.VAZIO
                            }
                            Selecionado.setTabColor(tbTabComicInfo, selecionado)
                            tbTabComicInfo.text = "Comic Info" + (if (selecionado == Selecionado.SELECIONADO) " (" + lista[0].nome + ")" else "")
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
            txtAreaImportar.replaceText(0, txtAreaImportar.length, it.capitulos)

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
                        if (capitulo.trim().isEmpty() || !Utils.ONLY_NUMBER_REGEX.containsMatchIn(capitulo))
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
                        if (numero.trim().isNotEmpty() && Utils.ONLY_NUMBER_REGEX.containsMatchIn(numero)) {
                            val capitulo = Utils.getNumber(numero) ?: continue
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
                if (numero.trim().isNotEmpty() && Utils.ONLY_NUMBER_REGEX.containsMatchIn(numero)) {
                    val capitulo = Utils.getNumber(numero) ?: continue
                    if (min == 0.0 || min > capitulo)
                        min = capitulo
                    else
                        max = capitulo
                }
            }

            if (min > 0 && max > 0) {
                val vol = try {
                    (Utils.getNumber(psq.volume) ?: 0).toInt() - (Utils.getNumber(it.volume) ?: 0).toInt()
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

        mComicInfo = mServiceComicInfo.find(nome, cbLinguagem.value.sigla) ?: ComicInfo(null, null, nome, nome)

        if (mComicInfo.id == null) {
            mLOG.info("Gerando novo ComicInfo.")
            txtMalId.text = ""
        } else {
            mLOG.info("ComicInfo localizado: " + mComicInfo.title)
            txtMalId.text = mComicInfo.idMal.toString()
        }

        if (mComicInfo.comic.isEmpty())
            mComicInfo.comic = nome

        if (mComicInfo.series.isEmpty())
            mComicInfo.series = nome

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
            val img = ImageIO.read(arquivo) ?: return false
            result = img.width / img.height > 0.9
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

            if (tipo == TipoCapa.SUMARIO && cbOcrSumario.isSelected)
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

                    if (lsVwImagens.selectionModel.selectedItem != null)
                        mSelecionado = lsVwImagens.selectionModel.selectedItem

                    val nome = txtNomePastaManga.text.trim { it <= ' ' } + " " + txtVolume.text.trim { it <= ' ' }
                    val processar = Historico(
                        nome, txtPastaOrigem.text, txtPastaDestino.text,
                        txtNomePastaManga.text, txtVolume.text, txtNomeArquivo.text, txtNomePastaCapitulo.text, txtGerarInicio.text,
                        txtGerarFim.text, txtAreaImportar.text, mSelecionado ?: "", mManga?.apply { Manga.copy(this) },
                        mComicInfo, mListaCaminhos.map { it.copy() }, mObsListaItens.toList(), mObsListaImagesSelected.map { it.copy() }, mObsListaMal.toList()
                    )

                    lsVwHistorico.items.removeIf { it.nome == nome }
                    lsVwHistorico.items.add(0, processar)

                    mCANCELAR = false
                    var i = 0L
                    var max = mCaminhoOrigem!!.listFiles(mFilterNomeArquivo)?.size?.toLong() ?: 0L
                    val pastasCompactar: MutableList<File> = ArrayList()
                    LAST_PROCESS_FOLDERS.clear()
                    val mesclarCapaTudo = cbMesclarCapaTudo.isSelected
                    val verificaPagDupla = cbVerificaPaginaDupla.isSelected
                    val pastasComic = mutableMapOf<String, File>()
                    updateProgress(i, max)

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
                        updateProgress(i, max)
                        updateMessage("Processando item " + i + " de " + max + ". Copiando - " + arquivos.absolutePath)
                        copiaItem(arquivos, destino)

                        if (contar)
                            contadorCapitulo++

                        if (!btnProcessar.accessibleTextProperty().value.equals("CANCELA", ignoreCase = true))
                            break
                    }

                    val linguagem = if (nomePasta.contains("[JPN]")) Linguagem.JAPANESE else Linguagem.PORTUGUESE

                    val callback = Callback<Triple<Long, Long, String>, Boolean> { param ->
                        if (param.first == -1L) {
                            Platform.runLater { txtSimularPasta.text = param.third }
                        } else {
                            updateProgress(param.first, param.second)
                            updateMessage(param.third)
                        }
                        mCANCELAR
                    }
                    val arquivoZip = mCaminhoDestino!!.path.trim { it <= ' ' } + "\\" + txtNomeArquivo.text.trim { it <= ' ' }

                    val manga = Manga()
                    manga.merge(mManga!!)
                    manga.caminhos = mListaCaminhos

                    if (Compactar.compactar(mCaminhoDestino!!, File(arquivoZip), manga, mComicInfo, pastasCompactar, pastasComic, linguagem, cbCompactarArquivo.isSelected, cbGerarCapitulo.isSelected, callback = callback))
                        LAST_PROCESS_FOLDERS = pastasCompactar
                } catch (e: Exception) {
                    mLOG.error("Erro ao processar.", e)
                    Platform.runLater { AlertasPopup.erroModal("Erro ao processar", e.stackTrace.toString()) }
                }
                return true
            }

            override fun succeeded() {
                updateMessage("Arquivos movidos com sucesso.")
                controllerPai.rootProgress.progressProperty().unbind()
                controllerPai.rootMessage.textProperty().unbind()
                controllerPai.clearProgress()
                habilita()
                lsVwHistorico.refresh()
            }

            override fun failed() {
                super.failed()
                updateMessage("Erro ao mover os arquivos.")
                AlertasPopup.erroModal("Erro ao mover os arquivos", super.getMessage())
                habilita()
            }
        }
        controllerPai.rootProgress.progressProperty().bind(movimentaArquivos.progressProperty())
        controllerPai.rootMessage.textProperty().bind(movimentaArquivos.messageProperty())
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

    private fun isProximoAoBranco(rgb: Int, tolerance: Int): Boolean {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val threshold = 255 - tolerance
        return r >= threshold && g >= threshold && b >= threshold
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
                    if (!isProximoAoBranco(image.getRGB(x, y), COR_TOLERANCIA)) {
                        startX = x
                        break@loop
                    }
                }
            }

            loop@ for (x in endX downTo 0) {
                for (y in 0 until height) {
                    if (!isProximoAoBranco(image.getRGB(x, y), COR_TOLERANCIA)) {
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
                        if (!isProximoAoBranco(image.getRGB(x, y), COR_TOLERANCIA)) {
                            startY = y
                            break@loop
                        }
                    }
                }

                loop@ for (y in endY downTo 0) {
                    for (x in 0 until width) {
                        if (!isProximoAoBranco(image.getRGB(x, y), COR_TOLERANCIA)) {
                            endY = y
                            break@loop
                        }
                    }
                }

                mLOG.info("Corte Y: $startY - $endY")
            }

            val newWidth = endX - startX + 1
            val newHeight = endY - startY + 1

            if (newWidth <= 1 || newHeight <= 1) {
                mLOG.info("Corte inválido resultou em dimensão mínima. Abortando.")
                return arquivo
            }

            val originalArea = width.toDouble() * height.toDouble()
            val newArea = newWidth.toDouble() * newHeight.toDouble()

            if (newArea / originalArea < MARGEM_MINIMO_RECORTE) {
                mLOG.info("O corte resultaria em uma imagem muito pequena. Abortando.")
                return arquivo
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
            image = ImageIO.read(arquivo) ?: return false
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
                controllerPai.setCursor(Cursor.WAIT)
                desabilita()
                processar()
            } else
                mCANCELAR = true
        }
    }

    @FXML
    private fun onBtnCarregarPastaOrigem() {
        mCaminhoOrigem = Utils.selecionaPasta(txtPastaOrigem.text)
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
        mCaminhoDestino = Utils.selecionaPasta(txtPastaDestino.text)
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
        lsVwImagens.items = mObsListaItens
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

    private fun incrementaCapitulos(volumeAtual: String, volumeAnterior: String) {
        if (volumeAnterior == volumeAtual || txtGerarFim.text.isNullOrEmpty() || txtGerarInicio.text.isNullOrEmpty())
            return

        val volAtual = Utils.getNumber(volumeAtual) ?: return
        val volAnterior = Utils.getNumber(volumeAnterior) ?: return

        val capInicio = Utils.getNumber(txtGerarInicio.text) ?: return
        val capFim = Utils.getNumber(txtGerarFim.text) ?: return

        val diferenca = volAtual - volAnterior
        val capitulos = capFim - capInicio

        val inicio = capInicio + (diferenca * capitulos) + (1 * diferenca)
        if (inicio <= 0)
            return

        txtGerarInicio.text = inicio.toInt().toString()
        txtGerarFim.text = (inicio + capitulos).toInt().toString()
        onBtnGerarCapitulos()
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
            val pipe = Utils.SEPARADOR_PAGINA
            val separador = Utils.SEPARADOR_CAPITULO
            val linhas = txtAreaImportar.text.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var linha: Array<String>
            mListaCaminhos = ArrayList()

            val pasta = txtNomePastaCapitulo.text.trim { it <= ' ' }
            for (ls in linhas) {
                val texto = if (ls.contains(separador)) ls.substringBefore(separador) else ls
                linha = texto.split(Utils.SEPARADOR_PAGINA.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                nomePasta = if (pasta.equals("Capítulo", ignoreCase = true) && linha[0].uppercase(Locale.getDefault()).contains("EXTRA"))
                    linha[0].trim { it <= ' ' }
                else
                    pasta + " " + linha[0].trim { it <= ' ' }

                val tag = if (cbGerarCapitulo.isSelected && ls.contains(separador)) ls.substringAfter(separador) else ""
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
            val inicio = Utils.getNumber(txtGerarInicio.text)?.toInt() ?: return
            val fim = Utils.getNumber(txtGerarFim.text)?.toInt() ?: return
            if (inicio <= fim) {
                var texto = ""
                val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3") + "d")
                for (i in inicio..fim)
                    texto += String.format(padding, i) + Utils.SEPARADOR_PAGINA + if (i < fim) "\r\n" else ""
                txtAreaImportar.replaceText(0, txtAreaImportar.length, texto)
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
                mSincronizacao.consultar()
                return mSincronizacao.sincroniza()
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
                mAnimacao.tmSincronizacao.play()
            else {
                mAnimacao.tmSincronizacao.stop()
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

    private var mDelaySubir: Timer? = null
    private var mDelayDescer: Timer? = null
    private fun selecionaImagens() {
        mObsListaImagesSelected = FXCollections.observableArrayList()
        lsVwImagens.addEventFilter(ScrollEvent.ANY) { e: ScrollEvent ->
            if (e.deltaY > 0) {
                if (e.deltaY > 10) {
                    btnScrollSubir.isVisible = true
                    btnScrollSubir.isDisable = false
                    if (mDelaySubir != null)
                        mDelaySubir!!.cancel()
                    mDelaySubir = Timer()
                    mDelaySubir!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollSubir.isVisible = false
                            btnScrollSubir.isDisable = true
                            mDelaySubir = null
                        }
                    }, 3000)
                }
            } else {
                if (e.deltaY < 10) {
                    btnScrollDescer.isVisible = true
                    btnScrollDescer.isDisable = false
                    if (mDelayDescer != null)
                        mDelayDescer!!.cancel()
                    mDelayDescer = Timer()
                    mDelayDescer!!.schedule(object : TimerTask() {
                        override fun run() {
                            btnScrollDescer.isVisible = false
                            btnScrollDescer.isDisable = true
                            mDelayDescer = null
                        }
                    }, 3000)
                }
            }
        }
        lsVwImagens.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1) {
                if (click.isControlDown)
                    limparCapas()
                else {
                    val item = lsVwImagens.selectionModel.selectedItem
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
        lsVwImagens.setCellFactory {
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

        editaColunas()
        selecionaImagens()

        tbViewMal.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1 && tbViewMal.items.isNotEmpty())
                carregaMal(tbViewMal.selectionModel.selectedItem)
        }

        lsVwHistorico.setCellFactory {
            object : ListCell<Historico?>() {
                override fun updateItem(historico: Historico?, empty: Boolean) {
                    super.updateItem(historico, empty)
                    text = if (empty || historico == null) null else historico.nome
                }
            }
        }

        lsVwHistorico.onMouseClicked = EventHandler { click: MouseEvent ->
            if (click.clickCount > 1 && lsVwHistorico.items.isNotEmpty()) {
                val item = lsVwHistorico.selectionModel.selectedItem
                if (item != null) {
                    txtPastaOrigem.text = item.pastaOrigem
                    txtPastaDestino.text = item.pastaDestino
                    txtNomePastaManga.text = item.nomeManga
                    txtVolume.text = item.volume
                    txtNomePastaCapitulo.text = item.pastaCapitulo
                    txtGerarInicio.text = item.inicio
                    txtGerarFim.text = item.fim
                    txtNomeArquivo.text = item.nomeArquivo
                    txtAreaImportar.text = item.importar
                    mManga = item.manga?.apply { Manga.copy(this) }
                    mComicInfo = ComicInfo(item.comicInfo)

                    mListaCaminhos = ArrayList(item.caminhos.map { it.copy() })
                    mObsListaCaminhos = FXCollections.observableArrayList(mListaCaminhos)
                    tbViewTabela.items = mObsListaCaminhos

                    limparCapas()
                    mObsListaItens = FXCollections.observableArrayList(item.itens)
                    lsVwImagens.items = mObsListaItens
                    mObsListaImagesSelected.addAll(item.capas.map { it.copy() })

                    mObsListaMal = FXCollections.observableArrayList(item.mal)
                    tbViewMal.items = mObsListaMal

                    lsVwImagens.refresh()
                    tbViewTabela.refresh()
                    tbViewMal.refresh()

                    if (item.selecionado.isNotEmpty())
                        lsVwImagens.selectionModel.select(lsVwImagens.items.indexOf(item.selecionado))
                    else
                        lsVwImagens.selectionModel.selectFirst()

                    acdArquivos.expandedPane = ttpArquivos
                }
            }
        }
    }

    private fun addCapitulo(capitulo: String) : String {
        if (capitulo.isEmpty())
            return ""
        val numero = Utils.getNumber(capitulo) ?: return ""
        return numero.plus(1).toInt().toString()
    }

    private fun minCapitulo(capitulo: String) : String {
        if (capitulo.isEmpty())
            return ""
        var numero = Utils.getNumber(capitulo) ?: return ""
        numero = if (numero <= 1)
            1.0
        else
            numero.minus(1)
        return numero.toInt().toString()
    }

    private val mCoroutine = CoroutineScope(Dispatchers.JavaFx + SupervisorJob())
    private var mCarregaSugestao: Job? = null
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
            when (e.code) {
                KeyCode.ENTER -> txtVolume.requestFocus()
                KeyCode.TAB -> {
                    if (!e.isControlDown && !e.isAltDown && !e.isShiftDown) {
                        txtPastaDestino.requestFocus()
                        e.consume()
                    }
                }
                KeyCode.R -> {
                    if (e.isControlDown)
                        carregaPastaOrigem()
                }
                else -> { }
            }
        }

        txtPastaDestino.focusedProperty().addListener { _: ObservableValue<out Boolean>?, oldPropertyValue: Boolean, _: Boolean? ->
            if (oldPropertyValue)
                carregaPastaDestino()
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

        txtNomePastaManga.onKeyPressed = EventHandler { e: KeyEvent ->
            if (e.code == KeyCode.ENTER)
                Utils.clickTab()
            else {
                mCarregaSugestao?.cancel()
                mCarregaSugestao = mCoroutine.launch {
                    mAutoComplete.suggestions.clear()
                    if (mAutoComplete.isShowing)
                        mAutoComplete.hide()

                    delay(1000)

                    if (txtNomePastaManga.text == null || txtNomePastaManga.text.isEmpty())
                        return@launch

                    try {
                        var nome = txtNomePastaManga.text
                        if (nome.contains("]"))
                            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim { it <= ' ' }

                        if (nome.substring(nome.length - 1).equals("-", ignoreCase = true))
                            nome = nome.substring(0, nome.length - 1).trim { it <= ' ' }

                        if (nome.isNotEmpty()) {
                            mAutoComplete.suggestions.addAll(mServiceManga.sugestao(nome))
                            if (mAutoComplete.suggestions.isNotEmpty())
                                mAutoComplete.show(txtNomePastaManga)
                        }
                    } catch (e: SQLException) {
                        mLOG.error(e.message, e)
                        println("Erro ao consultar as sugestões de mangas.")
                    }
                }
            }
        }

        mAutoComplete.setSelectionHandler { event ->
            var nome = event.getObject() + " -"

            if (txtNomePastaManga.text != null && txtNomePastaManga.text.contains("]"))
                nome = txtNomePastaManga.text.substringBefore("]") + "] " + nome

            txtNomePastaManga.text = nome
        }

        txtNomePastaManga.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if ((event.isControlDown || event.isMetaDown) && event.code == KeyCode.V || event.isShiftDown && event.code == KeyCode.INSERT) {
                val clipboard = Clipboard.getSystemClipboard()
                if (clipboard.hasString()) {
                    val matchs = Regex("""(\[JPN\][\s\S]+)(\- Volume)""", RegexOption.IGNORE_CASE).find(clipboard.string)
                    if (matchs != null) {
                        txtNomePastaManga.replaceText(0, txtNomePastaManga.length, matchs.groupValues[1] + " -")
                        txtNomePastaManga.positionCaret(txtNomePastaManga.text.length)
                        event.consume()
                    }
                }
            }
        }

        txtNomeArquivo.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            txtPastaDestino.unFocusColor = Color.GRAY
        }
        txtNomeArquivo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
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
        txtNomePastaCapitulo.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code.toString() == "ENTER") Utils.clickTab() }

        txtGerarInicio.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            txtPastaDestino.unFocusColor = Color.GRAY

            if (!txtGerarInicio.text.isNullOrEmpty() && !txtGerarFim.text.isNullOrEmpty()) {
                val inicio = Utils.getNumber(txtGerarInicio.text)?.toInt() ?: return@addListener
                val fim = Utils.getNumber(txtGerarFim.text)?.toInt() ?: return@addListener
                if (inicio > fim)
                    txtGerarFim.text = inicio.plus(1).toString()
            }
        }
        txtGerarInicio.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(Utils.NUMBER_REGEX))
                txtGerarInicio.text = oldValue
        }
        txtGerarInicio.onKeyPressed = EventHandler { e: KeyEvent ->
            when(e.code) {
                KeyCode.ENTER -> {
                    Utils.clickTab()
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
            if (newValue != null && !newValue.matches(Utils.NUMBER_REGEX))
                txtGerarFim.text = oldValue
        }
        txtGerarFim.onKeyPressed = EventHandler { e: KeyEvent ->
            when(e.code) {
                KeyCode.ENTER -> {
                    onBtnGerarCapitulos()
                    txtAreaImportar.requestFocus()
                    val position = txtAreaImportar.text.indexOf(Utils.SEPARADOR_PAGINA) + 1
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
            if (e.isControlDown && !e.isShiftDown && !e.isAltDown) {
                when (e.code) {
                    KeyCode.ENTER -> onBtnImporta()
                    KeyCode.S -> {
                        if (mSugestao.suggestions.isNotEmpty())
                            mSugestao.show(txtAreaImportar)
                    }
                    KeyCode.T -> {
                        if (txtAreaImportar.text.isEmpty() || !txtAreaImportar.text.contains(Utils.SEPARADOR_CAPITULO))
                            return@EventHandler

                        val txt = txtAreaImportar.text
                        val scroll = txtAreaImportar.scrollTopProperty().value

                        val separador = Utils.SEPARADOR_CAPITULO
                        val texto = mutableListOf<String>()
                        for (linha in txt.split("\n"))
                            texto.add(if (linha.contains(separador)) linha.substringBeforeLast(separador) else linha)

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        var line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                        line = if (line.contains(separador)) line.substringBeforeLast(separador) else line

                        txtAreaImportar.replaceText(0, txtAreaImportar.length, texto.joinToString("\n"))
                        txtAreaImportar.positionCaret(txtAreaImportar.text.indexOf(line) + line.length)
                        txtAreaImportar.scrollTop = scroll
                    }
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

                        val pipe = Utils.SEPARADOR_PAGINA
                        val separador = Utils.SEPARADOR_CAPITULO
                        var tag = ""
                        val texto = if (line.contains(separador)) {
                            tag = separador + line.substringAfter(separador)
                            line.substringBefore(separador)
                        } else
                            line
                        val page = if (texto.contains(pipe)) texto.substringAfter(pipe) else ""

                        val newLine = when (e.code) {
                            KeyCode.E -> {
                                if (texto.contains("extra", true)) {
                                    val fim = Utils.getNumber(txtGerarFim.text)?.toInt() ?: 0
                                    val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3") + "d")
                                    var sequence = txt.split("\n").last { !it.contains("extra", ignoreCase = true) }
                                    sequence = if (sequence.contains(pipe)) sequence.substringBefore(pipe) else sequence
                                    (Utils.getNumber(sequence)?.toInt()?.let { "${String.format(padding, it+1)}$pipe$page" } ?: sequence) + tag
                                } else {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    "Extra ${String.format("%02d", count + 1)}$pipe$page$tag"
                                }
                            }
                            KeyCode.D ->  {
                                if (line.contains("extra", true) && last.isEmpty()) {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    line + "\n" + "Extra ${String.format("%02d", count + 1)}$pipe$page$tag"
                                } else
                                    line + "\n" + line
                            }
                            in (KeyCode.NUMPAD0 .. KeyCode.NUMPAD9),
                            in (KeyCode.DIGIT0 .. KeyCode.DIGIT9) -> {
                                if (texto.contains("extra", true)) {
                                    val count = txt.split("\n").sumOf { if (it.contains("extra", ignoreCase = true)) 1 else 0 as Int }
                                    val number = if (e.code == KeyCode.DIGIT0 || e.code == KeyCode.NUMPAD0) count else e.text.toInt()
                                    "Extra ${String.format("%02d", number)}$pipe$page$tag"
                                } else {
                                    val chapter = if (texto.contains("."))
                                        texto.substringBefore(".")
                                    else if (texto.contains(pipe))
                                        texto.substringBefore(pipe)
                                    else
                                        texto
                                    val number = if (e.code == KeyCode.DIGIT0 || e.code == KeyCode.NUMPAD0) "" else "." + e.text
                                    "$chapter$number$pipe$page$tag"
                                }
                            }
                            else -> line
                        }

                        val newText = before + newLine + last
                        val position = if (newLine.contains(separador)) newLine.lastIndexOf(separador) else newLine.length

                        txtAreaImportar.replaceText(0, txtAreaImportar.length, newText)
                        lastCaretPos = before.length + position
                        txtAreaImportar.positionCaret(lastCaretPos)
                        txtAreaImportar.scrollTop = scroll
                    }
                    else -> {}
                }
            } else if (e.isControlDown && (e.isShiftDown || e.isAltDown)) {
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

                        txtAreaImportar.replaceText(0, txtAreaImportar.length, newText.substringBeforeLast("\n"))
                        txtAreaImportar.positionCaret(txtAreaImportar.text.indexOf(line))
                        txtAreaImportar.scrollTop = scroll
                    }
                    KeyCode.RIGHT,
                    KeyCode.LEFT -> {
                        if (txtAreaImportar.text.isEmpty())
                            return@EventHandler

                        val txt = txtAreaImportar.text
                        val scroll = txtAreaImportar.scrollTopProperty().value

                        val pipe = Utils.SEPARADOR_PAGINA
                        val separador = Utils.SEPARADOR_CAPITULO
                        val fim = Utils.getNumber(txtGerarFim.text)?.toInt() ?: 0
                        val padding = ("%0" + (if (fim.toString().length > 3) fim.toString().length.toString() else "3") + "d")

                        var moveCapitulo: (String) -> String = { linha ->
                            var tag = ""
                            val texto = if (linha.contains(separador)) {
                                tag = separador + linha.substringAfter(separador)
                                linha.substringBefore(separador)
                            } else
                                linha

                            val itens = texto.split(pipe)
                            if (itens.size == 2) {
                                if (itens[1].trim().isNotEmpty() && Utils.ONLY_NUMBER_REGEX.containsMatchIn(itens[1]))
                                    String.format(padding, itens[1].toInt()) + pipe + itens[0] + tag
                                else
                                    itens[1] + pipe + itens[0] + tag
                            } else linha
                        }

                        val position: Int
                        val newText: String

                        if (e.isShiftDown && e.isAltDown) {
                            val texto = mutableListOf<String>()
                            for (linha in txt.split("\n"))
                                texto.add(moveCapitulo(linha))
                            newText = texto.joinToString("\n")
                            position = newText.length
                        } else {
                            var before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                            val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                            val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")
                            before = before.substringBeforeLast(line)

                            val newLine = moveCapitulo(line)
                            newText = before + newLine + last
                            position = before.length + if (newLine.contains(separador)) newLine.lastIndexOf(separador) else newLine.length
                        }

                        txtAreaImportar.replaceText(0, txtAreaImportar.length, newText)
                        lastCaretPos = position
                        txtAreaImportar.positionCaret(lastCaretPos)
                        txtAreaImportar.scrollTop = scroll
                    }
                    else -> {}
                }
            } else if (e.isShiftDown && e.isAltDown) {
                when (e.code) {
                    KeyCode.UP,
                    KeyCode.DOWN -> {
                        if (txtAreaImportar.text.isEmpty() || !txtAreaImportar.text.contains("\n") || !txtAreaImportar.text.contains(Utils.SEPARADOR_CAPITULO))
                            return@EventHandler

                        val txt = txtAreaImportar.text ?: ""
                        val lines = txt.split("\n").toMutableList()
                        val scroll = txtAreaImportar.scrollTopProperty().value

                        val before = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(0, txt.indexOf('\n', lastCaretPos)) else txt
                        val last = if (txt.indexOf('\n', lastCaretPos) > 0) txt.substring(txt.indexOf('\n', lastCaretPos)) else ""
                        val line = before.substringAfterLast("\n", before) + last.substringBefore("\n", "")

                        if (!line.contains(Utils.SEPARADOR_CAPITULO))
                            return@EventHandler

                        val separador = Utils.SEPARADOR_CAPITULO

                        var index = -1
                        for ((idx, item) in lines.withIndex()) {
                            if (item == line) {
                                index = idx
                                break
                            }
                        }

                        if (e.code == KeyCode.UP) {
                            if (index > 0) {
                                if (index >= lines.size)
                                    index = lines.size - 2

                                var spaco = -1
                                for (i in (index - 1) downTo 2) {
                                    if (!lines[i].contains(separador)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = 1
                                if (spaco > 0)
                                    inicio = spaco + 1

                                for (i in inicio until index + 1) {
                                    lines[i-1] = lines[i-1] + if (lines[i].contains(separador)) separador + lines[i].substringAfterLast(separador) else ""
                                    lines[i] = lines[i].substringBeforeLast(separador)
                                }
                            }
                        } else if (e.code == KeyCode.DOWN) {
                            if (index < lines.size - 1) {
                                var spaco = -1
                                for (i in index + 1 until lines.size) {
                                    if (!lines[i].contains(separador)) {
                                        spaco = i
                                        break
                                    }
                                }

                                var inicio = lines.size - 2
                                if (spaco > 0 && spaco < lines.size -1)
                                    inicio = spaco - 1

                                for (i in inicio downTo index) {
                                    lines[i+1] = lines[i+1] + if (lines[i].contains(separador)) separador + lines[i].substringAfterLast(separador) else ""
                                    lines[i] = lines[i].substringBeforeLast(separador)
                                }
                            }
                        }

                        txtAreaImportar.replaceText(0, txtAreaImportar.length, lines.joinToString(separator = "\n"))

                        val caret = if (e.code == KeyCode.UP)
                            txtAreaImportar.text.substring(0, lastCaretPos).lastIndexOf(separador)
                        else
                            txtAreaImportar.text.indexOf(separador, lastCaretPos)

                        txtAreaImportar.positionCaret(caret)
                        txtAreaImportar.scrollTop = scroll
                    }
                    else -> {}
                }
            }
            lastCaretPos = txtAreaImportar.caretPosition
        }
        val menu = ContextMenu()
        val sugestao = MenuItem("Abrir menu sugestão")
        sugestao.setOnAction {
            if (mSugestao.suggestions.isNotEmpty())
                mSugestao.show(txtAreaImportar)
        }
        val capitulos = MenuItem("Importar capítulos da sugestão")
        capitulos.setOnAction {
            if (mSugestao.suggestions.isNotEmpty()) {
                var texto = ""
                val capitulos = mSugestao.suggestions[0].split("\n")
                val separador = Utils.SEPARADOR_CAPITULO
                if (capitulos.isNotEmpty() && mSugestao.suggestions[0].contains(separador)) {
                    val importar = txtAreaImportar.text.split("\n")
                    for ((index, capitulo) in capitulos.withIndex()) {
                        if (capitulo.contains(separador)) {
                            texto += if (index < importar.size)
                                importar[index] + separador + capitulo.substringAfter(separador) + "\n"
                            else
                                separador + capitulo.substringAfter(separador) + "\n"
                        }
                    }
                    if (importar.size > capitulos.size) {
                        for (i in capitulos.size until importar.size)
                            texto += importar[i] + "\n"
                    }

                    val caret = txtAreaImportar.caretPosition
                    val scroll = txtAreaImportar.scrollTopProperty().value
                    txtAreaImportar.replaceText(0, txtAreaImportar.length, texto.substringBeforeLast("\n"))
                    txtAreaImportar.positionCaret(caret)
                    txtAreaImportar.scrollTop = scroll
                    lastCaretPos = txtAreaImportar.caretPosition
                }
            }
        }
        menu.items.add(sugestao)
        menu.items.add(capitulos)
        txtAreaImportar.contextMenu = menu

        txtQuantidade.focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> txtPastaDestino.unFocusColor = Color.GRAY }
        txtQuantidade.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(Utils.NUMBER_REGEX))
                txtGerarFim.text = oldValue
        }

        cbMesclarCapaTudo.selectedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> reloadCapa() }
        cbAjustarMargemCapa.selectedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? -> reloadCapa() }

        cbAgeRating.items.addAll(AgeRating.values())
        cbLinguagem.items.addAll(Linguagem.JAPANESE, Linguagem.ENGLISH, Linguagem.PORTUGUESE)
        cbLinguagem.selectionModel.select(Linguagem.JAPANESE)

        cbLinguagem.focusedProperty().addListener { _, _, newPropertyValue ->
            if (newPropertyValue)
                cbLinguagem.setUnFocusColor(Color.web("#4059a9"))
            else {
                if (cbLinguagem.value == null)
                    cbLinguagem.setUnFocusColor(Color.RED)
                else
                    cbLinguagem.setUnFocusColor(Color.web("#4059a9"))
            }
        }

        txtIdMal.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        cbAgeRating.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        cbLinguagem.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtTitle.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtAlternateSeries.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtSeriesGroup.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtPublisher.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtStoryArc.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtImprint.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }
        txtGenre.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) Utils.clickTab() }

        txtMalId.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }
        txtMalNome.onKeyPressed = EventHandler { e: KeyEvent -> if (e.code == KeyCode.ENTER) btnMalConsultar.fire() }

        txtIdMal.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && !newValue.matches(Utils.NUMBER_REGEX))
                txtIdMal.text = oldValue
        }

        txtMalId.textProperty().addListener { _: ObservableValue<out String?>?, oldValue: String?, newValue: String? ->
            if (newValue != null && newValue.isNotEmpty() && !newValue.matches(Utils.NUMBER_REGEX))
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
        val kcHistorico: KeyCombination = KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN)

        val kcProcessar: KeyCombination = KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN)
        val mnProcessar = Mnemonic(btnProcessar, kcProcessar)
        scene.addMnemonic(mnProcessar)

        val kcProcessarAlter: KeyCombination = KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN)
        val mnProcessarAlter = Mnemonic(btnProcessar, kcProcessarAlter)
        scene.addMnemonic(mnProcessarAlter)

        scene.addEventFilter(KeyEvent.KEY_PRESSED) { ke: KeyEvent ->
            if (ke.isControlDown && lsVwImagens.selectionModel.selectedItem != null)
                mSelecionado = lsVwImagens.selectionModel.selectedItem

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
                    if (isAbaSelecionada)
                        btnMalConsultar.fire()
                } else
                    tbTabRoot.selectionModel.select(tbTabComicInfo)
            }

            if (kcArquivos.match(ke)) {
                if (tbTabRoot.selectionModel.selectedItem != tbTabArquivo)
                    tbTabRoot.selectionModel.select(tbTabArquivo)
                else if (acdArquivos.expandedPane != ttpArquivos)
                    acdArquivos.expandedPane = ttpArquivos
            }

            if (kcHistorico.match(ke)) {
                if (acdArquivos.expandedPane != ttpHistorico)
                    acdArquivos.expandedPane = ttpHistorico
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

    @Synchronized
    override fun initialize(arg0: URL, arg1: ResourceBundle?) {
        configuraImagens()
        linkaCelulas()
        configuraTextEdit()
        mAnimacao.animaSincronizacao(imgCompartilhamento, imgAnimaCompartilha, imgAnimaCompartilhaEspera)
        mSincronizacao.setObserver { observable: ListChangeListener.Change<out Pair<Tipo, Int>> ->
            if (!mSincronizacao.isSincronizando()) {
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

        if (!mSincronizacao.isConfigurado())
            imgCompartilhamento.image = imgAnimaCompartilhaErro
        else if (mSincronizacao.listSize() > 0) {
            lblAviso.text = "Pendente de envio " + mSincronizacao.listSize() + " registro(s)."
            imgCompartilhamento.image = imgAnimaCompartilhaEspera
        } else
            imgCompartilhamento.image = imgAnimaCompartilha

        mSugestao.cellLimit = 1
        lsVwHistorico.items = FXCollections.observableArrayList()
        acdArquivos.expandedPane = ttpArquivos
    }

    companion object {
        /**
         * Define a tolerância para o que é considerado "branco".
         * Um valor de 15 significa que qualquer cor onde R, G e B são todos >= 240 (255-15) será tratada como branco.
         * Aumente este valor se as margens ainda não estiverem sendo detectadas corretamente.
         */
        private const val COR_TOLERANCIA = 15

        /**
         * Define a proporção mínima da área da imagem que deve restar após o corte.
         * Um valor de 0.25 significa que se o corte remover mais de 75% da imagem original,
         * a operação será cancelada para evitar cortes incorretos em imagens com muito branco.
         */
        private const val MARGEM_MINIMO_RECORTE = 0.25

        private var LAST_PROCESS_FOLDERS: MutableList<File> = ArrayList()
        val fxmlLocate: URL get() = TelaInicialController::class.java.getResource("/view/AbaArquivo.fxml")
        var isAbaSelecionada = false

        val imgAnimaCompartilha = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamento_48.png"))
        val imgAnimaCompartilhaEspera = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEspera_48.png"))
        val imgAnimaCompartilhaErro = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoErro_48.png"))
        val imgAnimaCompartilhaEnvio = Image(TelaInicialController::class.java.getResourceAsStream("/images/icoCompartilhamentoEnvio_48.png"))
    }
}