package com.fenix.ordenararquivos.mock


abstract class MockBase<ID, E> : Mock<ID, E> {

    override fun mockEntity(): E = mockEntity(null)

    abstract fun randomId(): ID?

    override fun mockEntityList(): List<E> {
        val list: MutableList<E> = mutableListOf()
        for (i in 1..3)
            list.add(mockEntity(null))
        return list
    }

    override fun updateList(list: List<E>): List<E> {
        val updated: MutableList<E> = mutableListOf()
        for (item in list)
            updated.add(updateEntity(item))
        return updated
    }

}