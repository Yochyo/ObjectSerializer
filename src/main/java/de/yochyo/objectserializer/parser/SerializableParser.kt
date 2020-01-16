package de.yochyo.objectserializer.parser

import de.yochyo.objectserializer.annotations.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

class SerializableParser : Parser {
    override fun isParseable(o: Any, clazz: Class<*>, flags: Array<String>): Boolean {
        return o is Function<*> || flags.contains(Serializable::class.java.name)
    }

    override fun toJSON(o: Any, clazz: Class<*>): Any {
        val stream = ByteArrayOutputStream()
        val out = ObjectOutputStream(stream)
        out.writeObject(o)
        out.flush()
        val res = Base64.getEncoder().encodeToString(stream.toByteArray())
        stream.close()
        return res
    }

    override fun <E> toObject(json: Any, clazz: Class<E>): E {
        val stream = ByteArrayInputStream(Base64.getDecoder().decode(json as String))
        val `in` = ObjectInputStream(stream)
        val res = `in`.readObject()
        `in`.close()
        return res as E
    }
}