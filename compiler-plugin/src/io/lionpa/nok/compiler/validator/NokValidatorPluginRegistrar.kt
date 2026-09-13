package io.lionpa.nok.compiler.validator

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class NokValidatorPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirValidatorFunctionsGenerator
        +::FirValidatableGenerator
    }
}
