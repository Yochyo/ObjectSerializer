package de.yochyo.objectserializer

import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field

fun main() {
    val d = Dummy("testvalue")
    d.a = "test_if_it_works"
    d.field.field2 = "field22"
    d.superParent = "super"
    val json = ObjectSerializer.toJsonObject(d)
    println(json)
    println(ObjectSerializer.toObject(json, Dummy::class.java))
}

object ObjectSerializer {
    /**
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation
     * @param o Any not-null object
     * @return JSONObject containing all fields of o
     */
    fun toJsonObject(o: Any): JSONObject {
        val json = JSONObject()
        classToJson(o, o.javaClass, json)
        return json
    }

    private fun classToJson(o: Any, clazz: Class<*>, json: JSONObject): JSONObject {
        for (field in clazz.declaredFields)
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
                    addFieldToJson(o, field, json)
            }
        if (hasValidSuperclass(clazz)) json.put("super", classToJson(o, clazz.superclass, JSONObject()))
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
        val defaultConstructor = getDefaultConstructor<E>(clazz.constructors)
        val o = defaultConstructor.newInstance()
        jsonToClass(o, clazz, json)
        return o
    }

    private fun <E> jsonToClass(o: E, clazz: Class<*>, json: JSONObject): E {
        for (field in clazz.declaredFields) {
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
                    jsonToField(o as Any, field, json)
            }
        }
        if (hasValidSuperclass(clazz)) jsonToClass(o, clazz.superclass, json["super"] as JSONObject)
        return o
    }

    private fun addFieldToJson(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (isPrimitiveOrString(field)) json.put(field.name, field.get(o))
            else json.put(field.name, classToJson(field.get(o), field.type, JSONObject()))
        }
    }

    private fun jsonToField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (field.type.isPrimitive || field.type == String().javaClass) field.set(o, json[field.name])
            else field.set(o, jsonToClass(field.get(o), field.type, json[field.name] as JSONObject))
        }
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

    private fun hasValidSuperclass(clazz: Class<*>): Boolean {
        val superclass = clazz.superclass
        return superclass != null && superclass != Object().javaClass
    }
    private fun isPrimitiveOrString(field: Field) = field.type.isPrimitive || field.type == String().javaClass
}

class Dummy : Parent {
    val field = FieldInClass()

    private val initialized = "init"

    constructor() : this("")
    constructor(test: String) : super("parent") {
    }

    override fun toString(): String {
        return "${super.toString()} $field $initialized"
    }
}

class FieldInClass {
    var field1 = "field1"
    var field2 = "field2"

    override fun toString(): String {
        return "field1=$field1 field2=$field2"
    }
}

open class Parent : SuperParent {
    var a: String
    private val test: String = "test_in_parent"

    constructor() : this("")
    constructor(a: String) {
        this.a = a
    }

    override fun toString(): String {
        return super.toString() + "a=$a"
    }
}

open class SuperParent {
    var superParent: String

    constructor() : this("")
    constructor(a: String) {
        this.superParent = a
    }

    override fun toString(): String {
        return "super($superParent)"
    }
}