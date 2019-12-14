package de.yochyo.objectserializer.parser

import de.yochyo.objectserializer.utils.Utils
import org.json.JSONArray

class CollectionParser : Parser {
    override fun isParseable(o: Any, clazz: Class<*>, flags: Array<String>): Boolean {
        return Collection::class.java.isAssignableFrom(clazz)
    }

    override fun toJSON(o: Any, clazz: Class<*>): Any {
        val json = JSONArray()
        val array = o as Collection<Any>
        json.put(array.javaClass.typeName)
        if (array.isNotEmpty()) {
            json.put(array.first().javaClass.name)
            array.forEach { json.put(Parser.toJSON(it, it::class.java)) }
        } else json.put("java.lang.Object")
        return json
    }

    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        val json = json as JSONArray
        val listType = Class.forName(json[0].toString())
        val typeClass = Class.forName(json[1].toString())
        val collection = Utils.getDefaultConstructor(listType).newInstance() as MutableCollection<Any>
        if (json.length() > 2) {
            for (i in 0 until json.length() - 2)
                collection.add(Parser.toObject(json[i + 2], typeClass) as Any)
        }

        return collection as E
    }
}