package de.yochyo.objectserializer.parser

interface Parser {
    companion object {
        private val parsers = ArrayList<Parser>()

        init {
            parsers += SerializableParser()
            parsers += PrimitiveOrStringParser()
            parsers += ArrayParser()
            parsers += CollectionParser()
            parsers += ObjectParser()
        }

        fun toJSON(o: Any, clazz: Class<*>, flags: Array<String> = emptyArray()): Any {
            for (parser in parsers) {
                if (parser.isParseable(o, clazz, flags)) return parser.toJSON(o, clazz)
            }
            throw Exception("object $o of type ${clazz.name} could not be parsed")
        }

        fun <E> toObject(o: Any, clazz: Class<E>, flags: Array<String> = emptyArray()): E {
            for (parser in parsers) {
                if (parser.isParseable(o, clazz, flags)) return parser.toObject(o, clazz)
            }
            throw Exception("object $o of type ${clazz.name} could not be parsed")
        }
    }

    /**
     * @param object that should be checked
     * @param clazz of the object
     */
    fun isParseable(o: Any, clazz: Class<*>, flags: Array<String>): Boolean

    /**
     * Parses an Object to a JSON compatible format
     * @param object that should be parsed
     * @param clazz class of the object
     * @return Value that is stored in a JSONObject
     */
    fun toJSON(o: Any, clazz: Class<*>): Any

    /**
     * Parses a JSON value to an Object
     * @param json JSON value representing the Object
     * @param clazz class that should be returned
     * @return Object of type E
     */
    fun <E> toObject(json: Any, clazz: Class<E>): E
}