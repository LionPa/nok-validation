package io.lionpa.nok.compiler.validator

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

class FirValidatableGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

    companion object {
        val VALIDATE = Name.identifier($$"generated$validate")

        val VALIDATABLE_PREDICATE = DeclarationPredicate.create {
            annotated(FqName("io.lionpa.nok.validation.Validatable"))
        }

        val HAS_VALIDATABLE_PREDICATE = DeclarationPredicate.create {
            hasAnnotated(FqName("io.lionpa.nok.validation.Validatable"))
        }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!session.predicateBasedProvider.matches(VALIDATABLE_PREDICATE, classSymbol)) return emptySet()

        return setOf(VALIDATE)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName != VALIDATE) return emptyList()

        val list = ConeClassLikeTypeImpl(
            lookupTag = StandardClassIds.MutableList.toLookupTag(),
            typeArguments = arrayOf(session.builtinTypes.stringType.coneType.toTypeProjection(Variance.INVARIANT)),
            isMarkedNullable = false
        )

        val function = createMemberFunction(
            owner = context!!.owner,
            key = ClassValidationFunctionKey,
            name = callableId.callableName,
            returnType = list,
        )

        return listOf(function.symbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(VALIDATABLE_PREDICATE, HAS_VALIDATABLE_PREDICATE)
    }

    object ClassValidationFunctionKey : GeneratedDeclarationKey()

}