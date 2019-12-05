package de.yochyo.objectserializer

import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field

//TODO @Ignore Annotation hinzufügen
fun main() {
    val d = Dummy("testvalue", 5, 5.0)
    val json = ObjectSerializer.toJsonObject(d)
    println(json)
    val o = ObjectSerializer.toObject(json, Dummy::class.java)
    println(o)
}

object ObjectSerializer {
    /**
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation
     * @param o Any not-null object
     * @return JSONObject containing all fields of o
     */
    fun toJsonObject(o: Any): JSONObject {
        val json = JSONObject()
        for (field in o.javaClass.declaredFields)
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
                    json.put(it.name, it.get(o))
            }
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

        for (field in clazz.declaredFields) {
            accessField(field) {
                if (!field.isAnnotationPresent(Ignore::class.java))
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
    protected val int: Int
    val double: Double
    private val test: String

    @Ignore
    private val initialized = "init"

    constructor() : this("", 0, 0.0)
    constructor(test: String, int: Int, double: Double) : super("parent") {
        this.test = test
        this.int = int
        this.double = double
    }

    override fun toString(): String {
        return test + " " + initialized + " " + int + " " + double
    }
}

open class Parent {
    val a: String

    constructor() : this("")
    constructor(a: String) {
        this.a = a
    }
}