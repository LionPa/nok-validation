package io.lionpa.nok.validation

class Schema<T>(var value: T) {
    val fieldName : String = ""
    var invalidMessages : ValidationResultList = ValidationResultList()

    fun <C> sub(value: C): Schema<C> {
        return Schema(value)
    }
}

@Suppress("unchecked_cast")
fun <T> schema(block: Schema<T>.() -> Unit): T {
    return null as T
}