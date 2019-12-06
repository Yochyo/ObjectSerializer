package de.yochyo.objectserializer

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field

//TODO Array of Objects
//TODO Can not set static final long field java.util.ArrayList.serialVersionUID to java.lang.Long
//TODO implement lambdas
fun main() {
    val d = Dummy("testvalue")
    val json = ObjectSerializer.toJSONObject(d)
    println(json)
    println(ObjectSerializer.toObject(json, Dummy::class.java))
}

object ObjectSerializer {
    /**
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation
     * @param o Any not-null object
     * @return JSONObject containing all fields of o
     */
    fun toJSONObject(o: Any): JSONObject {
        val json = JSONObject()
        classToJson(o, o.javaClass, json)
        return json
    }

    /**
     * Takes a JSONObject and will parse it to a class of type E. This will only work if the class,
     * all it's superclasses and it's object instances contain a default constructor
     * @param json a JSONObject
     * @param clazz the class you want it function to return
     * @return instance of class E
     * @throws Exception
     */
    fun <E> toObject(json: JSONObject, clazz: Class<E>): E {
        val defaultConstructor = getDefaultConstructor(clazz.constructors as Array<Constructor<E>>)
        val o = defaultConstructor.newInstance()
        jsonToClass(o, clazz, json)
        return o
    }

    private fun classToJson(o: Any, clazz: Class<*>, json: JSONObject): JSONObject {
        for (field in clazz.declaredFields)
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
                    readField(o, field, json)
            }

        if (hasValidSuperclass(clazz)) json.put("super", classToJson(o, clazz.superclass, JSONObject()))
        return json
    }

    private fun <E> jsonToClass(o: E, clazz: Class<*>, json: JSONObject): E {
        for (field in clazz.declaredFields) {
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
                    writeField(o as Any, field, json)
            }
        }
        if (hasValidSuperclass(clazz)) jsonToClass(o, clazz.superclass, json["super"] as JSONObject)
        return o
    }

    private fun readField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (isPrimitiveOrString(field)) json.put(field.name, field.get(o))
            else if (field.type.isArray) json.put(field.name, readArray(o, field))
            else json.put(field.name, classToJson(field.get(o), field.type, JSONObject()))
        }
    }

    private fun writeField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (isPrimitiveOrString(field)) field.set(o, json[field.name])
            else if (field.type.isArray) field.set(o, readJSONArray(json[field.name] as JSONArray))
            else field.set(o, jsonToClass(field.get(o), field.type, json[field.name] as JSONObject))
        }
    }

    private fun readJSONArray(json: JSONArray): Array<Any> {
        val clazz = Class.forName(json[0].toString())
        val array = java.lang.reflect.Array.newInstance(clazz, json.length() - 1) as Array<Any>

        if (isPrimitveOrStringArray(array)) for (i in 0 until json.length() - 1) array[i] = json[i + 1]
        else for (i in 0 until json.length() - 1) array[i] = toObject(json[i + 1] as JSONObject, clazz)
        return array
    }

    private fun readArray(o: Any, field: Field): JSONArray {
        val json = JSONArray()
        val array = field.get(o) as Array<Any>
        json.put(array.javaClass.name.substring(2, array.javaClass.name.length - 1))

        if (isPrimitveOrStringArray(array)) array.forEach { json.put(it) }
        else array.forEach { json.put(toJSONObject(it)) }
        return json
    }

    private fun accessField(field: Field, run: (field: Field) -> Unit) {
        val accessible = field.isAccessible
        field.isAccessible = true
        run(field)
        field.isAccessible = accessible
    }

    private fun <E> getDefaultConstructor(constructors: Array<Constructor<E>>): Constructor<E> {
        for (constructor in constructors)
            if (constructor.parameters.isEmpty()) return constructor
        throw Exception("Class does not contain a default constructor")
    }

    private fun hasValidSuperclass(clazz: Class<*>): Boolean {
        val superclass = clazz.superclass
        return superclass != null && superclass != Object().javaClass
    }

    private fun isPrimitveOrStringArray(array: Array<Any>) = Array<String>::class.java.isAssignableFrom(array.javaClass) || Array<java.lang.Number>::class.java.isAssignableFrom(array.javaClass)
    private fun isPrimitiveOrString(field: Field) = field.type.isPrimitive || field.type == String().javaClass
}

class Dummy {
    // val lambda: () -> Unit = { println("lambda_testssss") }
    val array = emptyArray<Int>()
    val fooArray = arrayOf(Foo(), Foo())
    private val initialized = "init"

    constructor() : this("")
    constructor(test: String) {
        // val a = lambda.javaClass
    }

    override fun toString(): String {
        return "$initialized [${array.joinToString { it.toString() }}]  [${fooArray.joinToString { it.toString() }}]"
    }
}

class Foo {
    val foo = "1"
    override fun toString(): String {
        return foo
    }
}