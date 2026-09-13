package io.lionpa.nok.validation.validator

import io.lionpa.nok.validation.Schema
import io.lionpa.nok.validation.Validator

@Validator
inline fun <T> Schema<List<T>>.forEach(crossinline block: Schema<T>.() -> Unit) {
    for (item in value) {
        block(sub(item))
    }
}