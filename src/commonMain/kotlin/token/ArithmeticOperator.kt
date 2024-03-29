package token

import lexer.Parser
import node.Node
import node.operator.NumberOperator

class ArithmeticOperator(
    symbol: String,
    value: String,
    position: Pair<Int, Int>,
    bindingPower: Int,
    nud: ((node: Token, parser: Parser) -> Token)?,
    led: (
        (
            node: Token, parser: Parser, node2: Token
        ) -> Token
    )?,
    std: ((node: Token, parser: Parser) -> Token)?
) : Operator(symbol, value, position, bindingPower, nud, led, std) {
    override fun toNode(filePath: String): Node {
        return NumberOperator(symbol, value, position, children.map { it.toNode(filePath) })
    }
}
