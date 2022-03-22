package lexer

class PositionalException(
    private val errorMessage: String,
    private val token: Token = Token(),
    private val position: Pair<Int, Int> = Pair(0,0),
    private val length: Int = 1
) : Exception() {
    override val message: String
        get() = if (token.value != "")
            "$errorMessage at ${token.position.second},${token.position.first}-${token.position.first + token.value.length}"
        else "$errorMessage at ${position.second},${position.first}-${position.first + length}"
}