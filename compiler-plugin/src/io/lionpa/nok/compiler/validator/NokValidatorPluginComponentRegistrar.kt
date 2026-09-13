package io.lionpa.nok.compiler.validator

import io.lionpa.nok.compiler.validator.ir.NokValidatorIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class NokValidatorPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true


    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(NokValidatorPluginRegistrar())
        IrGenerationExtension.registerExtension(NokValidatorIrGenerationExtension())
    }
}
