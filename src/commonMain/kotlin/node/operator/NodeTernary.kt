package node.operator

import lexer.PositionalException
import node.Node
import table.SymbolTable

class NodeTernary(node: Node) :
    Node(node.symbol, node.value, node.position, node.children) {

    override fun evaluate(symbolTable: SymbolTable): Any {
        if (children.size != 3)
            throw PositionalException("ternary if should have else branch", this)
        val condition = evaluateCondition(symbolTable)
        return if (condition != 0)
            right.evaluate(symbolTable)
        else children[2].evaluate(symbolTable)
    }

    fun evaluateCondition(symbolTable: SymbolTable): Any =
        left.evaluate(symbolTable)
}