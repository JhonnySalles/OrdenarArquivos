package com.fenix.ordenararquivos.mock


interface Mock<ID, E> {
    fun mockEntity(): E
    fun mockEntityList(): List<E>
    fun mockEntity(id: ID?): E
    fun updateEntityById(lastId: ID?): E
    fun updateEntity(input: E): E
    fun updateList(list: List<E>): List<E>
    fun assertsService(input: E?)
    fun assertsService(oldObj: E?, newObj: E?)
}