package structure

import lexer.FunctionEvaluation
import lexer.Token
import lexer.ValueEvaluation

class Assignment(val token: Token) {
    val name: String get() = token.children[0].symbol

    // should be val, but no way to do it
    lateinit var parent: Type

    fun canEvaluate(): Boolean = token.find(".") == null

    fun getLink(): List<String> {
        val link = token.find(".")!!
        return link.value.split('.')
    }

    fun replaceFirst(value: Any) {
        // TODO think whether first encountered link is the right one in all cases
        //token.findValue(){
        token.find(".")!!.value = value.toString()
    }


    fun evaluate(): Node {
        val value = ValueEvaluation.evaluateValue(
            token.children[1],
            SymbolTable((TypeManager.types) as (MutableList<Node>), FunctionEvaluation.functions)
        )
        if (value is String)
            return TypeManager.find(value) ?: Property(name, value, parent)
        // number or array
        return Property(name, value, parent)
    }
}