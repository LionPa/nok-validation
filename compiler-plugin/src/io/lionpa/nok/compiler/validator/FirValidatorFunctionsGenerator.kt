package io.lionpa.nok.compiler.validator

import io.lionpa.nok.compiler.validator.util.generateFirFunctionUniqueId
import io.lionpa.nok.compiler.validator.util.replaceIllegalSymbols
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

class FirValidatorFunctionsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val VALIDATOR_PREDICATE = DeclarationPredicate.create {
            annotated(FqName("io.lionpa.nok.validation.Validator"))
        }

        val VALIDATOR_LOOKUP = LookupPredicate.create {
            annotated(FqName("io.lionpa.nok.validation.Validator"))
        }

        val HAS_VALIDATOR_PREDICATE = DeclarationPredicate.create {
            hasAnnotated(FqName("io.lionpa.nok.validation.Validator"))
        }
    }

    data class ValidatorFunction(val flatValidatorId: CallableId, val originalValidator: FirNamedFunctionSymbol, val originalUQID: String)

    val validators = hashMapOf<CallableId, ValidatorFunction>()
    val flatValidators = mutableSetOf<CallableId>()


    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelCallableIds(): Set<CallableId> {
        var symbols = session.predicateBasedProvider.getSymbolsByPredicate(VALIDATOR_LOOKUP)

        symbols = symbols.filterIsInstance<FirNamedFunctionSymbol>()

        for (symbol in symbols) {
            val name = replaceIllegalSymbols("${symbol.callableId.callableName}\$GeneratedValidator\$${symbol.generateFirFunctionUniqueId()}")
            val callableId = symbol.callableId.copy(Name.identifier(name))
            validators[callableId] = ValidatorFunction(callableId, symbol, replaceIllegalSymbols(symbol.generateFirFunctionUniqueId())
            )
            flatValidators += callableId
        }

        return flatValidators
    }

    val FIELD_NAME = Name.identifier("fieldName")
    val VALUE = Name.identifier("value")
    val INVALID_MESSAGES = Name.identifier("invalidMessages")

    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        if (!flatValidators.contains(callableId)) return emptyList()

        if (context != null) {
            // TODO Добавить ошибку о том, что validator должен быть вне классов
        }

        val validator = validators[callableId]!!

        session.firProvider.getContainingFile(validator.originalValidator)!!

        val function = createTopLevelFunction(
            returnType = validator.originalValidator.resolvedReturnType,
            key = ValidatorFunctionKey(validator),
            config = {
                status {
                    val originalStatus = validator.originalValidator.rawStatus
                    isInline = originalStatus.isInline
                }

                valueParameter(FIELD_NAME, session.builtinTypes.stringType.coneType)
                valueParameter(
                    VALUE,
                    validator.originalValidator.resolvedReceiverType!!.typeArguments.firstOrNull()!!.type!!
                )

                val invalidMessagesType = ConeClassLikeTypeImpl(
                    lookupTag = classId(
                        "io.lionpa.nok.validation",
                        "ValidationResultList"
                    ).toLookupTag(),
                    typeArguments = arrayOf(),
                    isMarkedNullable = false
                )

                valueParameter(INVALID_MESSAGES, invalidMessagesType)

                for (originParam in validator.originalValidator.valueParameterSymbols) {
                    val mappedType = mapSchemaLambdaType(originParam.resolvedReturnType, invalidMessagesType)
                        ?: originParam.resolvedReturnType
                    valueParameter(originParam.name, mappedType)
                }
            },

            callableId = callableId,
            containingFileName = session.firProvider.getContainingFile(validator.originalValidator)!!.name + "GeneratedValidator"
        )

        return listOf(function.symbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(VALIDATOR_PREDICATE, HAS_VALIDATOR_PREDICATE)
    }

    private fun mapSchemaLambdaType(
        type: ConeKotlinType,
        invalidMessagesType: ConeClassLikeTypeImpl
    ): ConeKotlinType? {
        if (!type.isSomeFunctionType(session) || !type.isExtensionFunctionType(session)) return null

        val receiver = type.receiverType(session) ?: return null
        val receiverClassId = receiver.classId ?: return null
        if (receiverClassId.shortClassName.asString() != "Schema") return null

        val receiverType = receiver as? ConeClassLikeType ?: return null
        val receiverValueType = receiverType.typeArguments.firstOrNull()
            ?.toTypeOrDefault(session.builtinTypes.anyType.coneType)
            ?: session.builtinTypes.anyType.coneType

        val functionType = type as? ConeClassLikeType ?: return null
        val returnType = functionType.returnType(session)
        val otherParams = functionType.valueParameterTypesWithoutReceivers(session)

        val paramTypes = buildList {
            add(session.builtinTypes.stringType.coneType)
            add(receiverValueType)
            add(invalidMessagesType)
            addAll(otherParams)
        }

        val typeArgs = (paramTypes.map { it.toTypeProjection(Variance.INVARIANT) } +
            returnType.toTypeProjection(Variance.INVARIANT)).toTypedArray()

        val functionClassId = StandardClassIds.FunctionN(paramTypes.size)
        return ConeClassLikeTypeImpl(
            lookupTag = functionClassId.toLookupTag(),
            typeArguments = typeArgs,
            isMarkedNullable = false
        )
    }

    private fun ConeTypeProjection.toTypeOrDefault(default: ConeKotlinType): ConeKotlinType = when (this) {
        is ConeKotlinTypeProjection -> type
        is ConeStarProjection -> default
    }

    // TODO Добавить полезную информацию
    data class ValidatorFunctionKey(val validatorFunction: ValidatorFunction) : GeneratedDeclarationKey()
}
