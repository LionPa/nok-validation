package io.lionpa.nok.validation.benchmark.jakarta

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCatDto(
    @field:NotBlank
    @field:Size(min = 3, max = 10)
    @field:Contains("meow")
    val name: String,

    @field:Valid
    val favoriteToys: List<@Contains("42") String>
)