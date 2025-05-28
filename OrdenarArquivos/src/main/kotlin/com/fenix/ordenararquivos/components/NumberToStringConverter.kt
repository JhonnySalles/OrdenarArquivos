package com.fenix.ordenararquivos.components

import javafx.util.converter.NumberStringConverter
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class NumberToStringConverter : NumberStringConverter {

    constructor() : super() {}
    constructor(locale: Locale, pattern: String) : super(locale, pattern) {}
    constructor(locale: Locale) : super(locale) {}
    constructor(numberFormat: NumberFormat) : super(numberFormat) {}
    constructor(pattern: String) : super(pattern) {}

    @Override
    override fun fromString(value: String): Number {
        //to transform the double, given by the textfield, just multiply by 100 and round if any left
        val rValue: Number = Math.round(super.fromString(value).toDouble() * 100)
        return rValue.toLong()
    }

    @Override
    override fun toString(value: Number?): String {
        if (value == null)
            return ""

        //Check for too big long value
        //If the long is too big, it could result in a strange double value.
        if (value.toLong() > 1000000000000L || value.toLong() < -1000000000000L) {
            return ""
        }
        var myBigDecimal = BigDecimal(value.toLong())
        //to convert the long to a double (currency with fractional digits)
        myBigDecimal = myBigDecimal.movePointLeft(2)
        val asDouble: Double = myBigDecimal.toDouble()
        return if (asDouble == Double.NEGATIVE_INFINITY || asDouble == Double.POSITIVE_INFINITY) {
            ""
        } else
            super.toString(asDouble)
    }
}