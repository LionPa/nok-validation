package io.lionpa.nok.validation.spring

import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.InitBinder

@ControllerAdvice
open class NokDataBinderAdvice(private val validator: NokSpringValidator) {

    @InitBinder
    open fun initBinder(binder: WebDataBinder) {
        binder.addValidators(validator)
    }
}