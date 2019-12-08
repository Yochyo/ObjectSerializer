package de.yochyo.objectserializer

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

//TODO list of object would not work
//TODO add documentation

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
        val defaultConstructor = getDefaultConstructor(clazz.constructors) as Constructor<E>
        val o = defaultConstructor.newInstance()
        jsonToClass(o, clazz, json)
        return o
    }

    private fun classToJson(o: Any, clazz: Class<*>, json: JSONObject): JSONObject {
        for (field in clazz.declaredFields)
            accessField(field) {
                if (isValidField(field))
                    readField(o, field, json)
            }

        if (hasValidSuperclass(clazz)) json.put("super", classToJson(o, clazz.superclass, JSONObject()))
        return json
    }

    private fun <E> jsonToClass(o: E, clazz: Class<*>, json: JSONObject): E {
        for (field in clazz.declaredFields) {
            accessField(field) {
                if (isValidField(field))
                    writeField(o as Any, field, json)
            }
        }
        if (hasValidSuperclass(clazz)) jsonToClass(o, clazz.superclass, json["super"] as JSONObject)
        return o
    }

    private fun readField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (field.isAnnotationPresent(Serializeable::class.java)) json.put(field.name, readSerializeable(o, field))
            else if (isPrimitiveOrString(field)) json.put(field.name, field.get(o))
            else if (field.type.isArray) json.put(field.name, readArray(o, field))
            else if (field.get(o) is Collection<*>) json.put(field.name, readCollection(o, field))
            else json.put(field.name, classToJson(field.get(o), field.type, JSONObject()))
        }
    }

    private fun writeField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (field.isAnnotationPresent(Serializeable::class.java)) field.set(o, parseSeriablizeable(json[field.name].toString()))
            else if (isPrimitiveOrString(field)) field.set(o, json[field.name])
            else if (field.type.isArray) field.set(o, parseArray(json[field.name] as JSONArray))
            else if (field.get(o) is Collection<*>) parseCollection(json[field.name] as JSONArray)
            else field.set(o, jsonToClass(field.get(o), field.type, json[field.name] as JSONObject))
        }
    }

    private fun readSerializeable(o: Any, field: Field): String {
        val stream = ByteArrayOutputStream()
        val out = ObjectOutputStream(stream)
        out.writeObject(field.get(o))
        out.flush()
        val res = Base64.getEncoder().encodeToString(stream.toByteArray())
        stream.close()
        return res
    }

    private fun parseSeriablizeable(string: String): Any {
        val stream = ByteArrayInputStream(Base64.getDecoder().decode(string))
        val `in` = ObjectInputStream(stream)
        val res = `in`.readObject()
        `in`.close()
        return res
    }

    private fun readArray(o: Any, field: Field): JSONArray {
        val json = JSONArray()
        val array = field.get(o) as Array<Any>
        json.put(array.javaClass.name.substring(2, array.javaClass.name.length - 1))

        if (isPrimitveOrStringArray(array)) array.forEach { json.put(it) }
        else array.forEach { json.put(toJSONObject(it)) }
        return json
    }

    private fun parseArray(json: JSONArray): Array<Any> {
        val clazz = Class.forName(json[0].toString())
        val array = java.lang.reflect.Array.newInstance(clazz, json.length() - 1) as Array<Any>

        if (isPrimitveOrStringArray(array)) for (i in 0 until json.length() - 1) array[i] = json[i + 1] //TODO kann man hier nicht  if(json[2] !is JSONObject)
        else for (i in 0 until json.length() - 1) array[i] = toObject(json[i + 1] as JSONObject, clazz)
        return array
    }

    private fun readCollection(o: Any, field: Field): JSONArray {
        val json = JSONArray()
        val array = field.get(o) as Collection<Any>
        json.put(array.javaClass.typeName)
        if (array.isNotEmpty()) {
            json.put(array.first().javaClass.name)
            if (array.first() is String || array.first() is java.lang.Number) array.forEach { json.put(it) }
            else array.forEach { json.put(toJSONObject(it)) }
        } else json.put("java.lang.Object")
        return json
    }

    private fun parseCollection(json: JSONArray): Collection<Any> {
        val listType = Class.forName(json[0].toString())
        val typeClass = Class.forName(json[1].toString())
        val collection = getDefaultConstructor(listType.constructors).newInstance() as MutableCollection<Any>
        if (json.length() > 2) {
            if (json[2] !is JSONObject) for (i in 0 until json.length() - 2) collection.add(json[i + 2])
            else {
                for (i in 0 until json.length() - 2) collection.add(toObject(json[i + 2] as JSONObject, typeClass))
            }
        }

        return collection
    }

    private fun accessField(field: Field, run: (field: Field) -> Unit) {
        val accessible = field.isAccessible
        field.isAccessible = true
        run(field)
        field.isAccessible = accessible
    }

    private fun getDefaultConstructor(constructors: Array<Constructor<*>>): Constructor<*> {
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
    private fun isValidField(field: Field) = !(field.isAnnotationPresent(Ignore::class.java) || Modifier.isStatic(field.modifiers))
}