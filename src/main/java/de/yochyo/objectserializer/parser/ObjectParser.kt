package de.yochyo.objectserializer.parser

import de.yochyo.objectserializer.utils.Utils
import de.yochyo.objectserializer.utils.Utils.toFlags
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Constructor

class ObjectParser : Parser{
    override fun isParseable(o: Any, clazz: Class<*>, flags: Array<String>): Boolean {
        return true
    }
    /**
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation.
     * Lambdas stored in variable or other Serializable objects
     * can be serialized if they have the @Serializeable annotation.
     * @param o Any not-null object
     * @param clazz class of the Object
     * @return JSONObject containing all fields of o
     */
    override fun toJSON(o: Any, clazz: Class<*>): Any {
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
    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        val defaultConstructor = Utils.getDefaultConstructor(clazz) as Constructor<E>
        val o = defaultConstructor.newInstance()
        return jsonToClass(o, clazz, json as JSONObject)
    }

    /**
    *  Takes an Object and parses it to a JSONObject
    *  @param o Object that should be parsed
    *  @param clazz Class whose fields should be read (needed for iterating through superclasses)
    *  @return JSONObject representing Object o
    *  @throws Exception
    */
    private fun classToJson(o: Any, clazz: Class<*>, json: JSONObject): JSONObject {
        val nullFields = JSONArray()
        for (field in clazz.declaredFields)
            Utils.accessField(field) {
                if (Utils.isValidField(field))
                    if (field.get(o) == null) nullFields.put(field.name)
                    else json.put(field.name, Parser.toJSON(field.get(o), field.type, field.annotations.toFlags()))
            }

        if (!nullFields.isEmpty) json.put("null", nullFields)
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
        val nullFields = if (json.has("null")) json.getJSONArray("null") else JSONArray()

        for (field in clazz.declaredFields) {
            Utils.accessField(field) {
                if (Utils.isValidField(field)) {
                    if (nullFields.contains(field.name)) field.set(o, null)
                    else field.set(o, Parser.toObject(json[field.name], field.type, field.annotations.toFlags()))
                }
            }
        }
        if (Utils.hasValidSuperclass(clazz)) jsonToClass(o, clazz.superclass, json["super"] as JSONObject)
        return o
    }
}