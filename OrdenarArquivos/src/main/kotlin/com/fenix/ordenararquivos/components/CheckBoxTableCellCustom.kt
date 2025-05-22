package com.fenix.ordenararquivos.components

import com.jfoenix.controls.JFXCheckBox
import javafx.beans.binding.Bindings
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.util.Callback
import javafx.util.StringConverter


// Reimplementado a classe apenas para trocar o checkbox pelo JFoenix.
class CheckBoxTableCellCustom<S, T> @JvmOverloads constructor(getSelectedProperty: Callback<Int, ObservableValue<Boolean>>? = null, converter: StringConverter<T>? = null) :
    TableCell<S, T>() {

    private val checkBox: JFXCheckBox
    private var showLabel = false
    private var booleanProperty: ObservableValue<Boolean>? = null

    private val converter: ObjectProperty<StringConverter<T>?> = object : SimpleObjectProperty<StringConverter<T>?>(this, "converter") {
        override fun invalidated() {
            updateShowLabel()
        }
    }

    fun converterProperty(): ObjectProperty<StringConverter<T>?> {
        return converter
    }

    fun setConverter(value: StringConverter<T>?) {
        converterProperty().set(value)
    }

    fun getConverter(): StringConverter<T>? {
        return converterProperty().get()
    }

    private val selectedStateCallback: ObjectProperty<Callback<Int, ObservableValue<Boolean>>?> = SimpleObjectProperty<Callback<Int, ObservableValue<Boolean>>?>(
        this, "selectedStateCallback"
    )

    init {
        this.styleClass.add("check-box-tree-table-cell")
        checkBox = JFXCheckBox()
        graphic = null
        setSelectedStateCallback(getSelectedProperty)
        setConverter(converter)
    }

    fun selectedStateCallbackProperty(): ObjectProperty<Callback<Int, ObservableValue<Boolean>>?> {
        return selectedStateCallback
    }

    fun setSelectedStateCallback(value: Callback<Int, ObservableValue<Boolean>>?) {
        selectedStateCallbackProperty().set(value)
    }

    fun getSelectedStateCallback(): Callback<Int, ObservableValue<Boolean>>? {
        return selectedStateCallbackProperty().get()
    }

    @SuppressWarnings("unchecked")
    @Override
    override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            text = null
            setGraphic(null)
        } else {
            val c = getConverter()
            if (showLabel && c != null) {
                text = c.toString(item)
            }
            graphic = checkBox
            if (booleanProperty is BooleanProperty) {
                checkBox.selectedProperty().unbindBidirectional(booleanProperty as BooleanProperty)
            }
            val obsValue: ObservableValue<*> = selectedProperty
            if (obsValue is BooleanProperty) {
                booleanProperty = obsValue
                checkBox.selectedProperty().bindBidirectional(booleanProperty as BooleanProperty)
            }
            checkBox.disableProperty().bind(
                Bindings.not(
                    tableView.editableProperty().and(
                        tableColumn.editableProperty()
                    ).and(
                        editableProperty()
                    )
                )
            )
        }
    }

    private fun updateShowLabel() {
        showLabel = converter != null
        checkBox.alignment = if (showLabel) Pos.CENTER_LEFT else Pos.CENTER
    }

    private val selectedProperty: ObservableValue<*>
        private get() = if (getSelectedStateCallback() != null) getSelectedStateCallback()!!.call(index) else tableColumn.getCellObservableValue(index)

    companion object {
        fun <S> forTableColumn(
            column: TreeTableColumn<S, Boolean>,
        ): Callback<TreeTableColumn<S, Boolean>, TableCell<S, Boolean>> {
            return forTableColumn(null, null)
        }

        fun <S, T> forTableColumn(
            getSelectedProperty: Callback<Int, ObservableValue<Boolean>>,
        ): Callback<TreeTableColumn<S, T>, TableCell<S, T>> {
            return forTableColumn(getSelectedProperty, null)
        }

        fun <S, T> forTableColumn(getSelectedProperty: Callback<Int, ObservableValue<Boolean>>?, converter: StringConverter<T>?): Callback<TreeTableColumn<S, T>, TableCell<S, T>> {
            return Callback<TreeTableColumn<S, T>, TableCell<S, T>> { CheckBoxTableCell(getSelectedProperty, converter) }
        }
    }
}