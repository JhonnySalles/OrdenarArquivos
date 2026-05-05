package com.fenix.ordenararquivos.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NaturalOrderComparatorTest {

    private val comparator = object : NaturalOrderComparator<String>() {
        override fun stringValue(o: String): String = o
    }

    @Test
    fun testNaturalSort() {
        val list = listOf("1", "10", "2", "20", "1.5", "1.10", "1.2")
        val sorted = list.sortedWith(comparator)
        
        // Natural order: 1, 1.2, 1.5, 1.10, 2, 10, 20
        assertEquals(listOf("1", "1.2", "1.5", "1.10", "2", "10", "20"), sorted)
    }

    @Test
    fun testVolumeSort() {
        val list = listOf("Volume 10", "Volume 1", "Volume 2", "Volume 1.5")
        val sorted = list.sortedWith(comparator)
        
        assertEquals(listOf("Volume 1", "Volume 1.5", "Volume 2", "Volume 10"), sorted)
    }

    @Test
    fun testLeadingZeros() {
        // nza - nzb logic
        // "1" (0 zeros) vs "01" (1 zero) -> 0 - 1 = -1 -> "1" < "01"
        assertTrue(comparator.compare("1", "01") < 0)
        assertTrue(comparator.compare("01", "001") < 0)
    }

    @Test
    fun testComplexStrings() {
        val list = listOf("pic 10.jpg", "pic 1.jpg", "pic 11.jpg", "pic 2.jpg")
        val sorted = list.sortedWith(comparator)
        assertEquals(listOf("pic 1.jpg", "pic 2.jpg", "pic 10.jpg", "pic 11.jpg"), sorted)
    }
}
