package lexer

import structure.Assignment
import structure.Type
import structure.Property
import java.util.*


/**
 * Do BFS for each NodeDeclaration children
 */
class InstanceTreeBuilder(private val root: Type, isObject: Boolean = false) {
    init {
        resolveTree()
    }

    fun resolveTree() {
        do {
            val current = bfs(root) ?: break
            processNode(current)
        } while (true)
    }

    fun evaluateProperty(assignment: Assignment){

    }

    /**
     * Find unresolved assignments
     */
    private fun bfs(root: Type): Assignment? {
        val stack = Stack<Type>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            if (current.assignments.isNotEmpty())
                return current.assignments.first()

            val containers = current.properties.filterIsInstance<Type>()
            stack.addAll(containers)
        }

        return null
    }

    private fun processNode(starting: Assignment) {
        val stack = Stack<Assignment>()
        stack.push(starting)

        while (stack.isNotEmpty()) {
            val current = stack.peek()
            // evaluate assignment into node
            if (current.canEvaluate()) {
                val node = current.evaluate()
                current.parent.properties.add(node)
                current.parent.assignments.remove(current)
                stack.pop()
            } else
                processChain(current, stack)
        }
    }

    private fun processChain(current: Assignment, stack: Stack<Assignment>) {
        var linkList = current.getLink()
        var parent = current.parent
        while (linkList.isNotEmpty()) {
            val notResolved = parent.assignments.find { it.name == linkList[0] }
            if (notResolved != null) {
                stack.push(notResolved)
                break
            } else if (parent.properties.find { it.name == linkList[0] } != null) {
                val smth = parent.properties.find { it.name == linkList[0] }
                // take value from existing node
                if (smth is Property) {
                    current.replaceFirst(smth.value)
                    // stack.push(current)
                    break
                    // go one node deeper on chain
                } else {
                    parent = smth as Type
                    linkList = linkList.drop(1)
                }
            } else if (linkList[0] == "parent") {
                parent = parent.parent!!
                linkList = linkList.drop(1)
            } else throw PositionalException(
                "no property with name ${linkList[0]} in ${parent.name}",
                current.token
            )
        }
    }
}