package de.yochyo.objectserializer.parser

import org.json.JSONArray

class ArrayParser : Parser {
    override fun isParseable(o: Any, clazz: Class<*>, annotations: Array<Annotation>): Boolean {
        return clazz.isArray
    }

    override fun toJSON(o: Any, clazz: Class<*>): Any {
        val json = JSONArray()
        val array = o as Array<Any>
        array.forEach { json.put(Parser.toJSON(it, it::class.java)) }
        return json
    }

    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        val json = json as JSONArray
        val clazz = Class.forName(clazz.name.substring(2, clazz.name.length - 1))
        val array = java.lang.reflect.Array.newInstance(clazz, json.length()) as Array<Any>
        for (i in 0 until json.length()) array[i] = Parser.toObject(json[i], clazz)
        return array as E
    }
}