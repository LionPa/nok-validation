package io.lionpa.nok.validation.spring

import io.lionpa.nok.validation.Validatable
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class NokSpringValidator : Validator {

    override fun supports(clazz: Class<*>): Boolean {
        return clazz.isAnnotationPresent(Validatable::class.java) ||
                NokValidatorInvoker.getValidateHandle(clazz) != null
    }
    override fun validate(target: Any, errors: Errors) {
        val errorMessages = NokValidatorInvoker.invokeValidate(target)
        for (message in errorMessages) {

            errors.reject("nok.validation.error", message)
        }
    }
}