package io.lionpa.nok.validation.spring

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object NokValidatorInvoker {

    private val LOOKUP = MethodHandles.lookup()
    private val TARGET_TYPE = MethodType.methodType(List::class.java, Any::class.java)

    val VALIDATE_HANDLES = object : ClassValue<MethodHandle?>() {
        override fun computeValue(clazz: Class<*>): MethodHandle? {
            return try {
                LOOKUP.unreflect(clazz.getMethod("generated\$validate")).asType(TARGET_TYPE)
            } catch (_: NoSuchMethodException) {
                null
            }
        }
    }

    fun getValidateHandle(clazz: Class<*>): MethodHandle? {
        return VALIDATE_HANDLES.get(clazz)
    }

    @Suppress("UNCHECKED_CAST")
    fun invokeValidate(target: Any): List<String> {
        val handle = getValidateHandle(target.javaClass)
            ?: throw IllegalStateException("Class ${target.javaClass.name} does not have generated\$validate method")
        
        return handle.invokeExact(target) as List<String>
    }
}