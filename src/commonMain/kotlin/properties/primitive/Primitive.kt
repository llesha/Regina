package properties.primitive

import delete.Delete
import isInt
import lexer.NotFoundException
import lexer.PositionalException
import lexer.RuntimeError
import node.Node
import node.invocation.Call
import properties.*

/**
 * Stores Dictionary, Array, String, Int, Double values.
 * 0 : Primitive
 * 1 : Number
 * 2 : Int
 * 3 : Double
 * 4 : String
 * 5 : Array
 * 6 : Dictionary
 *
 * **Array and Dictionary are mutable**, unlike other primitive classes
 */
abstract class Primitive(protected open var value: Any, parent: Type?) : Property(parent) {
    open fun getIndex() = 0
    open fun getPValue() = value
    override fun toString() = "$value"

    fun getSymbol(): String {
        return when (value) {
            is Number -> "(NUMBER)"
            is String -> "(STRING)"
            is MutableList<*> -> "[]"
            else -> throw Exception("unsupported type")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Primitive) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        // var result = super.hashCode()
        //   result = 31 * result + value.hashCode()
        return value.hashCode()
    }

    override fun getPropertyOrNull(name: String) = when (name) {
        "this" -> this
        "parent" -> getParentOrNull()
        "properties" -> getProperties()
        else -> getAllProperties()[getIndex()][name]?.let { it(this) }
            ?: (if (getIndex() in 2..3)
                getAllProperties()[1][name]?.let { it(this) } ?: getAllProperties()[0][name]?.let { it(this) }
            else getAllProperties()[0][name]?.let { it(this) })
    }

    override fun getProperty(node: Node): Property = when (node.value) {
        "this" -> this
        "parent" -> getParentOrNull()
        "properties" -> getProperties()
        else -> getAllProperties()[getIndex()][node.value]?.let { it(this) }
            ?: (if (getIndex() in 2..3)
                getAllProperties()[1][node.value]?.let { it(this) }
                    ?: getAllProperties()[0][node.value]?.let { it(this) }
            else getAllProperties()[0][node.value]?.let { it(this) })
            ?: throw NotFoundException(node, variable = this)
    }

    /**
     * Does not include "this"
     */
    override fun getProperties(): PDictionary {
        val res = getAllProperties()[0].toMutableMap()
        if (getIndex() in 2..3)
            res.putAll(getAllProperties()[1])
        res.putAll(getAllProperties()[getIndex()])
        return PDictionary(res.mapValues { it.value(this) }.toMutableMap(), null, dictionaryId++)
    }

    override fun getFunction(node: Node): RFunction = getFunctionOrNull(node)
        ?: throw PositionalException("${formatClassName()} does not contain function", node)

    override fun getFunctionOrNull(node: Node): RFunction? = RFunction.getFunctionOrNull(
        node as Call,
        if (getIndex() in 2..3)
            (functions[getIndex()] + functions[1]) + functions[0] else functions[getIndex()]
    )

    private fun formatClassName() = this::class.toString().split('.').last().substring(1)

    companion object {
        // used for debugging ids. hashCode() as id will give stack overflow on recursive arrays or dictionaries
        var dictionaryId = 0
        var arrayId = 0

        fun setProperty(primitive: Primitive, name: String, property: (s: Primitive) -> Property) {
            properties[primitive.getIndex()][name] = property
        }

        fun setFunction(primitive: Primitive, embeddedFunction: EmbeddedFunction) {
            functions[primitive.getIndex()].add(embeddedFunction)
        }

        private val properties = List(7) { mutableMapOf<String, (s: Primitive) -> Property>() }
        fun getAllProperties() = properties.map { it.toMutableMap() }
        private val functions = List(7) { mutableSetOf<RFunction>() }
        fun createPrimitive(value: Any, parent: Type? = null, node: Node = Node()): Primitive {
            return when (value) {
                is String -> PString(value, parent)
                is List<*> -> PArray(value as MutableList<Variable>, parent, arrayId++)
                is Number -> {
                    if (isInt(value))
                        PInt(value.toInt(), parent)
                    else PDouble(value.toDouble(), parent)
                }
                is MutableMap<*, *> -> PDictionary(value as MutableMap<out Any, out Variable>, parent, dictionaryId++)
                else -> throw PositionalException(
                    "Cannot create variable of type `${value::class}`", node
                )
            }
        }

        fun createPrimitive(value: Any, parent: Type? = null, delete: Delete): Primitive {
            return when (value) {
                is String -> PString(value, parent)
                is List<*> -> PArray(value as MutableList<Variable>, parent, arrayId++)
                is Number -> {
                    if (isInt(value))
                        PInt(value as Int, parent)
                    else PDouble(value as Double, parent)
                }
                is MutableMap<*, *> -> PDictionary(value as MutableMap<out Any, out Variable>, parent, dictionaryId++)
                else -> throw RuntimeError(
                    "Cannot create variable of type `${value::class}`", delete
                )
            }
        }
    }
}