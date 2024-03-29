package properties.primitive

import evaluation.Evaluation.eval
import kotlin.test.Test

class PDictionaryTest {
    @Test
    fun testDictionary() {
        eval(
            """
           fun main() {
                b = {1:2, 2:2}
                print(b)
                print(b.values)
                print("before")
                print(b.keys)
                print(1)
                test(b.values == [2, 2])
                print(2)
                print(b.keys)
                test(b.keys == [1,2])
                a = {}
                a[1] = 2
                a[2] = 3
                print(a[1])
                test(a[1] == 2)
                test(a[2] == 3)
                test(a.size == 2)
                test(a.values == [2,3])
                test(a.remove(1) == 2)
                test(a[1] == null)
                test(a.size == 1)
                print(a.keys)
                test(a.keys == [2])
                test(a.values == [3])
                
           } 
        """
        )
    }
}