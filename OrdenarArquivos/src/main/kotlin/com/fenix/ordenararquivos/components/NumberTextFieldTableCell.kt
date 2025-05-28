package com.fenix.ordenararquivos.components

import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.util.Callback
import javafx.util.converter.NumberStringConverter
import java.util.*
import java.util.function.UnaryOperator

class NumberTextFieldTableCell<S> private constructor(converter: NumberStringConverter) : TableCell<S, Number?>() {
    /***************************************************************************
     * * Fields * *
     */
    private val hBox: HBox = HBox()
    private val currencyLabel: Label = Label("")
    private val textField: TextField = TextField("" + 0L)

    /***************************************************************************
     * * Constructors * *
     */
    constructor() : this(NumberToStringConverter(Locale.US, "0.##")) {}

    /***************************************************************************
     * * Properties * *
     */
    private val converter: NumberStringConverter

    init {
        this.styleClass.add("currency-text-field-table-cell")
        this.converter = converter
        setupTextField()
        setupHBox()
        style = "-fx-alignment: CENTER-RIGHT;"
    }

    /** {@inheritDoc}  */
    @Override
    override fun startEdit() {
        if (!isEditable || !tableView.isEditable || !tableColumn.isEditable)
            return

        super.startEdit()
        if (isEditing) {
            this.text = null
            if (hBox != null) {
                this.setGraphic(hBox)
            } else {
                this.setGraphic(textField)
            }
            if (textField != null) {
                textField.text = itemText
                textField.selectAll()
                // requesting focus so that key input can immediately go into the
                // TextField (see RT-28132)
                textField.requestFocus()
            }
        }
    }

    /** {@inheritDoc}  */
    @Override
    override fun cancelEdit() {
        super.cancelEdit()
        this.text = itemText
        this.graphic = currencyLabel
        contentDisplayProperty().value = ContentDisplay.RIGHT
    }

    /** {@inheritDoc}  */
    @Override
    override fun updateItem(item: Number?, empty: Boolean) {
        super.updateItem(item, empty)
        if (isEmpty) {
            text = null
            setGraphic(null)
        } else {
            if (isEditing) {
                if (textField != null)
                    textField.text = itemText

                text = null
                setGraphic(hBox)
            } else {
                text = itemText
                graphic = currencyLabel
                contentDisplayProperty().setValue(ContentDisplay.RIGHT)
            }
        }
    }

    private fun setupTextField() {
        val textFormatter: TextFormatter<Number?> = TextFormatter(createFilter())
        textField.textFormatter = textFormatter
        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction { event ->
            if (converter == null) {
                throw IllegalStateException(
                    "Attempting to convert text input into Object, but provided "
                            + "StringConverter is null. Be sure to set a StringConverter " + "in your cell factory."
                )
            }
            if (textField.text != null)
                commitEdit(converter.fromString(textField.text))
            event.consume()
        }
        textField.setOnKeyReleased { t ->
            if (t.code === KeyCode.ESCAPE) {
                cancelEdit()
                t.consume()
            }
        }
    }

    private fun setupHBox() {
        hBox.children.add(textField)
        hBox.children.add(Label(" €"))
        hBox.padding = Insets(hBox.padding.top + 9.0, hBox.padding.right, hBox.padding.bottom, hBox.padding.left)
    }

    private val itemText: String
        private get() = if (converter == null) {
            if (item == null) "" else item.toString()
        } else {
            converter.toString(item)
        }

    companion object {
        fun <S> forTableColumn(): Callback<TableColumn<S, Number?>, TableCell<S, Number?>> {
            return forTableColumn(NumberStringConverter())
        }

        fun <S> forTableColumn(converter: NumberStringConverter): Callback<TableColumn<S, Number?>, TableCell<S, Number?>> {
            return Callback<TableColumn<S, Number?>, TableCell<S, Number?>> { NumberTextFieldTableCell(converter) }
        }

        // This will filter the changes
        fun createFilter(): UnaryOperator<TextFormatter.Change?> {
            //this is a simple Regex to define the acceptable Chars
            val validEditingStateRegex = "[0123456789,.-]*".toRegex()
            return UnaryOperator { change: TextFormatter.Change? ->
                val text = change!!.text
                //Check if something changed and just return if not
                if (!change.isContentChange)
                    change
                //check if the changed text validates against the regex
                else if (text.matches(validEditingStateRegex) || text.isEmpty()) //if valid return the change
                    change
                else
                    null
            }
        }
    }
}