package io.lionpa.nok.validation.benchmark.konform

import io.konform.validation.Validation
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.minLength

data class CreateCatDto(
    val name: String,
    val favoriteToys: List<String>
)

object CreateCatDtoValidation {

    val validation = Validation {

        CreateCatDto::name {
            constrain("name must not be blank") {
                it.isNotBlank()
            }

            minLength(3)
            maxLength(10)

            constrain("name must contain meow") {
                it.contains("meow")
            }
        }

        CreateCatDto::favoriteToys onEach {
            constrain("favoriteToy must contain 42") {
                it.contains("42")
            }
        }
    }
}