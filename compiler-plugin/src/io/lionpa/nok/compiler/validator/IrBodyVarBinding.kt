package io.lionpa.nok.compiler.validator

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid


// TODO Rewrite (AI Slop)
class IrBodyVarBinding(
    val originalFunction: IrSimpleFunction,
    val generatedFunction: IrSimpleFunction,
    private val irBuiltIns: IrBuiltIns
) : IrElementTransformerVoid() {

    private val valueSymbolMap = mutableMapOf<IrValueSymbol, IrValueParameter>()
    private val lambdaInvokeSymbols = mutableMapOf<IrValueSymbol, IrSimpleFunctionSymbol>()

    private val newValueParam: IrValueParameter
    private val newInvalidMessagesParam: IrValueParameter
    private val newFieldName: IrValueParameter
    private val originalReceiver: IrValueParameter?

    init {
        val generatedParams = generatedFunction.parameters

        // Маппим фиксированные параметры согласно контракту FIR
        this.newFieldName = generatedParams[0]
        this.newValueParam = generatedParams[1]
        this.newInvalidMessagesParam = generatedParams[2]

        // Запоминаем оригинальный ресивер (Schema<T>)
        this.originalReceiver = originalFunction.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            ?: originalFunction.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }

        if (originalReceiver != null) {
            valueSymbolMap[originalReceiver.symbol] = newValueParam
        }

        // Маппим кастомные параметры пользователя (min, max и т.д.)
        val originalRegularParams = originalFunction.parameters.filter {
            it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context
        }

        originalRegularParams.forEachIndexed { index, originalParam ->
            val newParam = generatedParams.getOrNull(3 + index)
            if (newParam != null) {
                valueSymbolMap[originalParam.symbol] = newParam
                registerLambdaRewrite(originalParam, newParam)
            }
        }
    }

    private fun registerLambdaRewrite(originalParam: IrValueParameter, newParam: IrValueParameter) {
        val originalArity = originalParam.type.functionArityOrNull() ?: return
        val newArity = newParam.type.functionArityOrNull() ?: return
        if (newArity != originalArity + 2) return

        val invokeSymbol = newParam.type.classOrNull?.owner?.functions
            ?.firstOrNull {
                it.name.asString() == "invoke" && it.regularAndContextParameterCount() == newArity
            }
            ?.symbol
            ?: return

        lambdaInvokeSymbols[newParam.symbol] = invokeSymbol
    }

    private fun findGeneratedBlockParam(): IrValueParameter? {
        return generatedFunction.parameters.firstOrNull { param ->
            (param.kind == IrParameterKind.Regular || param.kind == IrParameterKind.Context) &&
                param.type.functionArityOrNull() == 3
        }
    }

    private fun findInvokeSymbolForParam(param: IrValueParameter?): IrSimpleFunctionSymbol? {
        val functionParam = param ?: return null
        val arity = functionParam.type.functionArityOrNull() ?: return null

        return functionParam.type.classOrNull?.owner?.functions
            ?.firstOrNull {
                it.name.asString() == "invoke" && it.regularAndContextParameterCount() == arity
            }
            ?.symbol
    }

    private fun IrSimpleFunction.regularAndContextParameterCount(): Int =
        parameters.count { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }

    private fun IrType.functionArityOrNull(): Int? {
        val fqName = classFqName?.asString() ?: return null
        val match = Regex(""".*Function(\d+)""").matchEntire(fqName) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun mapValueSymbol(symbol: IrValueSymbol): IrValueParameter? {
        return valueSymbolMap[symbol]
    }

    private fun unwrapReceiver(expression: IrExpression?): IrExpression? = when (expression) {
        is IrTypeOperatorCall -> unwrapReceiver(expression.argument)
        else -> expression
    }

    private fun isOriginalReceiverValue(expression: IrExpression?): Boolean {
        val unwrapped = unwrapReceiver(expression)
        return unwrapped is IrGetValue && (
            unwrapped.symbol == originalReceiver?.symbol ||
                unwrapped.symbol == newValueParam.symbol ||
                (unwrapped.symbol.owner is IrValueParameter &&
                    (unwrapped.symbol.owner as IrValueParameter).kind == IrParameterKind.ExtensionReceiver)
            )
    }

    private fun extractSubValue(call: IrCall): IrExpression? {
        val receiver = unwrapReceiver(call.getReceiverArgument()) ?: return null
        if (!isOriginalReceiverValue(receiver)) return null

        // Любой вызов на ресивере, который берет 1 регулярный аргумент
        if (call.regularArgumentCount() != 1) return null

        return call.getRegularArgument(0)
    }

    private fun buildRewrittenInvoke(
        original: IrCall,
        invokeSymbol: IrSimpleFunctionSymbol,
        itemArg: IrExpression
    ): IrCall {
        val newCall = IrCallImpl.fromSymbolOwner(
            startOffset = original.startOffset,
            endOffset = original.endOffset,
            type = invokeSymbol.owner.returnType,
            symbol = invokeSymbol
        )

        val dispatchIndex = invokeSymbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
        if (dispatchIndex >= 0) {
            val receiver = unwrapReceiver(original.getDispatchReceiver()) ?: unwrapReceiver(original.getExtensionReceiver())
            val mappedReceiver = when (receiver) {
                is IrGetValue -> {
                    val mappedParam = mapValueSymbol(receiver.symbol)
                    mappedParam?.let { irGetValue(it, original.startOffset, original.endOffset) }
                        ?: receiver.transform(this, null) as IrExpression
                }
                null -> null
                else -> receiver.transform(this, null) as IrExpression
            }
            newCall.arguments[dispatchIndex] = mappedReceiver
        }

        val regularParams = invokeSymbol.owner.parameters.filter {
            it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context
        }

        if (regularParams.size >= 3) {
            newCall.arguments[regularParams[0].indexInParameters] =
                irGetValue(newFieldName, original.startOffset, original.endOffset)
            newCall.arguments[regularParams[1].indexInParameters] =
                itemArg.transform(this, null) as IrExpression
            newCall.arguments[regularParams[2].indexInParameters] =
                irGetValue(newInvalidMessagesParam, original.startOffset, original.endOffset)

            for (i in 1 until original.regularArgumentCount()) {
                val targetParam = regularParams.getOrNull(2 + i) ?: break
                newCall.arguments[targetParam.indexInParameters] =
                    original.getRegularArgument(i)?.transform(this, null)
            }
        } else {
            // Если вызывается не "пропатченная" FIR лямбда (которая arity+2),
            // а обычная пользовательская лямбда (как в block(fieldName, value))
            for (i in 0 until regularParams.size) {
                val targetParam = regularParams[i]
                val arg = original.getRegularArgument(i)
                if (arg != null) {
                    newCall.arguments[targetParam.indexInParameters] = arg.transform(this, null)
                }
            }
        }

        newCall.typeArguments.clear()
        newCall.typeArguments.addAll(original.typeArguments)
        while (newCall.typeArguments.size < invokeSymbol.owner.typeParameters.size) {
            newCall.typeArguments.add(null)
        }
        if (newCall.typeArguments.size > invokeSymbol.owner.typeParameters.size) {
            newCall.typeArguments.subList(
                invokeSymbol.owner.typeParameters.size,
                newCall.typeArguments.size
            ).clear()
        }

        return newCall
    }

    private fun IrCall.getDispatchReceiver(): IrExpression? {
        val index = symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
        return if (index >= 0) arguments.getOrNull(index) else null
    }

    private fun IrCall.getExtensionReceiver(): IrExpression? {
        val index = symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        return if (index >= 0) arguments.getOrNull(index) else null
    }

    private fun IrCall.getReceiverArgument(): IrExpression? {
        return this.getExtensionReceiver() ?: this.getDispatchReceiver()
    }

    private fun IrCall.getRegularArgument(index: Int): IrExpression? {
        val param = symbol.owner.parameters
            .filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
            .getOrNull(index)
            ?: return null
        return arguments.getOrNull(param.indexInParameters)
    }

    private fun IrCall.regularArgumentCount(): Int =
        symbol.owner.parameters.count { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }

    private fun irGetValue(param: IrValueParameter, startOffset: Int, endOffset: Int): IrGetValueImpl {
        return IrGetValueImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = param.type,
            symbol = param.symbol,
            origin = null
        )
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        return IrReturnImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            // ФИКС 1: Сама инструкция RETURN обязана иметь тип Nothing
            type = irBuiltIns.nothingType,
            returnTargetSymbol = generatedFunction.symbol,
            value = expression.value.transform(this, null)
        )
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val targetNewParam = mapValueSymbol(expression.symbol)
        if (targetNewParam != null) {
            return IrGetValueImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = targetNewParam.type,
                symbol = targetNewParam.symbol,
                origin = null
            )
        }
        return super.visitGetValue(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val dispatchGetValue = (unwrapReceiver(expression.getDispatchReceiver()) as? IrGetValue)
            ?: (unwrapReceiver(expression.getExtensionReceiver()) as? IrGetValue)
            
        val mappedReceiver = dispatchGetValue?.let { mapValueSymbol(it.symbol) }
        
        if (mappedReceiver != null && expression.symbol.owner.name.asString() == "invoke") {
            val invokeSymbol = lambdaInvokeSymbols[mappedReceiver.symbol]
                ?: mappedReceiver.type.classOrNull?.owner?.functions
                    ?.firstOrNull { it.name.asString() == "invoke" }
                    ?.symbol

            if (invokeSymbol != null) {
                // Пытаемся понять, является ли это Schema-вызовом (типа block(sub(item)))
                val receiverArg = expression.getRegularArgument(0)
                val subCallArg = (receiverArg as? IrCall)?.let { extractSubValue(it) }
                
                // Если мы смогли идентифицировать ресивер, то это "сгенерированный" schema-block (arity >= 3)
                // Если нет - это может быть вызов обычной лямбды
                if (lambdaInvokeSymbols.containsKey(mappedReceiver.symbol)) {
                    val itemArg = subCallArg ?: receiverArg
                    if (itemArg != null) {
                        return buildRewrittenInvoke(expression, invokeSymbol, itemArg)
                    }
                } else {
                    return buildRewrittenInvoke(expression, invokeSymbol, expression.getRegularArgument(0) ?: expression)
                }
            }
        }

        // Ищем, есть ли у вызываемой функции ресивер типа Schema
        val ownerParameters = expression.symbol.owner.parameters

        // Находим индекс Extension-ресивера в сигнатуре вызываемой функции
        val extensionReceiverIndex = ownerParameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        val dispatchReceiverIndex = ownerParameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }

        // Проверяем, является ли аргумент, переданный в качестве ресивера, чтением нашего оригинального Schema-ресивера
        val isCallOnSchemaReceiver = originalReceiver != null && (
            (extensionReceiverIndex != -1 && isOriginalReceiverValue(expression.arguments[extensionReceiverIndex])) ||
                (dispatchReceiverIndex != -1 && isOriginalReceiverValue(expression.arguments[dispatchReceiverIndex]))
            )

        if (isCallOnSchemaReceiver) {
            val propertySymbol = expression.symbol.owner.correspondingPropertySymbol
            val propertyName = propertySymbol?.owner?.name?.asString() ?: expression.symbol.owner.name.asString()

            if (propertyName == "value" || propertyName == "<get-value>") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newValueParam.type, newValueParam.symbol, null)
            }
            if (propertyName == "invalidMessages" || propertyName == "<get-invalidMessages>") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newInvalidMessagesParam.type, newInvalidMessagesParam.symbol, null)
            }
            if (propertyName == "fieldName" || propertyName == "<get-fieldName>") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newFieldName.type, newFieldName.symbol, null)
            }
        }

        return super.visitCall(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        // У IrGetField пока остался обычный nullable receiver (так как это чтение поля Java/Kotlin класса напрямую)
        val isFieldOnSchemaReceiver = originalReceiver != null && isOriginalReceiverValue(expression.receiver)

        if (isFieldOnSchemaReceiver) {
            val fieldName = expression.symbol.owner.name.asString()
            if (fieldName == "value") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newValueParam.type, newValueParam.symbol, null)
            }
            if (fieldName == "invalidMessages") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newInvalidMessagesParam.type, newInvalidMessagesParam.symbol, null)
            }
            if (fieldName == "fieldName" || fieldName == "ieldName") {
                return IrGetValueImpl(expression.startOffset, expression.endOffset, newFieldName.type, newFieldName.symbol, null)
            }
        }

        return super.visitGetField(expression)
    }
}