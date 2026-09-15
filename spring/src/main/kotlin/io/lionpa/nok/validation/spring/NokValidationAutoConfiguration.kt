package io.lionpa.nok.validation.spring

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean

@AutoConfiguration
open class NokValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun nokSpringValidator(): NokSpringValidator = NokSpringValidator()

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    open fun nokDataBinderAdvice(nokSpringValidator: NokSpringValidator): NokDataBinderAdvice {
        return NokDataBinderAdvice(nokSpringValidator)
    }
}