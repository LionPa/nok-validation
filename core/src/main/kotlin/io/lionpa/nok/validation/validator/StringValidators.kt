@file:Suppress("NOTHING_TO_INLINE")

package io.lionpa.nok.validation.validator

import io.lionpa.nok.validation.FieldValueMessage
import io.lionpa.nok.validation.Schema
import io.lionpa.nok.validation.Validator

@Validator
inline fun Schema<String>.length(min: Int, max: Int) {
    if (value.length !in min..max) {
        invalidMessages.add("$fieldName length $value dont falls in range $min..$max")
    }
}

@Validator
inline fun Schema<String>.length(range: IntRange) {
    if (!range.contains(value.length)) {
        invalidMessages.add("$fieldName length $value dont falls in range ${range.first}..${range.last}")
    }
}

@Validator
inline fun Schema<String>.isNotBlank() {
    if (value.isBlank()) {
        invalidMessages.add("$fieldName is blank!")
    }
}

@Validator
inline fun Schema<String>.contains(string: String) {
    if (!value.contains(string)) {
        invalidMessages.add("$fieldName $value dont contains $string!")
    }
}

@Validator
inline fun Schema<String>.contains(string: String, block: FieldValueMessage<String>) {
    if (!value.contains(string)) {
        invalidMessages.add(block.get(fieldName, value))
    }
}

@Validator
inline fun <T> Schema<T>.isNotNull() {
    if (value == null) {
        invalidMessages.add("$fieldName is null!")
    }
}

@Validator
inline fun Schema<String>.regex(regex: Regex) {
    if (!regex.matches(value)) {
        invalidMessages.add("$fieldName $value dont match required pattern!")
    }
}