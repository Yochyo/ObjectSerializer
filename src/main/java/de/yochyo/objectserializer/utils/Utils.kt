package de.yochyo.objectserializer.utils

import de.yochyo.objectserializer.annotations.Ignore
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object Utils {
    /**
     * Checks if the class has a valid superclass for classToJson()
     * @param clazz The class
     * @return true if superclass != null and != Object
     */
    fun hasValidSuperclass(clazz: Class<*>): Boolean {
        val superclass = clazz.superclass
        return superclass != null && superclass != Object().javaClass
    }

    /**
     * Wrapper class for field access as most fields can't be accessed without setting isAccessible to true
     */
    fun accessField(field: Field, run: (field: Field) -> Unit) {
        val accessible = field.isAccessible
        field.isAccessible = true
        run(field)
        field.isAccessible = accessible
    }

    /**
     * Returns the default contructor of a class or throws an Exception if it does not have any.
     * @param clazz Class the default constructor is needed of
     * @return default contructor
     * @throws Exception
     */
    fun getDefaultConstructor(clazz: Class<*>): Constructor<*> {
        for (constructor in clazz.constructors)
            if (constructor.parameters.isEmpty()) return constructor
        throw Exception("Class ${clazz.name} does not contain a default constructor")
    }

    fun Array<Annotation>.toFlags() = Array(size) { (get(it) as java.lang.annotation.Annotation).annotationType().name }

    fun isValidField(field: Field) = !(field.isAnnotationPresent(Ignore::class.java) || Modifier.isStatic(field.modifiers))
}