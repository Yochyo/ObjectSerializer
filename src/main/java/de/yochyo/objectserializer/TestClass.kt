package de.yochyo.objectserializer

import de.yochyo.objectserializer.annotations.Ignore
import de.yochyo.objectserializer.annotations.Serializeable
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

fun main() {

    val instance = TestClass(1)
    val json = ObjectSerializer.toJSONObject(instance)
    val copy = ObjectSerializer.toObject(json, TestClass::class.java)
    println(json)

}

class TestClass : Superclass, Interface {
    override val interfaceValue = 5
    @Serializeable
    var lambda: () -> Unit = {}
    var NULL: Int? = 1
    var int = 0
    var string = ""
    var intArray = arrayOf<Int>()
    var stringArray = arrayOf<String>()
    var intArrayList: MutableList<Int> = ArrayList<Int>()
    var stringArrayList = ArrayList<String>()
    var intLinkedList = LinkedList<Int>()
    var stringLinkedList = LinkedList<String>()

    val instance = Instance()

    @Ignore
    val ignore = 0

    constructor()
    constructor(ii: Int) {
        NULL = null
        lambda = { println("hello") }
        int = 1
        string = "String"
        intArray = arrayOf(1, 2, 4)
        stringArray = arrayOf("1", "2", "3")
        intArrayList.add(1)
        stringArrayList.add("3")
        intLinkedList.add(1)
        stringLinkedList.add("3")
    }
}

open class Superclass : SuperSuperClass() {
    val superValue = 1
}

open class SuperSuperClass {
    val superSuperValue = 99
}

class Instance {
    val instance = 1
    val string = "String"

    @Ignore
    val ignore = 0
}

interface Interface {
    val interfaceValue: Int
}