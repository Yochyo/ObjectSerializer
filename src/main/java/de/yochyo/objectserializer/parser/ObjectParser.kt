package de.yochyo.objectserializer.parser

import de.yochyo.objectserializer.ObjectSerializer
import org.json.JSONObject

class ObjectParser : Parser{
    override fun isParseable(o: Any, clazz: Class<*>, annotations: Array<Annotation>): Boolean {
        return true
    }

    override fun toJSON(o: Any, clazz: Class<*>): Any {
        return ObjectSerializer.toJSONObject(o)
    }

    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        return ObjectSerializer.toObject(json as JSONObject, clazz)
    }
}