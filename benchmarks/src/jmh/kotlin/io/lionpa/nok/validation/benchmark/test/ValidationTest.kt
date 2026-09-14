package io.lionpa.nok.validation.benchmark.test

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.lionpa.nok.validation.benchmark.jakarta.CreateCatDto
import io.lionpa.nok.validation.benchmark.konform.CreateCatDtoValidation
import jakarta.validation.Validation
import jakarta.validation.Validator
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.collections.List
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationCompatibilityTest {

    companion object {
        @JvmStatic
        val VALIDATE_HANDLE: MethodHandle =
            MethodHandles.lookup()
                .unreflect(
                    io.lionpa.nok.validation.benchmark.CreateCatDto::class.java.getMethod("generated\$validate")
                )
    }

    private val jakartaValidator: Validator =
        Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `all validators produce same result`() {
        cases().forEach { case ->
            val expected = referenceValidation(case.name, case.favoriteToys)

            val generated = generatedValidation(case)
            val jakarta = jakartaValidation(case)
            val konform = konformValidation(case)

            assertEquals(
                expected,
                generated,
                "Generated validator failed for $case"
            )

            assertEquals(
                expected,
                jakarta,
                "Jakarta validator failed for $case"
            )

            assertEquals(
                expected,
                konform,
                "Konform validator failed for $case"
            )
        }
    }

    private fun generatedValidation(case: Case): Int {
        val dto = io.lionpa.nok.validation.benchmark.CreateCatDto(
            name = case.name,
            favoriteToys = case.favoriteToys
        )

        val list = VALIDATE_HANDLE.invokeExact(dto) as List<*>

        return list.size
    }

    private fun jakartaValidation(case: Case): Int {
        val dto = CreateCatDto(
            name = case.name,
            favoriteToys = case.favoriteToys
        )

        return jakartaValidator.validate(dto).size
    }

    private fun konformValidation(case: Case): Int {
        val dto = io.lionpa.nok.validation.benchmark.konform.CreateCatDto(
            name = case.name,
            favoriteToys = case.favoriteToys
        )

        val result = CreateCatDtoValidation.validation(dto)

        return when (result) {
            is Valid -> 0
            is Invalid -> result.errors.size
        }
    }

    /**
     * This is the source of truth.
     *
     * It must contain the same rules as the original generated validator,
     * but must not use any of the validation libraries.
     */
    private fun referenceValidation(
        name: String,
        favoriteToys: List<String>
    ): Int {
        var errors = 0

        if (name.isBlank()) {
            errors++
        }

        if (name.length !in 3..10) {
            errors++
        }

        if (!name.contains("meow")) {
            errors++
        }

        favoriteToys.forEach { toy ->
            if (!toy.contains("42")) {
                errors++
            }
        }

        return errors
    }

    private fun cases(): List<Case> =
        listOf(
            // Fully valid
            Case(
                name = "meow",
                favoriteToys = listOf("toy42")
            ),

            // Name: blank
            Case(
                name = "",
                favoriteToys = emptyList()
            ),

            Case(
                name = "   ",
                favoriteToys = emptyList()
            ),

            // Name: too short
            Case(
                name = "me",
                favoriteToys = listOf("toy42")
            ),

            // Name: too long
            Case(
                name = "verylongcatname",
                favoriteToys = listOf("toy42")
            ),

            // Name: does not contain "meow"
            Case(
                name = "cat",
                favoriteToys = listOf("toy42")
            ),

            // Multiple name violations
            Case(
                name = "ab",
                favoriteToys = listOf("toy42")
            ),

            // Invalid toy
            Case(
                name = "meow",
                favoriteToys = listOf("toy")
            ),

            // Multiple invalid toys
            Case(
                name = "meow",
                favoriteToys = listOf(
                    "toy",
                    "ball",
                    "mouse42"
                )
            ),

            // Everything invalid
            Case(
                name = "ab",
                favoriteToys = listOf(
                    "toy",
                    "ball"
                )
            ),

            // Boundary: min length
            Case(
                name = "meow",
                favoriteToys = emptyList()
            ),

            // Boundary: max length
            Case(
                name = "meow123456",
                favoriteToys = listOf("42")
            ),

            // Contains "meow" in different positions
            Case(
                name = "meowcat",
                favoriteToys = listOf("42")
            ),

            Case(
                name = "catmeow",
                favoriteToys = listOf("42")
            ),

            Case(
                name = "xmeowx",
                favoriteToys = listOf("42")
            )
        )

    private data class Case(
        val name: String,
        val favoriteToys: List<String>
    )
}