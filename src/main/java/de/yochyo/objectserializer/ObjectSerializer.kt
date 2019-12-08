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
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation.
     * Lambdas stored in variable or other Serializable objects
     * can be serialized if they have the @Serializeable annotation.
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
     * all it's superclasses and it's field instances contain a default constructor
     * or have the @Serializeable annotation
     *
     * @param json a JSONObject
     * @param clazz the class you want it function to return
     * @return instance of class E
     * @throws Exception
     */
    fun <E> toObject(json: JSONObject, clazz: Class<E>): E {
        val defaultConstructor = getDefaultConstructor(clazz) as Constructor<E>
        val o = defaultConstructor.newInstance()
        jsonToClass(o, clazz, json)
        return o
    }

    /**
     *  Takes an Object and parses it to a JSONObject
     *  @param o Object that should be parsed
     *  @param clazz Class whose fields should be read (needed for iterating through superclasses)
     *  @return JSONObject representing Object o
     *  @throws Exception
     */
    private fun classToJson(o: Any, clazz: Class<*>, json: JSONObject): JSONObject {
        for (field in clazz.declaredFields)
            accessField(field) {
                if (isValidField(field))
                    readField(o, field, json)
            }

        if (hasValidSuperclass(clazz)) json.put("super", classToJson(o, clazz.superclass, JSONObject()))
        return json
    }

    /**
     *  Takes a JSONObject and parses it to an Object
     *  @param E class it should be parsed to
     *  @param o Object that should be parsed
     *  @param clazz Class whose fields should be read (needed for iterating through superclasses)
     *  @param json JSONObject representing the Object
     *  @return JSONObject representing Object o
     *  @throws Exception
     */
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

    /**
     * Decides how to parse a field. It will be serialized if it has the @Serializeable annotation,
     * or parsed to a value/object.
     * @param o Object that should be parsed
     * @param field Field that should be parsed
     * @param json JSONObject were the result is written into
     * @return returns the JSONObject handed over as parameter
     * @throws Exception
     */
    private fun readField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (field.isAnnotationPresent(Serializeable::class.java)) json.put(field.name, readSerializeable(o, field))
            else if (isPrimitiveOrString(field)) json.put(field.name, field.get(o))
            else if (field.type.isArray) json.put(field.name, readArray(o, field))
            else if (field.get(o) is Collection<*>) json.put(field.name, readCollection(o, field))
            else json.put(field.name, classToJson(field.get(o), field.type, JSONObject()))
        }
    }

    /**
     * Decides how to write the value into the field. It will be unserialized if it has the @Serializeable annotation,
     * or unparsed from a value/object.
     * @param o Object that should be parsed
     * @param field Field that should be written
     * @param json JSONObject representing the Object
     * @throws Exception
     */
    private fun writeField(o: Any, field: Field, json: JSONObject) {
        accessField(field) {
            if (field.isAnnotationPresent(Serializeable::class.java)) field.set(o, parseSeriablizeable(json[field.name].toString()))
            else if (isPrimitiveOrString(field)) field.set(o, json[field.name])
            else if (field.type.isArray) field.set(o, parseArray(json[field.name] as JSONArray))
            else if (field.get(o) is Collection<*>) parseCollection(json[field.name] as JSONArray)
            else field.set(o, jsonToClass(field.get(o), field.type, json[field.name] as JSONObject))
        }
    }

    /**
     * Reads a value with the @Serializeable annotation
     * @param o Object that should be parsed
     * @param field Field that should be parsed
     * @return String containing the serialized Object
     * @throws Exception
     */
    private fun readSerializeable(o: Any, field: Field): String {
        val stream = ByteArrayOutputStream()
        val out = ObjectOutputStream(stream)
        out.writeObject(field.get(o))
        out.flush()
        val res = Base64.getEncoder().encodeToString(stream.toByteArray())
        stream.close()
        return res
    }

    /**
     * Reads a serialized value and returns the Object it represents
     * @param string string representing the serialized object
     * @return Object
     * @throws Exception
     */
    private fun parseSeriablizeable(string: String): Any {
        val stream = ByteArrayInputStream(Base64.getDecoder().decode(string))
        val `in` = ObjectInputStream(stream)
        val res = `in`.readObject()
        `in`.close()
        return res
    }

    /**
     * Reads a value that is an Array
     * @param o Object that should be parsed
     * @param field Field that should be parsed
     * @return JSONArray representing the array
     * @throws Exception
     */
    private fun readArray(o: Any, field: Field): JSONArray {
        val json = JSONArray()
        val array = field.get(o) as Array<Any>
        json.put(array.javaClass.name.substring(2, array.javaClass.name.length - 1))

        if (isPrimitveOrStringArray(array)) array.forEach { json.put(it) }
        else array.forEach { json.put(toJSONObject(it)) }
        return json
    }

    /**
     * Reads a JSONArray and returns the Object it represents
     * @param json JSONArray representing the serialized object
     * @return Array represented by the JSONArray
     * @throws Exception
     */
    private fun parseArray(json: JSONArray): Array<Any> {
        val clazz = Class.forName(json[0].toString())
        val array = java.lang.reflect.Array.newInstance(clazz, json.length() - 1) as Array<Any>

        if (isPrimitveOrStringArray(array)) for (i in 0 until json.length() - 1) array[i] = json[i + 1] //TODO kann man hier nicht  if(json[2] !is JSONObject)
        else for (i in 0 until json.length() - 1) array[i] = toObject(json[i + 1] as JSONObject, clazz)
        return array
    }

    /**
     * Reads a value that is an Collection
     * @param o Object that should be parsed
     * @param field Field that should be parsed
     * @return JSONArray representing the Collection
     * @throws Exception
     */
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

    /**
     * Reads a JSONArray and returns the Object it represents
     * @param json JSONArray representing the serialized object
     * @return Collection represented by the JSONArray
     * @throws Exception
     */
    private fun parseCollection(json: JSONArray): Collection<Any> {
        val listType = Class.forName(json[0].toString())
        val typeClass = Class.forName(json[1].toString())
        val collection = getDefaultConstructor(listType).newInstance() as MutableCollection<Any>
        if (json.length() > 2) {
            if (json[2] !is JSONObject) for (i in 0 until json.length() - 2) collection.add(json[i + 2])
            else {
                for (i in 0 until json.length() - 2) collection.add(toObject(json[i + 2] as JSONObject, typeClass))
            }
        }

        return collection
    }

    /**
     * Wrapper class for field access as most fields can't be accessed without setting isAccessible to true
     */
    private fun accessField(field: Field, run: (field: Field) -> Unit) {
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
    private fun getDefaultConstructor(clazz: Class<*>): Constructor<*> {
        for (constructor in clazz.constructors)
            if (constructor.parameters.isEmpty()) return constructor
        throw Exception("Class does not contain a default constructor")
    }

    /**
     * Checks if the class has a valid superclass for classToJson()
     * @param clazz The class
     * @return true if superclass != null and != Object
     */
    private fun hasValidSuperclass(clazz: Class<*>): Boolean {
        val superclass = clazz.superclass
        return superclass != null && superclass != Object().javaClass
    }

    /**
     * Returns true if array is array of primitive types or Strings
     * @return true if array is array of primitive types or Strings
     */
    private fun isPrimitveOrStringArray(array: Array<Any>) = Array<String>::class.java.isAssignableFrom(array.javaClass) || Array<java.lang.Number>::class.java.isAssignableFrom(array.javaClass)

    /**
     * Returns true if field is field of primitive types or Strings
     * @return true if field is field of primitive types or Strings
     */
    private fun isPrimitiveOrString(field: Field) = field.type.isPrimitive || field.type == String().javaClass

    /**
     * Returns true if field is not static and has not the @Ignore annotation
     * @return true if field is not static and has not the @Ignore annotation
     */
    private fun isValidField(field: Field) = !(field.isAnnotationPresent(Ignore::class.java) || Modifier.isStatic(field.modifiers))
}