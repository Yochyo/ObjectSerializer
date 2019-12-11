package de.yochyo.objectserializer.parser

import org.json.JSONObject
import java.lang.Exception

interface Parser {
    companion object{
        private val parsers = ArrayList<Parser>()
        init{
            parsers += SerializeableParser()
            parsers += PrimitiveOrStringParser()
            parsers += ArrayParser()
            parsers += CollectionParser()
        }
        fun toJSON(o: Any, clazz: Class<*>, annotations: Array<Annotation> = emptyArray()): Any{
            for(parser in parsers){
                if(parser.isParseable(o, clazz, annotations)) return parser.toJSON(o, clazz)
            }
            throw Exception("object $o of type ${clazz.name} could not be parsed")
        }
        fun <E> toObject(o: Any, clazz: Class<E>, annotations: Array<Annotation> = emptyArray()): E{
            for(parser in parsers){
                if(parser.isParseable(o, clazz, annotations)) return parser.toObject(o, clazz)
            }
            throw Exception("object $o of type ${clazz.name} could not be parsed")
        }
    }

    /**
     * @param object that should be checked
     * @param clazz of the object
     */
    fun isParseable(o: Any, clazz: Class<*>, annotations: Array<Annotation>): Boolean
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