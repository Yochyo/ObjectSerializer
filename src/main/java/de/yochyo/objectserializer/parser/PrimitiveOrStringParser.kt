package de.yochyo.objectserializer.parser

class PrimitiveOrStringParser : Parser{
    override fun isParseable(o: Any, clazz: Class<*>, annotations: Array<Annotation>): Boolean {
        return clazz.isPrimitive || java.lang.Number::class.java.isAssignableFrom(clazz) || o is String
    }

    override fun toJSON(o: Any, clazz: Class<*>): Any {
        return o
    }

    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        return json as E
    }
}