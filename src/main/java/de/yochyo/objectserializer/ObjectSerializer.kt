package de.yochyo.objectserializer

import de.yochyo.objectserializer.annotations.Serializeable
import de.yochyo.objectserializer.parser.Parser
import org.json.JSONObject

object ObjectSerializer {
    /**
     * Takes an object an parses it to a JSONObject. It will ignore fields with the @Ignore Annotation.
     * Lambdas stored in variable or other Serializable objects
     * can be serialized if they have the @Serializeable annotation.
     * @param o Any not-null object
     * @return JSONObject containing all fields of o
     */
    fun toJSONObject(o: Any): JSONObject = Parser.toJSON(o, o.javaClass) as JSONObject

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
    fun <E> toObject(json: JSONObject, clazz: Class<E>) = Parser.toObject(json, clazz)

    private val serializeableArray = arrayOf(Serializeable::class.java.constructors.first().newInstance() as Annotation)
    fun serializeableToJSONObject(o: Any) = Parser.toJSON(o, o.javaClass, serializeableArray)
    fun <E> serializeableToObject(o: Any, clazz: Class<E>) = Parser.toObject(o, clazz, serializeableArray)
}