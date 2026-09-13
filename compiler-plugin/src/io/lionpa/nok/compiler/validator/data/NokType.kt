package io.lionpa.nok.compiler.validator.data

import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.*


// io.undertow.server, RoutingHandler
data class NokType(val packageName: String, val name: String) {

    val classId = ClassId(
        FqName(packageName),
        FqName(name),
        false
    )

    lateinit var irClassSymbol: IrClassSymbol
    lateinit var irType: IrSimpleType

    var irInitialized = false

    fun irInit(finder: DeclarationFinder) : NokType {
        if (irInitialized) return this
        irClassSymbol = finder.findClass(classId)!!

        irType = irClassSymbol.owner.defaultType

        irInitialized = true
        return this
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun irType(context: IrPluginContext) : IrSimpleType {
        return context.symbolTable.referenceClass(IdSignature.CommonSignature(
            packageName,
            name,
            null,
            0L,
            null
        )).defaultType
    }

    fun firType(session: FirSession) : ConeKotlinType {
        return session.symbolProvider
            .getClassLikeSymbolByClassId(ClassId(
                FqName(packageName),
                FqName(name),
                false
            ))!!.defaultType()
    }

    companion object {
        fun DeclarationFinder.type(str: String) : NokType {
            return of(str).irInit(this)
        }

        fun from(type: ConeKotlinType) : NokType {
            return from(type.classId!!)
        }

        fun from(type: ClassId) : NokType {
            val pair = Pair(type.packageFqName.toString(), type.shortClassName.toString())

            if (NokTypesCache.cache.contains(pair)) {
                return NokTypesCache.cache[pair]!!
            }

            return NokType(pair.first, pair.second)
        }

        fun of(str: String) : NokType {
            val split = str.split(":")

            val pair = Pair(split[0], split[1])

            if (NokTypesCache.cache.contains(pair)) {
                return NokTypesCache.cache[pair]!!
            }

            return NokType(split[0], split[1])
        }
    }
}

object NokTypesCache {
    val cache = WeakHashMap<Pair<String, String>, NokType>()
}
