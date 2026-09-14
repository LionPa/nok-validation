package io.lionpa.nok.validation.benchmark.jakarta

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ContainsValidator::class])
annotation class Contains(
    val value: String,
    val message: String = "must contain required value",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ContainsValidator : ConstraintValidator<Contains, String> {

    private lateinit var expected: String

    override fun initialize(annotation: Contains) {
        expected = annotation.value
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        return value == null || value.contains(expected)
    }
}