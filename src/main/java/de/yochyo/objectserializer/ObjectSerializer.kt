package de.yochyo.objectserializer

import de.yochyo.objectserializer.annotations.Ignore
import de.yochyo.objectserializer.parser.Parser
import de.yochyo.objectserializer.utils.Utils
import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier

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
        val defaultConstructor = Utils.getDefaultConstructor(clazz) as Constructor<E>
        val o = defaultConstructor.newInstance()
        return jsonToClass(o, clazz, json)
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
            Utils.accessField(field) {
                if (isValidField(field))
                    json.put(field.name, Parser.toJSON(field.get(o), field.type, field.annotations))
            }

        if (Utils.hasValidSuperclass(clazz)) json.put("super", classToJson(o, clazz.superclass, JSONObject()))
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
            Utils.accessField(field) {
                if (isValidField(field))
                    Utils.accessField(field) { field.set(o, Parser.toObject(json[field.name], field.type, field.annotations)) }
            }
        }
        if (Utils.hasValidSuperclass(clazz)) jsonToClass(o, clazz.superclass, json["super"] as JSONObject)
        return o
    }
    fun isValidField(field: Field) = !(field.isAnnotationPresent(Ignore::class.java) || Modifier.isStatic(field.modifiers))
}