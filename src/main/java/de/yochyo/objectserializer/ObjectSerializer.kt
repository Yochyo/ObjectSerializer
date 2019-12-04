package de.yochyo.objectserializer

import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field

fun main() {
    val d = Dummy("testvalue")
    val json = ObjectSerializer.toJsonObject(d)
    println(json)
    val o = ObjectSerializer.toObject(json, Dummy::class.java)
    println(o)
}

object ObjectSerializer {
    fun toJsonObject(o: Any): JSONObject {
        val json = JSONObject()
        for (field in o.javaClass.declaredFields)
            accessField(field) { json.put(it.name, it.get(o)) }
        return json
    }

    fun <E> toObject(json: JSONObject, clazz: Class<E>): E {
        val defaultConstructor = getDefaultConstructor<E>(clazz.constructors)
        val o = defaultConstructor.newInstance()

        for (field in clazz.declaredFields) {
            accessField(field) {
                it.set(o, json[it.name] ?: throw Exception("Json does not contain field \"${field.name}\""))
            }
        }
        return o
    }

    private fun <E> getDefaultConstructor(constructors: Array<Constructor<*>>): Constructor<E> {
        for (constructor in constructors)
            if (constructor.parameters.isEmpty()) return constructor as Constructor<E>
        throw Exception("Class does not contain a default constructor")
    }

    private fun accessField(field: Field, run: (field: Field) -> Unit) {
        val accessible = field.isAccessible
        field.isAccessible = true
        run(field)
        field.isAccessible = accessible
    }

}

class Dummy : Parent {
    private val test: String

    constructor() : this("empty")
    constructor(test: String) : super("parent") {
        this.test = test
    }

    override fun toString(): String {
        return test
    }
}

open class Parent(val a: String)