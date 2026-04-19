package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Mal
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.ImageView
import javafx.stage.Stage
import java.net.URL
import java.util.*

class PopupComicInfoController : Initializable {

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
    private lateinit var txtComic: JFXTextField

    @FXML
    private lateinit var txtPublisher: JFXTextField

    @FXML
    private lateinit var txtAlternateSeries: JFXTextField

    @FXML
    private lateinit var txtSeriesGroup: JFXTextField

    @FXML
    private lateinit var txtStoryArc: JFXTextField

    @FXML
    private lateinit var txtGenre: JFXTextField

    @FXML
    private lateinit var txtImprint: JFXTextField

    @FXML
    private lateinit var txtNotes: JFXTextArea

    @FXML
    private lateinit var txtMalId: JFXTextField

    @FXML
    private lateinit var txtMalNome: JFXTextField

    @FXML
    private lateinit var btnMalConsultar: JFXButton

    @FXML
    private lateinit var btnMalAplicar: JFXButton

    @FXML
    private lateinit var tbViewMal: TableView<Mal>

    @FXML
    private lateinit var clMalId: TableColumn<Mal, Long>

    @FXML
    private lateinit var clMalNome: TableColumn<Mal, String>

    @FXML
    private lateinit var clMalSite: TableColumn<Mal, JFXButton>

    @FXML
    private lateinit var clMalImagem: TableColumn<Mal, ImageView>

    @FXML
    private lateinit var btnConfirmar: JFXButton

    @FXML
    private lateinit var btnCancelar: JFXButton

    var onClose: (() -> Unit)? = null
    private lateinit var mComicInfo: ComicInfo
    private val mServiceComicInfo = ComicInfoServices()
    private var mObsListaMal: ObservableList<Mal> = FXCollections.observableArrayList()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initTable()
        initCombos()
    }

    private fun initTable() {
        clMalId.cellValueFactory = PropertyValueFactory("id")
        clMalNome.cellValueFactory = PropertyValueFactory("nome")
        clMalSite.cellValueFactory = PropertyValueFactory("site")
        clMalImagem.cellValueFactory = PropertyValueFactory("imagem")
        tbViewMal.items = mObsListaMal
    }

    private fun initCombos() {
        cbAgeRating.items.setAll(*AgeRating.values())
        cbLinguagem.items.setAll(*Linguagem.values())
        cbLinguagem.value = Linguagem.JAPANESE
    }

    fun setComicInfo(comicInfo: ComicInfo?) {
        this.mComicInfo = comicInfo ?: ComicInfo()
        carregaCampos()
    }

    private fun carregaCampos() {
        txtIdMal.text = mComicInfo.idMal?.toString() ?: ""
        cbAgeRating.value = mComicInfo.ageRating ?: AgeRating.Unknown
        cbLinguagem.value = Linguagem.getEnum(mComicInfo.languageISO ?: "ja") ?: Linguagem.JAPANESE
        txtTitle.text = mComicInfo.title ?: ""
        txtSeries.text = mComicInfo.series ?: ""
        txtComic.text = mComicInfo.comic ?: ""
        txtPublisher.text = mComicInfo.publisher ?: ""
        txtAlternateSeries.text = mComicInfo.alternateSeries ?: ""
        txtSeriesGroup.text = mComicInfo.seriesGroup ?: ""
        txtStoryArc.text = mComicInfo.storyArc ?: ""
        txtGenre.text = mComicInfo.genre ?: ""
        txtImprint.text = mComicInfo.imprint ?: ""
        txtNotes.text = mComicInfo.notes ?: ""

        txtMalNome.text = mComicInfo.series ?: mComicInfo.title ?: ""
    }

    private fun atualizaObjeto() {
        mComicInfo.idMal = txtIdMal.text.toLongOrNull()
        mComicInfo.ageRating = cbAgeRating.value
        mComicInfo.languageISO = cbLinguagem.value?.sigla ?: "ja"
        mComicInfo.title = txtTitle.text
        mComicInfo.series = txtSeries.text
        mComicInfo.comic = txtComic.text
        mComicInfo.publisher = txtPublisher.text
        mComicInfo.alternateSeries = txtAlternateSeries.text
        mComicInfo.seriesGroup = txtSeriesGroup.text
        mComicInfo.storyArc = txtStoryArc.text
        mComicInfo.genre = txtGenre.text
        mComicInfo.imprint = txtImprint.text
        mComicInfo.notes = txtNotes.text
    }

    @FXML
    private fun onBtnMalConsultar() {
        val id = txtMalId.text.toLongOrNull()
        val nome = txtMalNome.text

        if (nome.isEmpty() && id == null) return

        btnMalConsultar.isDisable = true
        val task = object : Task<List<Mal>>() {
            override fun call(): List<Mal> {
                return mServiceComicInfo.getMal(id, nome)
            }

            override fun succeeded() {
                mObsListaMal.setAll(value)
                btnMalConsultar.isDisable = false
            }

            override fun failed() {
                btnMalConsultar.isDisable = false
                exception.printStackTrace()
            }
        }
        Thread(task).start()
    }

    @FXML
    private fun onBtnMalAplicar() {
        val selected = tbViewMal.selectionModel.selectedItem ?: return
        atualizaObjeto()
        mServiceComicInfo.updateMal(mComicInfo, selected, cbLinguagem.value ?: Linguagem.JAPANESE)
        carregaCampos()
    }

    @FXML
    private fun onBtnConfirmar() {
        atualizaObjeto()
        try {
            mServiceComicInfo.save(mComicInfo)
            fechar()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @FXML
    private fun onBtnCancelar() {
        fechar()
    }

    private fun fechar() {
        onClose?.invoke() ?: run {
            val stage = btnConfirmar.scene.window as Stage
            stage.close()
        }
    }
}
