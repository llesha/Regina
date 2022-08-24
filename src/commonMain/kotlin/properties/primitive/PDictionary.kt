package properties.primitive

import DebugDictionary
import NestableDebug
import References
import elementToDebug
import evaluation.FunctionFactory.getDictionary
import evaluation.FunctionFactory.getIdent
import node.Node
import properties.EmbeddedFunction
import properties.Type
import properties.Variable
import utils.Utils.toProperty
import utils.Utils.toVariable

class PDictionary(value: MutableMap<out Any, out Variable>, parent: Type?, var id: Int) : Primitive(value, parent),
    Indexable,
    NestableDebug {
    override fun getIndex() = 6
    override fun getPValue() = value as MutableMap<Any, Variable>

    override fun get(index: Any, node: Node): Variable {
        return getPValue()[index.toVariable(node)] ?: PInt(0, null)
    }

    override fun set(index: Any, value: Any, nodeIndex: Node, nodeValue: Node) {
        getPValue()[index.toVariable(nodeIndex)] = value.toVariable(nodeValue)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PDictionary) return false
        if (getPValue() == other.getPValue())
            return true
        return false
    }

    override fun hashCode(): Int = getPValue().hashCode()

    override fun toString(): String {
        if (getPValue().isEmpty())
            return "{}"
        val res = StringBuilder("{")
        for ((key, value) in getPValue()) {
            if (key == this)
                res.append("this")
            else res.append(key)
            res.append("=")
            res.append(if (value == this) "this" else value)
            res.append(", ")
        }
        res.deleteAt(res.lastIndex)
        res.deleteAt(res.lastIndex)
        res.append("}")
        return res.toString()
    }

    override fun toDebugClass(references: References): Any {
        val id = getDebugId()
        references.queue.remove(id)
        if (references.dictionaries[id.second] != null)
            return id
        val res = DebugDictionary(getPValue().map {
            when (it.key) {
                is Variable -> if (it.key == this) id else elementToDebug(it.key as Variable, references)
                else -> it.key
            } to if (it.value == this) id else elementToDebug(it.value, references)

        }.toMap().toMutableMap())
        references.dictionaries[id.second as Int] = res
        return id
    }

    override fun getDebugId(): Pair<String, Any> = Pair("dictionary", id)

    override fun checkIndexType(index: Variable): Boolean {
        return true
    }

    companion object {
        fun initializeDictionaryProperties() {
            val p = PDictionary(mutableMapOf(), null, -1)
            setProperty(p, "size") { pr: Primitive ->
                (pr as PDictionary).getPValue().size.toProperty()
            }
            setProperty(p, "keys") { pr: Primitive ->
                (pr as PDictionary).getPValue().keys.toMutableList().toProperty()
            }
            setProperty(p, "values") { pr: Primitive ->
                (pr as PDictionary).getPValue().values.toMutableList().toProperty()
            }
        }

        fun initializeDictionaryFunctions() {
            val p = PDictionary(mutableMapOf(), null, -1)
            setFunction(p, EmbeddedFunction("remove", listOf("key")) { token, args ->
                val dict = getDictionary(token, "this", args)
                val key = getIdent(token, "key", args)
                dict.getPValue().remove(key) ?: PInt(0, null)
            })
        }
    }
}
