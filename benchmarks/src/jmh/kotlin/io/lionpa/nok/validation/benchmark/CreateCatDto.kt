package io.lionpa.nok.validation.benchmark

import io.lionpa.nok.validation.*
import io.lionpa.nok.validation.validator.*

@Validatable
data class CreateCatDto(
    val name: String = schema {
        isNotBlank()
        length(3, 10)
        contains("meow")
    },
    val favoriteToys: List<String> = schema {
        forEach {
            contains("42")
        }
    }
)