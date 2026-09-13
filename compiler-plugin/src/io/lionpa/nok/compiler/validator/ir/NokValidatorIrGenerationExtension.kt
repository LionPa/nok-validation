package io.lionpa.nok.compiler.validator.ir

import io.lionpa.nok.compiler.validator.IrValidatableGenerator
import io.lionpa.nok.compiler.validator.IrValidatorFunctionsGenerator
import io.lionpa.nok.compiler.validator.bindings.BindingsFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class NokValidatorIrGenerationExtension: IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        BindingsFinder.finder = pluginContext.finderForBuiltins()

        // DTO & Validation
        moduleFragment.acceptChildrenVoid(IrValidatorFunctionsGenerator(pluginContext))
        moduleFragment.acceptChildrenVoid(IrValidatableGenerator(pluginContext))
    }
}
