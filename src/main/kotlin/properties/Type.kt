package properties

import lexer.PositionalException
import token.Token
import token.TokenFactory
import token.statement.Assignment

open class Type(
    val name: String,
    parent: Type?,
    val assignments: MutableList<Assignment>,
    val fileName: String,
    private val exported: Any? = null,
    private var supertype: Type? = null
) :
    Variable(parent), Invokable {
    private val properties = mutableMapOf<String, Property>()
    val functions = mutableListOf<Function>()

    fun getFunction(token: Token) = functions.find { it.name == token.value }
        ?: throw PositionalException("\"$name\" class does not contain \"${token.value}\" function", token)

    fun getResolved(token: Token) =
        properties[token.value] ?: assignments.find { it.left.value == token.value }
        ?: throw PositionalException("unknown property", token)

    fun getProperties() = properties.toMutableMap()
    fun getPropertyOrNull(name: String) = properties[name] ?: assignments.find { it.left.value == name }
    fun getProperty(token: Token) =
        properties[token.value] ?: throw PositionalException("${token.value} not found in $name", token)

    override fun toString(): String {
        return "$name${if (supertype != null) ":${supertype!!.name}" else ""}{parent:${parent ?: "-"}, " +
                "${assignments}${if (exported != null) ",to $exported" else ""}}"
    }

    fun inherits(other: Type): Boolean {
        var type: Type? = this
        while (type != null) {
            if (type.name == other.name && type.fileName == other.fileName)
                return true
            type = type.supertype
        }
        return false
    }

    fun copy(): Type {
        val copy =
            Type(
                name,
                parent?.copy(),
                assignments.map { TokenFactory().copy(it) }.toMutableList() as MutableList<Assignment>,
                fileName,
                this.exported,
                this.supertype
            )
        copy.assignments.forEach { it.parent = copy }
        return copy
    }

    //    fun getFirstUnresolved(token: Token): Pair<Type, String>? {
//        var linkRoot = token
//        var table = symbolTable
//        var type = this
//        while (linkRoot.value == ".") {
//            val nextType = table.getVariableOrNull(linkRoot.left) ?: return Pair(type, linkRoot.left.value)
//            if (nextType !is Type)
//                throw PositionalException("expected class instance, but primitive was found", linkRoot.left)
//            type = nextType
//            table = type.symbolTable
//            linkRoot = linkRoot.right
//        }
//        return null
//    }
    companion object {

//        fun initializeSuperTypes() {
//            for ((pair, token) in superTypes) {
//                val (type, fileName) = pair
//                if (token.value == ".")
//                    SymbolTable.types[type]!![fileName]!!.supertype =
//                        SymbolTable.types[token.right.value]!![token.left.value]
//                else {
//                    val parents = SymbolTable.types[token.value]!!.filter {
//                        SymbolTable.importMap[fileName]?.contains(it.key) ?: false
//                                || it.key == fileName
//                    }
//                    if (parents.isEmpty())
//                        throw PositionalException("no superclass ${token.value} found", token)
//                    if (parents.size > 1)
//                        throw PositionalException(
//                            "superclass ambiguity. There are ${parents.size} applicable supertypes in files ${parents.keys}",
//                            token
//                        )
//                    SymbolTable.types[type]!![fileName]!!.supertype = parents[parents.keys.first()]
//                }
//            }
//        }
    }
}