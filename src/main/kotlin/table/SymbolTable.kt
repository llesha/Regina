package table

import evaluation.FunctionEvaluation.initializeEmbedded
import lexer.PositionalException
import properties.*
import properties.Function
import token.Token
import utils.Utils.toVariable

class SymbolTable(
    // during recursive evaluation multiple symbol tables are used, hence need different scopes, files and types
    private var scopeTable: ScopeTable? = ScopeTable(),
    private var variableTable: Variable? = null,
    private var fileTable: FileTable = FileTable("")
) {
    companion object {
        private val imports = mutableMapOf<FileTable, MutableSet<FileTable>>()

        //private val embedded: MutableMap<String, Function> = initializeEmbedded()
        private val globalFile = initializeGlobal()

        private fun initializeGlobal(): FileTable {
            val res = FileTable("@global")
            for (i in initializeEmbedded())
                res.addFunction(i.value)
            imports[res] = mutableSetOf()
            return res
        }

        fun getEmbeddedNames(): MutableSet<String> = globalFile.getFunctionNames()
    }

    private fun checkImports(check: (table: FileTable) -> Any?): List<Any> {
        val suitable = mutableListOf<Any>()
        if(imports[fileTable]==null)
            println(fileTable)
        for (table in imports[fileTable]!!) {
            val fromFile = check(table)
            if (fromFile != null)
                suitable.add(fromFile)
        }
        return suitable
    }

    private fun getListFromFiles(getValue: (table: FileTable) -> Any?): List<Any> {
        val fromCurrent = getValue(fileTable)
        if (fromCurrent != null)
            return listOf(fromCurrent)
        return checkImports(getValue)
    }

    private fun getFromFilesOrNull(getValue: (table: FileTable) -> Any?): Any? {
        val valuesList = getListFromFiles(getValue)
        return if (valuesList.size == 1)
            valuesList.first()
        else null
    }

    private fun getFromFiles(token: Token, getValue: (table: FileTable) -> Any?): Any {
        val valuesList = getListFromFiles(getValue)
        if (valuesList.size >= 2)
            throw PositionalException("Import ambiguity. `${token.value}` found in $valuesList", token)
        if (valuesList.isEmpty())
            throw PositionalException("`${token.value}` not found", token)
        return valuesList.first()
    }

    fun getFileOfValue(token: Token, getValue: (table: FileTable) -> Any?): FileTable {
        val inCurrent = getValue(fileTable)
        if (inCurrent != null)
            return fileTable
        val suitable = mutableListOf<FileTable>()
        for (table in imports[fileTable]!!) {
            val fromFile = getValue(table)
            if (fromFile != null)
                suitable.add(table)
        }
        when (suitable.size) {
            0 -> throw PositionalException("File with `${token.value}` not found", token)
            1 -> return suitable.first()
            else -> throw PositionalException("`${token.value}` is found in files: $suitable. Specify file.", token)
        }
    }

    fun getFileTable() = fileTable

    fun changeScope(): SymbolTable {
        return SymbolTable(variableTable = variableTable, fileTable = fileTable)
    }

    fun changeScope(scopeTable: ScopeTable?): SymbolTable {
        return SymbolTable(scopeTable = scopeTable?.copy(), variableTable = variableTable, fileTable = fileTable)
    }

    fun getScope() = scopeTable

    fun changeFile(fileName: String): SymbolTable {
        return SymbolTable(scopeTable?.copy(), variableTable, imports.keys.find { it.fileName == fileName }
            ?: throw PositionalException("File not found"))
    }

    // TODO dangerous if type is in different file
    fun changeVariable(type: Variable) = SymbolTable(scopeTable?.copy(), type, fileTable)

    fun addFile(fileName: String): Boolean {
        if (imports[FileTable(fileName)] == null) {
            imports[FileTable(fileName)] = mutableSetOf(globalFile)
            return true
        }
        return false
    }

    fun addImport(fileName: Token) {
        imports[fileTable]!!.add(imports.keys.find { it.fileName == fileName.value }!!)
    }

    fun addType(token: Token) = fileTable.addType(token)
    fun addFunction(function: Function) = fileTable.addFunction(function)
    fun addObject(token: Token) = fileTable.addObject(token)
    fun addVariable(name: String, value: Variable) = scopeTable!!.addVariable(name, value)


    fun getImportOrNull(fileName: String) = imports[fileTable]!!.find { it.fileName == fileName }
    fun getType(token: Token): Type =
        getFromFiles(token) { it.getTypeOrNull(token.value)?.copy() ?: throw PositionalException("EF",token) } as Type? ?: throw PositionalException(
            "Type `${token.value}` not found",
            token
        )

    fun getFunction(token: Token): Function {
        val res = getFromFilesOrNull { it.getFunctionOrNull(token.value) } as Function?
        if (res == null) {
            if (variableTable == null) throw PositionalException("Function `${token.value}` not found", token)
            return variableTable!!.getFunction(token)
        }
        return res
    }

    fun getObjectOrNull(token: Token): Object? = getFromFilesOrNull { it.getObjectOrNull(token.value) } as Object?
    fun getTypeOrNull(token: Token): Type? = getFromFilesOrNull { it.getTypeOrNull(token.value) } as Type?
    fun getFunctionOrNull(token: Token): Function? =
        getFromFilesOrNull { it.getFunctionOrNull(token.value) } as Function?

    fun getMain(): Function {
        val mains = getFromFilesOrNull { it.getFunctionOrNull("main") }
            ?: throw PositionalException("no main functions found")
        return fileTable.getFunctionOrNull("main") ?: throw PositionalException("no main function in current file")
    }

    fun getVariable(token: Token) = scopeTable!!.getVariable(token)
    fun getVariable(name: String) = scopeTable!!.getVariable(name)
    fun getVariableOrNull(name: String): Variable? = scopeTable!!.getVariableOrNull(name)
    fun getIdentifier(token: Token): Variable {
        val variable = scopeTable?.getVariableOrNull(token.value)
        if (variable != null)
            return variable
        val property = variableTable?.getPropertyOrNull(token.value)
        if (property != null)
            return property
        val type = getTypeOrNull(token)
        if (type != null)
            return type
        return getObjectOrNull(token) ?: throw PositionalException(
            "Identifier `${token.value}` not found in `$fileTable`",
            token
        )
    }

    fun getCurrentType() = variableTable

    fun getTypes(): MutableMap<String, List<Type>> {
        val res = mutableMapOf<String, List<Type>>()
        for (i in imports.keys)
            res[i.fileName] = i.getTypes()
        return res
    }

    fun getProperty(token: Token): Property {
        return variableTable!!.getProperty(token)
    }

    fun getPropertyOrNull(name: String): Property? {
        return variableTable?.getPropertyOrNull(name)
    }


    fun copy() = SymbolTable(scopeTable?.copy() ?: ScopeTable(), variableTable, fileTable)
    fun addVariableOrNot(token: Token) = scopeTable?.addVariable(token.value, "".toVariable(token))

    override fun toString(): String {
        val res = StringBuilder()
        res.append(globalFile.stringNotation())
        for (i in imports.keys) {
            res.append("\n")
            res.append(i.stringNotation())
            if (imports[i]?.isNotEmpty() == true)
                res.append("\n\timports: ${imports[i]!!.joinToString(separator = ",")}\n")
        }
        return res.toString()
    }
}