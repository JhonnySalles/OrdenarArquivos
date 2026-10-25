package com.fenix.ordenararquivos.controller

import com.fenix.ordenararquivos.BaseJfxTest
import com.fenix.ordenararquivos.model.entities.comicinfo.AgeRating
import com.fenix.ordenararquivos.model.entities.comicinfo.ComicInfo
import com.fenix.ordenararquivos.model.entities.comicinfo.Pages
import com.fenix.ordenararquivos.model.enums.Linguagem
import com.fenix.ordenararquivos.service.ComicInfoServices
import com.fenix.ordenararquivos.service.MangaServices
import com.fenix.ordenararquivos.service.WinrarServices
import com.jfoenix.controls.JFXComboBox
import com.jfoenix.controls.JFXTextArea
import com.jfoenix.controls.JFXTextField
import jakarta.xml.bind.JAXBContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field

class AbaArquivoGravarComicInfoTest : BaseJfxTest() {

    private lateinit var controller: AbaArquivoController
    private val mangaService: MangaServices = mock()
    private val comicInfoService: ComicInfoServices = mock()
    private val winrarService: WinrarServices = mock()
    private val telaInicialController: TelaInicialController = mock()
    private val sincronizacaoService: com.fenix.ordenararquivos.service.SincronizacaoServices = mock()

    @TempDir
    lateinit var tempFolder: File

    @BeforeEach
    fun setUp() {
        val latch = java.util.concurrent.CountDownLatch(1)
        javafx.application.Platform.runLater {
            com.fenix.ordenararquivos.notification.Notificacoes.rootAnchorPane = javafx.scene.layout.AnchorPane()
            latch.countDown()
        }
        latch.await()

        reset(mangaService, comicInfoService, winrarService, telaInicialController, sincronizacaoService)
        whenever(telaInicialController.mSincronizacao).thenReturn(sincronizacaoService)

        controller = AbaArquivoController()
        setField("controller", telaInicialController)

        setField("mServiceManga", mangaService)
        setField("mServiceComicInfo", comicInfoService)
        setField("mRarService", winrarService)

        // Inicializa campos de UI necessários
        setField("mCaminhoDestino", tempFolder)
        setField("txtNomeArquivo", JFXTextField("Manga_v05.cbr"))
        setField("txtVolume", JFXTextField("Volume 05"))

        setField("txtIdMal", JFXTextField("12345"))
        setField("txtIdAnilist", JFXTextField("67890"))
        setField("cbAgeRating", JFXComboBox<AgeRating>().apply {
            items.addAll(AgeRating.values().toList())
            value = AgeRating.Teen
        })
        setField("cbLinguagem", JFXComboBox<Linguagem>().apply {
            items.addAll(Linguagem.values().toList())
            value = Linguagem.PORTUGUESE
        })
        setField("txtTitle", JFXTextField("Capitulo Novo"))
        setField("txtSeries", JFXTextField("Série Atualizada"))
        setField("txtAlternateSeries", JFXTextField("Alt Series"))
        setField("txtSeriesGroup", JFXTextField("Grupo"))
        setField("txtPublisher", JFXTextField("Nova Editora"))
        setField("txtStoryArc", JFXTextField("Arco 1"))
        setField("txtImprint", JFXTextField("Imprint X"))
        setField("txtGenre", JFXTextField("Aventura"))
        setField("txtNotes", JFXTextArea("Novas notas"))
    }

    private fun setField(name: String, value: Any?) {
        val field: Field = findField(controller.javaClass, name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private fun findField(clazz: Class<*>, name: String): Field {
        return try {
            clazz.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            if (clazz.superclass != null) findField(clazz.superclass, name)
            else throw e
        }
    }

    @Test
    fun `test onBtnGravarComicInfo preserva volume e paginas de arquivo existente no RAR`() {
        // Cria arquivo RAR/CBR simulado na pasta de destino
        val archiveFile = File(tempFolder, "Manga_v05.cbr")
        archiveFile.createNewFile()

        // Simula conteúdo do RAR com ComicInfo.xml
        whenever(winrarService.listarConteudo(archiveFile)).thenReturn(listOf("ComicInfo.xml", "001.jpg", "002.jpg"))

        // Mock extrairItens para escrever um ComicInfo.xml com Volume 5 e 2 páginas
        whenever(winrarService.extrairItens(eq(archiveFile), any(), any())).thenAnswer { invocation ->
            val destDir = invocation.getArgument<File>(2)
            val xmlOriginal = File(destDir, "ComicInfo.xml")
            val infoOriginal = ComicInfo().apply {
                volume = 5
                number = 5f
                count = 5
                pageCount = 2
                pages = listOf(
                    Pages(image = 0, imageWidth = 800, imageHeight = 1200),
                    Pages(image = 1, imageWidth = 800, imageHeight = 1200)
                )
                summary = "Capítulos 20 ao 25"
                series = "Série Antiga"
            }
            val marshaller = JAXBContext.newInstance(ComicInfo::class.java).createMarshaller()
            FileOutputStream(xmlOriginal).use { out -> marshaller.marshal(infoOriginal, out) }
            true
        }

        whenever(winrarService.insereComicInfo(eq(archiveFile), any())).thenReturn(true)
        whenever(winrarService.insereArquivo(eq(archiveFile), any<File>())).thenReturn(true)

        // Invoca onBtnGravarComicInfo via reflexão
        val method = controller.javaClass.getDeclaredMethod("onBtnGravarComicInfo")
        method.isAccessible = true
        method.invoke(controller)

        // Verifica que o ComicInfo salvo no serviço tem volume 5, 2 páginas e o sumário original preservado
        val comicCaptor = argumentCaptor<ComicInfo>()
        verify(comicInfoService).save(comicCaptor.capture(), any(), any(), any())

        val saved = comicCaptor.firstValue
        assertEquals(5, saved.volume)
        assertEquals(5f, saved.number)
        assertEquals(5, saved.count)
        assertEquals(2, saved.pageCount)
        assertEquals(2, saved.pages?.size)
        assertEquals("Capítulos 20 ao 25", saved.summary)
        assertEquals("Série Atualizada", saved.series)
        assertEquals("Nova Editora", saved.publisher)
        assertEquals(12345L, saved.idMal)

        verify(winrarService).insereComicInfo(eq(archiveFile), any())
        verify(winrarService).insereArquivo(eq(archiveFile), any<File>())
    }

    @Test
    fun `test onBtnGravarComicInfo usa fallback de txtVolume quando RAR nao possui ComicInfo previo`() {
        val archiveFile = File(tempFolder, "Manga_v05.cbr")
        archiveFile.createNewFile()

        // RAR sem ComicInfo.xml
        whenever(winrarService.listarConteudo(archiveFile)).thenReturn(listOf("001.jpg", "002.jpg"))
        whenever(winrarService.insereComicInfo(eq(archiveFile), any())).thenReturn(true)
        whenever(winrarService.insereArquivo(eq(archiveFile), any<File>())).thenReturn(true)

        // Invoca onBtnGravarComicInfo via reflexão
        val method = controller.javaClass.getDeclaredMethod("onBtnGravarComicInfo")
        method.isAccessible = true
        method.invoke(controller)

        val comicCaptor = argumentCaptor<ComicInfo>()
        verify(comicInfoService).save(comicCaptor.capture(), any(), any(), any())

        val saved = comicCaptor.firstValue
        // Volume derivado de "Volume 05"
        assertEquals(5, saved.volume)
        assertEquals(5f, saved.number)
        assertEquals(5, saved.count)
        assertEquals("Série Atualizada", saved.series)

        verify(winrarService).insereComicInfo(eq(archiveFile), any())
        verify(winrarService).insereArquivo(eq(archiveFile), any<File>())
    }

    @Test
    fun `test onBtnGravarComicInfo nao executa operacao se arquivo compactado nao existir`() {
        // Garante que o arquivo configurado não existe
        setField("txtNomeArquivo", JFXTextField("ArquivoInexistente_99.cbr"))

        val method = controller.javaClass.getDeclaredMethod("onBtnGravarComicInfo")
        method.isAccessible = true
        method.invoke(controller)

        verify(winrarService, never()).listarConteudo(any())
        verify(winrarService, never()).insereComicInfo(any(), any())
        verify(comicInfoService, never()).save(any(), any(), any(), any())
    }
}
