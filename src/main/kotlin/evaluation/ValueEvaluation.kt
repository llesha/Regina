package evaluation

import evaluation.Evaluation.evaluateInvocation
import evaluation.FunctionEvaluation.toVariable
import lexer.PositionalException
import lexer.Token
import properties.primitive.Primitive
import structure.SymbolTable.Type
import structure.*

object ValueEvaluation {
    fun evaluateValue(token: Token, symbolTable: SymbolTable): Any {
        return when (token.symbol) {
            "." -> evaluateLink(token, symbolTable)
            "(IDENT)" -> evaluateIdentifier(token, symbolTable)
            "(NUMBER)" -> if (token.value.contains(".")) token.value.toDouble() else token.value.toInt()
            "(STRING)" -> token.value
            "(" -> {
                val res = evaluateInvocation(token, symbolTable)
                if (res is Unit)
                    throw PositionalException("expected value but nothing was returned from function", token)
                res
            }
            "[" -> {
                val element = evaluateIndex(token, symbolTable)
                if (element is Primitive)
                    return element.value
                return element
            }
            "true" -> 1
            "false" -> 0
            "!" -> evaluateNot(token, symbolTable)
            "[]" -> token.children.map { evaluateValue(it, symbolTable).toVariable(it) }.toMutableList()
            "if" -> evaluateTernary(token, symbolTable)
            "+" -> evaluateValue(token.left, symbolTable) + evaluateValue(token.right, symbolTable)
            "==" -> evaluateValue(token.left, symbolTable).eq(
                evaluateValue(
                    token.right,
                    symbolTable
                )
            ).toInt()
            "!=" -> evaluateValue(token.left, symbolTable).neq(
                evaluateValue(
                    token.right,
                    symbolTable
                )
            ).toInt()
            "is" -> evaluateTypeCheck(token, symbolTable).toInt()
            "!is" -> (!evaluateTypeCheck(token, symbolTable)).toInt()
            else -> evaluateInfixArithmetic(token, symbolTable)
        }
    }

    private fun evaluateTypeCheck(token: Token, symbolTable: SymbolTable): Boolean {
        val checked = evaluateValue(token.left, symbolTable)
        val type = evaluateValue(token.right, symbolTable)
        if (checked is Type && type is Type
            && checked.assignments.isEmpty()
            && type.symbolTable.isEmpty()
        )
            return checked.typeName == type.typeName
        throw PositionalException("expected class instance as left operator and class name as right operator", token)
    }

    fun evaluateLink(token: Token, symbolTable: SymbolTable): Any {
        var linkRoot = token
        var table = symbolTable
        while (linkRoot.value == ".") {
            val type = evaluateValue(linkRoot.left, table)
            if (type !is Type)
                throw PositionalException("expected class", linkRoot.left)
            linkRoot = linkRoot.right
            table = type.symbolTable
        }
        return evaluateValue(linkRoot, table)
    }

    private fun evaluateIdentifier(token: Token, symbolTable: SymbolTable): Any {
        val identifier = symbolTable.getIdentifier(token)
        return if (identifier is Primitive)
            identifier.value
        else (identifier as Type)
    }

    fun evaluateIndex(token: Token, symbolTable: SymbolTable): Any {
        val array = evaluateValue(token.left, symbolTable)
        val index = evaluateValue(token.right, symbolTable)
        if (index is Int) {
            println(array)
            return when (array) {
                is MutableList<*> -> if (index < array.size) array[index]!!
                else throw PositionalException("index $index out of bounds for array of size ${array.size}", token)
                is String -> if (index < array.length) array[index].toString()
                else throw PositionalException("index $index out of bounds for string of length ${array.length}", token)
                else -> throw PositionalException("array or string expected", token)
            }
        } else throw PositionalException("expected Int as index", token)
    }

    private fun evaluateInfixArithmetic(token: Token, symbolTable: SymbolTable): Number {
        if (token.children.size == 1) {
            val a = evaluateValue(token.left, symbolTable)
            return when (token.symbol) {
                "-" -> evaluateUnaryMinus(a as Number)
                else -> throw PositionalException("no such prefix operator", token)
            }
        }
        if (token.children.size == 2) {
            val (a, b) = unifyNumbers(
                evaluateValue(token.left, symbolTable),
                evaluateValue(token.right, symbolTable),
                token
            )
            return when (token.symbol) {
                ">" -> (a.toDouble() > b.toDouble()).toInt()
                "<" -> (a.toDouble() < b.toDouble()).toInt()
                ">=" -> (a.toDouble() >= b.toDouble()).toInt()
                "<=" -> (a.toDouble() <= b.toDouble()).toInt()
                "&" -> (a != 0 && b != 0).toInt()
                "|" -> (a != 0 || b != 0).toInt()
                // never happens, because // is for comments
                "//" -> a.toInt() / b.toInt()
                else -> evaluateDuplicatedOperators(a, b, token)
            }
        } else throw PositionalException("expected infix operator", token)
    }

    private fun evaluateDuplicatedOperators(first: Number, second: Number, token: Token): Number {
        if (first is Double) {
            val a = first.toDouble()
            val b = second.toDouble()
            return when (token.symbol) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                "%" -> a % b
                else -> throw PositionalException("operator ${token.symbol} not implemented", token)
            }
        } else {
            val a = first.toInt()
            val b = second.toInt()
            return when (token.symbol) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                "%" -> a % b
                else -> throw PositionalException("operator ${token.symbol} not implemented", token)
            }
        }
    }

    private fun unifyNumbers(first: Any, second: Any, token: Token): List<Number> {
        if (first !is Number)
            throw PositionalException("left operand is not numeric for this infix operator", token)
        if (second !is Number)
            throw PositionalException("right operand is not numeric for this infix operator", token)
        if (first is Int && second is Int)
            return listOf(first, second)
        return listOf(first.toDouble(), second.toDouble())
    }

    private fun evaluateUnaryMinus(number: Number): Number = if (number is Double) -number else -(number as Int)

    private fun evaluateTernary(token: Token, symbolTable: SymbolTable): Any {
        if (token.children.size != 3)
            throw PositionalException("ternary if should have else branch", token)
        return if (evaluateValue(token.left, symbolTable) != 0)
            evaluateValue(token.right, symbolTable)
        else evaluateValue(token.children[2], symbolTable)
    }

    private operator fun Any.plus(other: Any): Any {
        if (this is MutableList<*>) {
            return if (other is MutableList<*>) {
                val res = this.toMutableList()
                res.addAll(other)
                res
            } else {
                val res = this.toMutableList()
                res.add(other)
                res
            }
        }
        if (this is String || other is String)
            return this.toString() + other.toString()
        if (this is Double && other is Number || this is Number && other is Double)
            return this.toString().toDouble() + other.toString().toDouble()
        if (this is Int && other is Int)
            return this + other
        else throw Exception("operator not applicable to operands")
    }

    private fun evaluateNot(token: Token, symbolTable: SymbolTable): Int {
        val res = (evaluateValue(token.left, symbolTable))
        if (res is Number)
            return (res == 0).toInt()
        throw PositionalException("! operator applicable to numeric", token)
    }

    private fun Any.eq(other: Any): Boolean {
        if (this is Number && other is Number)
            return this.toDouble() == other.toDouble()
        if (this is MutableList<*> && other is MutableList<*>) {
            var res = true
            this.forEachIndexed { index, _ ->
                if (!this[index]!!.eq(other[index]!!)) {
                    res = false
                }
            }
            return res
        }
        return this == other
    }

    private fun Any.neq(other: Any) = !this.eq(other)

    fun Boolean.toInt(): Int = if (this) 1 else 0
    fun Any.toBoolean(token: Token): Boolean {
        try {
            return this.toString().toDouble() != 0.0
        } catch (e: NumberFormatException) {
            throw PositionalException("expected numeric value", token)
        }
    }
}


