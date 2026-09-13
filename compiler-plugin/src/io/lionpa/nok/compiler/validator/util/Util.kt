package io.lionpa.nok.compiler.validator.util

import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.callableId

inline fun <reified T> IrDeclaration.ifGenerated(block: (T) -> Unit) {
    val origin = this.origin
    if (origin is IrDeclarationOrigin.GeneratedByPlugin){
        if (origin.pluginKey is T) {
            block.invoke(origin.pluginKey as T)
        }
    }
}

fun FirNamedFunctionSymbol.generateFirFunctionUniqueId(): String {
    val baseName = "${callableId.packageName}.${callableId.callableName}"

    fun ConeKotlinType?.toFqNameString(): String {
        if (this == null) return "void"
        val classId = (this as? ConeClassLikeType)?.lookupTag?.classId
        return classId?.asSingleFqName()?.asString() ?: this.toString()
    }

    val receiverType = resolvedReceiverTypeRef?.coneType.toFqNameString()

    val argumentsTypes = valueParameterSymbols.joinToString(separator = ",") { param ->
        param.resolvedReturnType.toFqNameString()
    }

    val returnType = resolvedReturnTypeRef.coneType.toFqNameString()

    return "$baseName(receiver=$receiverType, args=[$argumentsTypes]) -> $returnType"
}


fun IrSimpleFunction.generateIrFunctionUniqueId(): String {
    val baseName = "${callableId.packageName}.${callableId.callableName}"

    fun IrType?.toFqNameString(): String {
        if (this == null) return "void"
        return this.classFqName?.asString() ?: this.toString()
    }

    val receiverParam = parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
    val receiverType = receiverParam?.type.toFqNameString()

    val regularArgsTypes = parameters
        .filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
        .joinToString(separator = ",") { param ->
            param.type.toFqNameString()
        }

    val returnType = returnType.toFqNameString()

    return "$baseName(receiver=$receiverType, args=[$regularArgsTypes]) -> $returnType"
}

fun replaceIllegalSymbols(string: String): String {
    return string
        .replace(" ", "")
        .replace("-","_")
        .replace("(", "_")
        .replace(")", "_")
        .replace(",", "_")
        .replace("<", "_")
        .replace(">", "_")
        .replace("=", "_")
        .replace(".", "_")
        .replace("[", "_")
        .replace("]", "_")
}