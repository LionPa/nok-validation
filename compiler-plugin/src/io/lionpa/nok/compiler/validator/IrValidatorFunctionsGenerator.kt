package io.lionpa.nok.compiler.validator

import io.lionpa.nok.compiler.validator.util.generateIrFunctionUniqueId
import io.lionpa.nok.compiler.validator.util.ifGenerated
import io.lionpa.nok.compiler.validator.util.replaceIllegalSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class IrValidatorFunctionsGenerator(val context: IrPluginContext) : IrVisitorVoid() {

    private class DefaultOffsetsTransformer(
        private val start: Int,
        private val end: Int
    ) : IrElementTransformerVoid() {
        override fun visitElement(element: IrElement): IrElement {
            if (element.startOffset == UNDEFINED_OFFSET && start != UNDEFINED_OFFSET) {
                element.startOffset = start
            }
            if (element.endOffset == UNDEFINED_OFFSET && end != UNDEFINED_OFFSET) {
                element.endOffset = end
            }
            return super.visitElement(element)
        }
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)

            else -> {}
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.body != null) return

        declaration.ifGenerated<FirValidatorFunctionsGenerator.ValidatorFunctionKey> { key ->
            val finder = context.finderForBuiltins()

            val originalFunctions = finder
                .findFunctions(key.validatorFunction.originalValidator.callableId)
                .filter { replaceIllegalSymbols(it.owner.generateIrFunctionUniqueId()) == key.validatorFunction.originalUQID }
                .filter { it.owner.startOffset != -1 } // TODO Подумать может поадекватнее сделать

            if (originalFunctions.size != 1) {
                throw Exception("Compilation error. More then one Validator TODO Normal message ${originalFunctions.joinToString("\n") { it.owner.generateIrFunctionUniqueId() }}") // TODO Normal message
            }

            val original = originalFunctions.firstOrNull()!!

            val file = declaration.fileOrNull
            val fileEntry = file?.fileEntry
            val useSyntheticOffsets = fileEntry?.lineStartOffsets?.isEmpty() == true

            if (useSyntheticOffsets) {
                file.fileEntry = NaiveSourceBasedFileEntryImpl(
                    name = fileEntry.name,
                    lineStartOffsets = intArrayOf(0),
                    maxOffset = 0,
                    firstRelevantLineIndex = 0
                )
            }

            if (declaration.startOffset == UNDEFINED_OFFSET) {
                declaration.startOffset = if (useSyntheticOffsets) SYNTHETIC_OFFSET else original.owner.startOffset
            }
            if (declaration.endOffset == UNDEFINED_OFFSET) {
                declaration.endOffset = if (useSyntheticOffsets) SYNTHETIC_OFFSET else original.owner.endOffset
            }

            val defaultStart = if (useSyntheticOffsets) SYNTHETIC_OFFSET else original.owner.startOffset
            val defaultEnd = if (useSyntheticOffsets) SYNTHETIC_OFFSET else original.owner.endOffset

            val copiedBody = original.owner.body!!.deepCopyWithSymbols(declaration)
                .transform(IrBodyVarBinding(original.owner, declaration, context.irBuiltIns), null)
                .transform(DefaultOffsetsTransformer(defaultStart, defaultEnd), null)
                .patchDeclarationParents(declaration)

            declaration.body = copiedBody
        }
    }


}