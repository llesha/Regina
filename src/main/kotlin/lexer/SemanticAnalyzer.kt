package lexer

import Logger
import evaluation.Evaluation.globalTable
import evaluation.FunctionEvaluation
import readFile
import table.SymbolTable
import token.Token
import token.TokenFactory.Companion.createSpecificIdentifierFromInvocation
import java.util.*

class SemanticAnalyzer(private val fileName: String, private val tokens: List<Token>) {

    fun analyze(): List<Token> {
        createAssociations(tokens, fileName)
        // TODO do the same for all imports
        changeIdentTokens()
        return tokens
    }

    private fun createAssociations(tokens: List<Token>, fileName: String) {
        globalTable.addFile(fileName)
        globalTable = globalTable.changeFile(fileName)

        val queue = ArrayDeque<Pair<Token, String>>()
        queue.addAll(tokens.map { Pair(it, fileName) })
        while (queue.isNotEmpty()) {
            val (token, currentFileName) = queue.pop()
            when (token.symbol) {
                "fun" -> globalTable.addFunction(FunctionEvaluation.createFunction(token))
                "class" -> {
                    globalTable.addType(token)
                }
                "object" -> {
                    globalTable.addObject(token)
                }
                "import" -> {
                    if (globalTable.getImportOrNull(token.left.value) == null) {
                        globalTable.addFile(token.left.value)
                        globalTable.addImport(token.left)
                        createAssociations(readFile(token.left.value + ".redi"), token.left.value)
                        //queue.addAll(readFile(tokenPath = token.left).map { Pair(it, token.left.value) })
                    } else Logger.addWarning(token.left, "Same import found above")
                }
                else -> throw PositionalException("class or function can be top level declaration", token)
            }
        }
        // TODO implement
        // initializeSuperTypes()
    }

    private fun changeIdentTokens() {
        for (token in tokens)
            changeTokenType(token)
    }

    private fun changeTokenType(token: Token) {
        for ((index, child) in token.children.withIndex()) {
            when (child.symbol) {
                "(" -> {
                    if (token.value != "fun")
                        token.children[index] = createSpecificIdentifierFromInvocation(child, globalTable)
                }
                // "[]" -> token.children[index] = TokenArray(child)
                //"[" -> token.children[index] = TokenIndexing(child)
            }
            changeTokenType(token.children[index])
        }
    }

    private fun checkIntersections(tokens: List<Token>) {
        val classes = mutableSetOf<String>()
        val functions = SymbolTable.getEmbeddedNames()
        for (token in tokens) {
            when (token.symbol) {
                "class" -> {
                    classes.add(getSupertype((getExport(token.left))).value)
                }
                "fun" -> {
                    val added = functions.add(token.left.left.value)
                    if (!added)
                        throw  PositionalException(
                            if (SymbolTable.getEmbeddedNames()
                                    .contains(functions.last())
                            ) "reserved function name" else "same function name within one file", token.left.left
                        )
                    checkParams(token.left.children.subList(1, token.left.children.size))
                }
            }
        }
        val intersections = classes.intersect(functions)
        if (intersections.isNotEmpty())
            throw PositionalException("$fileName contains functions and classes with same names: $intersections")
    }

    private fun getExport(token: Token): Token {
        return if (token.value == "export")
            token.left
        else token
    }

    private fun getSupertype(token: Token): Token {
        return if (token.value == ":")
            token.left
        else token
    }

    private fun checkParams(params: List<Token>) {
        for (param in params)
            if (param.symbol != "(IDENT)") throw PositionalException("expected identifier as function parameter", param)
    }

    /**
     * constructor params are assignments, because of the dynamic structure of type
     */
    private fun checkConstructorParams(params: List<Token>) {
        for (param in params)
            if (param.value != "=") throw PositionalException(
                "expected assignment as constructor parameter",
                param
            )
    }
}