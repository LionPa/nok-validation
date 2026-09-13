package io.lionpa.nok.compiler.validator.bindings

import io.lionpa.nok.compiler.validator.bindings.BindingsFinder.finder
import io.lionpa.nok.compiler.validator.data.NokType.Companion.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

object ValidationResultList {

    val nok = finder.type("io.lionpa.nok.validation:ValidationResultList")
    val classId = nok.classId
    val clazz = nok.irClassSymbol

    val constructor = finder.findConstructors(classId)
        .firstOrNull()!!

    val list = finder.findFunctions(CallableId(classId, Name.identifier("list")))
        .firstOrNull()!!
}