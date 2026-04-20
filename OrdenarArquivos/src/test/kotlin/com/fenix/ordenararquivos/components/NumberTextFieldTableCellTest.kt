package com.fenix.ordenararquivos.components

import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.util.WaitForAsyncUtils

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class NumberTextFieldTableCellTest {

    @Test
    fun testRegexFilterValidInputs(robot: FxRobot) {
        val textField = TextField()
        val filter = NumberTextFieldTableCell.createFilter()
        textField.textFormatter = TextFormatter<String>(filter)
        
        val validInputs = listOf("123", "123.45", "-123", "0.5")
        
        for (input in validInputs) {
            robot.interact {
                textField.text = ""
                textField.text = input
            }
            WaitForAsyncUtils.waitForFxEvents()
            assertEquals(input, textField.text, "Input '$input' deveria ser aceito pelo filtro.")
        }
    }

    @Test
    fun testRegexFilterInvalidInputs(robot: FxRobot) {
        val textField = TextField()
        val filter = NumberTextFieldTableCell.createFilter()
        textField.textFormatter = TextFormatter<String>(filter)
        
        val invalidInputs = listOf("abc", "12a3", "@")
        
        for (input in invalidInputs) {
            robot.interact {
                textField.text = ""
                // Tentando digitar texto inválido
                textField.replaceText(0, 0, input)
            }
            WaitForAsyncUtils.waitForFxEvents()
            // Se o filtro rejeitou, o texto deve continuar vazio (ou sem a parte inválida)
            assertNotEquals(input, textField.text, "Input '$input' deveria ser rejeitado pelo filtro.")
        }
    }

    @Test
    fun testEmptyInputAcceptance(robot: FxRobot) {
        val textField = TextField("initial")
        val filter = NumberTextFieldTableCell.createFilter()
        textField.textFormatter = TextFormatter<String>(filter)
        
        robot.interact {
            textField.text = ""
        }
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals("", textField.text, "Input vazio deveria ser aceito pelo filtro.")
    }
}
