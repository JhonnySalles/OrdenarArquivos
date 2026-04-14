package com.fenix.ordenararquivos.components

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.TableCell
import javafx.util.Callback
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testfx.api.FxRobot
import org.testfx.framework.junit5.ApplicationExtension
import org.testfx.util.WaitForAsyncUtils
import com.jfoenix.controls.JFXCheckBox
import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.testfx.framework.junit5.Start

@Tag("UI")
@ExtendWith(ApplicationExtension::class)
class CheckBoxTableCellCustomTest {

    private lateinit var tableView: TableView<Person>
    private val data = Person(true)

    class Person(selected: Boolean) {
        val selectedProperty = SimpleBooleanProperty(selected)
        var isSelected: Boolean
            get() = selectedProperty.get()
            set(value) = selectedProperty.set(value)
    }

    @Start
    fun start(stage: Stage) {
        tableView = TableView<Person>()
        tableView.isEditable = true
        
        val column = TableColumn<Person, Boolean>("Selected")
        column.isEditable = true
        column.cellValueFactory = Callback { it.value.selectedProperty }
        
        // Usando o componente customizado
        column.cellFactory = Callback { 
            CheckBoxTableCellCustom<Person, Boolean>(Callback { index -> 
                tableView.items[index].selectedProperty 
            })
        }
        
        tableView.columns.add(column)
        tableView.items.add(data)
        
        stage.scene = Scene(VBox(tableView), 200.0, 200.0)
        stage.show()
    }

    @Test
    fun testBidirectionalBinding(robot: FxRobot) {
        val checkBox = robot.lookup(".check-box").queryAs(JFXCheckBox::class.java)
        
        // Verifica estado inicial
        assertTrue(checkBox.isSelected, "CheckBox deveria estar selecionado inicialmente")
        
        // Simula clique na UI
        robot.clickOn(checkBox)
        WaitForAsyncUtils.waitForFxEvents()
        
        // Verifica se o modelo foi atualizado
        assertFalse(data.isSelected, "O modelo deveria ter sido atualizado para falso após o clique")
        
        // Simula alteração programática no modelo
        robot.interact {
            data.isSelected = true
        }
        WaitForAsyncUtils.waitForFxEvents()
        
        // Verifica se a UI refletiu
        assertTrue(checkBox.isSelected, "A UI deveria ter refletido a mudança no modelo")
    }
}
