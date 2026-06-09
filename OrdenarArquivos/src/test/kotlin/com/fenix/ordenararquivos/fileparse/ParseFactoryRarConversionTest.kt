package com.fenix.ordenararquivos.fileparse

import com.fenix.ordenararquivos.process.Winrar
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class ParseFactoryRarConversionTest {

    @TempDir
    lateinit var tempDir: Path

    private fun invokeTryParse(parse: Parse, file: File, tentarConversao: Boolean = true): Parse {
        val companionField = ParseFactory::class.java.getDeclaredField("Companion")
        companionField.isAccessible = true
        val companion = companionField.get(null)
        val method = companion.javaClass.getDeclaredMethod(
            "tryParse",
            Parse::class.java,
            File::class.java,
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        return try {
            @Suppress("UNCHECKED_CAST")
            method.invoke(companion, parse, file, tentarConversao) as Parse
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    @Test
    fun testConversaoFalhaPropagaExcecaoOriginal() {
        val rarFile = File(tempDir.toFile(), "test.rar")
        rarFile.createNewFile()
        val parse = mock<RarParse>()
        whenever(parse.parse(eq(rarFile))).thenThrow(
            IOException("unsupported rar v5", UnsupportedRarV5Exception())
        )

        mockStatic(Winrar::class.java).use { winrarMock ->
            winrarMock.`when`<Boolean> { Winrar.converterRar5ParaRar4(eq(rarFile)) }.thenReturn(false)

            val ex = assertThrows(Exception::class.java) {
                invokeTryParse(parse, rarFile)
            }

            assertEquals("Não foi possível abrir o arquivo", ex.message)
            assertTrue(ex.cause is IOException)
            assertTrue(ex.cause?.cause is UnsupportedRarV5Exception)
            verify(parse, times(1)).parse(rarFile)
            winrarMock.verify { Winrar.converterRar5ParaRar4(eq(rarFile)) }
        }
    }

    @Test
    fun testConversaoSucessoRetentaParseUmaVez() {
        val rarFile = File(tempDir.toFile(), "test.rar")
        rarFile.createNewFile()
        val parse = mock<RarParse>()
        whenever(parse.parse(eq(rarFile)))
            .thenThrow(IOException("unsupported rar v5", UnsupportedRarV5Exception()))
            .thenAnswer { }

        mockStatic(Winrar::class.java).use { winrarMock ->
            winrarMock.`when`<Boolean> { Winrar.converterRar5ParaRar4(eq(rarFile)) }.thenReturn(true)

            val result = invokeTryParse(parse, rarFile)

            assertSame(parse, result)
            verify(parse, times(2)).parse(rarFile)
            winrarMock.verify { Winrar.converterRar5ParaRar4(eq(rarFile)) }
        }
    }

    @Test
    fun testErroNaoRarV5NaoTentaConversao() {
        val rarFile = File(tempDir.toFile(), "test.rar")
        rarFile.createNewFile()
        val parse = mock<RarParse>()
        whenever(parse.parse(eq(rarFile))).thenThrow(
            IOException("unable to open archive", IOException("corrupt"))
        )

        mockStatic(Winrar::class.java).use { winrarMock ->
            val ex = assertThrows(Exception::class.java) {
                invokeTryParse(parse, rarFile)
            }

            assertEquals("Não foi possível abrir o arquivo", ex.message)
            winrarMock.verifyNoInteractions()
        }
    }
}
