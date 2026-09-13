package io.lionpa.nok.compiler.validator

import io.lionpa.nok.compiler.validator.IrValidatableGenerator.Companion.VALIDATOR
import io.lionpa.nok.compiler.validator.bindings.ValidationResultList
import io.lionpa.nok.compiler.validator.util.generateIrFunctionUniqueId
import io.lionpa.nok.compiler.validator.util.ifGenerated
import io.lionpa.nok.compiler.validator.util.replaceIllegalSymbols
import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class IrValidatableGenerator(val context: IrPluginContext) : IrVisitorVoid() {

    companion object {
        val VALIDATABLE = FqName("io.lionpa.nok.validation.Validatable")

        val VALIDATOR = FqName("io.lionpa.nok.validation.Validator")
    }

    data class SchemaBlock(val fieldName: String, val field: IrField, val funExpr: IrFunctionExpression)

    val schemas = hashMapOf<IrClass, MutableList<SchemaBlock>>()

    val finder = context.finderForBuiltins()

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

        declaration.ifGenerated<FirValidatableGenerator.ClassValidationFunctionKey> {
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                this.generateValidateFunction(declaration)
            }
        }
    }

    fun IrBlockBodyBuilder.generateValidateFunction(declaration: IrSimpleFunction){
        val schemaBlocks = schemas[declaration.parentAsClass] ?: emptyList()

        val stringType = context.irBuiltIns.stringType

        val createValidationResultListCall = irCallConstructor(
            ValidationResultList.constructor,
            listOf(stringType)
        )

        val invalidMessagesVar = irTemporary(
            value = createValidationResultListCall,
            nameHint = "invalidMessages"
        )

        val paramRewriter = ConstructorParameterRewriter(declaration, this)

        for (block in schemaBlocks) {
            val originalBody = block.funExpr.function.body as? IrBlockBody ?: continue

            originalBody.patchDeclarationParents(declaration)
            originalBody.transformChildrenVoid(paramRewriter)

            val rootRewriter = ValidatorRewriter(
                context,
                finder,
                { irString(block.fieldName) },
                { irGetField(irGet(declaration.dispatchReceiverParameter!!), block.field) },
                { irGet(invalidMessagesVar) }
            )

            originalBody.transformChildrenVoid(rootRewriter)

            for (statement in originalBody.statements) {
                +statement // Добавление модифицированных DSL блоков в генерируемую validate функцию
            }
        }

        +irReturn(
            irCall(ValidationResultList.list).apply { arguments[0] = irGet(invalidMessagesVar)  }
        )
    }


    // У Validatable, собираются все поля в которых вызывается "schema".
    // Собранная инфоромация используется в visitSimpleFunction
    override fun visitConstructor(declaration: IrConstructor) {
        val clazz = declaration.parentAsClass
        if (!clazz.hasAnnotation(VALIDATABLE)) return

        for (parameter in declaration.parameters) {
            if (parameter.defaultValue == null || parameter.defaultValue!!.expression !is IrCall) continue

            val call = parameter.defaultValue!!.expression as IrCall

            if (call.symbol.owner.name.identifier != "schema") continue

            parameter.defaultValue = null
            val name = parameter.name.asString()
            val schemaField = clazz.properties.firstOrNull { it.name.identifier == parameter.name.identifier }!!.backingField!!
            val dslBlock = call.arguments[0]!! as IrFunctionExpression

            schemas.getOrPut(clazz) { mutableListOf() }.add(
                SchemaBlock(
                    name,
                    schemaField,
                    dslBlock
                )
            )
        }

    }
}

class ConstructorParameterRewriter(val declaration: IrSimpleFunction, val builder: IrBlockBodyBuilder) : IrElementTransformerVoid() {

    // Поддержка Cross-field доступа. TODO Переписать, текущая реализация выглядит сомнительно
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val owner = expression.symbol.owner
        if (owner !is IrValueParameter) return super.visitGetValue(expression)

        val parent = owner.parent

        if (parent !is IrConstructor) return super.visitGetValue(expression)
        if (parent.parentAsClass != declaration.parentAsClass) return super.visitGetValue(expression)

        val property = declaration.parentAsClass.properties.firstOrNull { it.name == owner.name }
        val backingField = property?.backingField ?: return super.visitGetValue(expression)

        return builder.irGetField(
            builder.irGet(declaration.dispatchReceiverParameter!!),
            backingField
        ).apply {
            startOffset = expression.startOffset
            endOffset = expression.endOffset
        }
    }
}

class ValidatorRewriter(
    private val context: IrGeneratorContext,
    private val finder: DeclarationFinder,
    private val getFieldName: () -> IrExpression,
    private val getValue: () -> IrExpression,
    private val getInvalidMessages: () -> IrExpression
) : IrElementTransformerVoid() {

    private fun IrType.functionArityOrNull(): Int? {
        val fqName = classFqName?.asString() ?: return null
        val match = Regex(""".*Function(\d+)""").matchEntire(fqName) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        if (!function.hasAnnotation(VALIDATOR)) return super.visitCall(expression)

        val generatedFunctionName =
            replaceIllegalSymbols("${function.callableId.callableName}\$GeneratedValidator\$${function.generateIrFunctionUniqueId()}") // TODO To utility
        val callableId = function.callableId.copy(Name.identifier(generatedFunctionName))
        val validatorFunction = finder.findFunctions(callableId).firstOrNull() ?: return super.visitCall(expression)

        val callBuilder = DeclarationIrBuilder(context, expression.symbol)
        val newCall = callBuilder.irCall(validatorFunction).apply {
            arguments[0] = getFieldName()
            arguments[1] = getValue()
            arguments[2] = getInvalidMessages()

            validatorCallArguments(expression, validatorFunction, this)
        }

        return newCall
    }

    private fun validatorCallArguments(expression: IrCall, f: IrSimpleFunctionSymbol, newCall: IrCall) {

        val parameters = expression.symbol.owner.parameters

        for (i in parameters.indices) {
            val param = parameters[i]
            val argIndex = i + 2

            if (param.kind != IrParameterKind.Regular && param.kind != IrParameterKind.Context) continue

            val arg = expression.arguments[i] ?: continue

            if (arg is IrFunctionExpression) {
                val expectedArity = f.owner.parameters.getOrNull(argIndex)?.type?.functionArityOrNull()
                val originalArity = arg.type.functionArityOrNull()

                if (expectedArity != null && originalArity != null && expectedArity == originalArity + 2) {
                    newCall.arguments[argIndex] = transformSchemaLambda(arg)
                    continue
                }
            }

            newCall.arguments[argIndex] = arg.transform(this@ValidatorRewriter, null)
        }
    }

    private fun transformSchemaLambda(arg: IrFunctionExpression): IrFunctionExpression {
        val lambdaFun = arg.function

        val newFieldName = buildValueParameter(lambdaFun) {
            name = Name.identifier("fieldName")
            type = context.irBuiltIns.stringType
            kind = IrParameterKind.Regular
        }
        val newValue = buildValueParameter(lambdaFun) {
            name = Name.identifier("value")
            type = context.irBuiltIns.anyNType
            kind = IrParameterKind.Regular
        }
        val newInvalidMessages = buildValueParameter(lambdaFun) {
            name = Name.identifier("invalidMessages")
            type = ValidationResultList.clazz.owner.defaultType
            kind = IrParameterKind.Regular
        }

        lambdaFun.parameters = listOf(newFieldName, newValue, newInvalidMessages)

        arg.type = context.irBuiltIns.functionN(3).typeWith(
            context.irBuiltIns.stringType,
            context.irBuiltIns.anyNType,
            ValidationResultList.clazz.owner.defaultType,
            context.irBuiltIns.unitType
        )

        val innerRewriter = ValidatorRewriter(
            context,
            finder,
            { DeclarationIrBuilder(context, lambdaFun.symbol).irGet(newFieldName) },
            { DeclarationIrBuilder(context, lambdaFun.symbol).irGet(newValue) },
            { DeclarationIrBuilder(context, lambdaFun.symbol).irGet(newInvalidMessages) }
        )

        lambdaFun.body?.transformChildrenVoid(innerRewriter)

        return arg
    }
}