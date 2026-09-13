package io.lionpa.nok.validation

fun interface FieldValueMessage<T> {
    fun get(fieldName: String, value: T): String
}