import evaluation.Evaluation.eval
import kotlin.test.Test

class DefaultTest {
    @Test
    fun testTypeFunction() {
        eval(
            """
            fun main() {
            test(type(A()) == A)
            }
            class A {}
        """
        )
    }

    @Test
    fun testDebug() {
        eval(
            """
          fun main() {/*w
{
e*/
    b = A()
    print(b)
    print(b.properties)
    t = []
    d = [1, {"abc":"cde"}]
    d.add(d)
    c =  {b:"S", t:d, "a":"b"}
    c[1] = c
    o = Obj
    f = [c, 1, 2]
    #stop
    print("a " + 1)
}

class A {
    n = if(iter < 5)  A() else B()
    iter =  (parent?.iter ?? 0) + 1
}

class B {
    p1 = p2 + 5
    p2 = 3
}

fun C() {
    q = 1
}

object Obj {
    objProp = 1
}
        """
        )
    }

    @Test
    fun testIs() {
        eval("""
            fun main() {
                test((1 !is Class) || 0) 
            }
        """)
    }
}