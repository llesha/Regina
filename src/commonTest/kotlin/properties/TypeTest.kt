package properties

import evaluation.Evaluation.eval
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TypeTest {
    @Test
    fun testBidirectionalPropertyResolving() {
        eval(
            """
           fun main() {
                a = A()
                print("Built")
                print(a.str())
           }
           class A {
                iter = (parent?.iter ?? 0) + 1
                next = if(iter < 1) A() else 0
                fromNext = if(next == 0) -1 else next.fromNext - 1
              
              fun str() {
                  return "\n" + iter + " " + fromNext + (if(next == 0) "" else next.str()) 
              }
              
              fun withCycle() {
                while(1) {
                    a.b.c
                }
              }
           }
        """
        )
    }

    @Test
    fun noProperty() {
        eval(
            """
            fun main() {
                q = A()
                print(q?.a)
                q.a = 1
                test(q.a == 1)
            }
            
            class A{}
        """
        )
    }

    @Test
    fun sameProperty() {
        val thrown = assertFails {
            eval(
                """
            fun main(){}
            class A {
                a = 0
                a = 1
            }
        """
            )
        }
        assertTrue(thrown.message!!.contains("Same property found above"))
    }

    @Test
    fun testLinkProperty() {
        eval(
            """
            class Root {
                a = A()
                a.root = this
                iter = 0
            }
            class A {
                iter = parent.iter + 1
                a = if(iter < 1) A() else Leaf()
                a.root2 = a.root
                a.root = this
            }
            class Leaf {}
            fun main() {
                r = Root()
                print(r)
            }
        """
        )
    }

    @Test
    fun testNontrivialTwoStepLink() {
        eval(
            """
            class B {
                nontrivial = c.d.e
                c = C()
            }
            class C {
                d = D()
                d.e = E()
            }
            class D {}
            class E {}
            fun main() {
                b = B()
                test(b.c.d.e is E)
                test(b.c.d is D)
                test(b.c.d !is E)
                test(b.nontrivial is E)
                test(b is B); test(b.c is C)
            }
        """
        )
    }

    @Test
    fun findPropertyInMiddleClass() {
        eval(
            """
           class Start {
                mid = Middle()
           }
           class Middle {
                end = End()
                property = 2
                laterInitFromEnd = 1
           }
           class End {
                start = parent.parent
                a = start.mid.property
                start.mid.laterInitFromEnd = 3
           }
           fun main() {
                start = Start()
                test(start.mid.laterInitFromEnd == 3)
                test(start.mid.end.a == 2)
           }
        """
        )
    }

    @Test
    fun testEquals() {
        eval(
            """
            fun main() {
                first = A()
                firstOtherLink = first
                second = A()
                test(first != second)
                test(first == firstOtherLink)
                first.s = 2
                first.s = 1
                test(first == firstOtherLink)
                test(firstOtherLink.s == 1)
                firstOtherLink.a = "a"
                test(first.a == "a")
                
                firstSecondOther = changeB(first)
                test(firstOtherLink.b == "b")
                test(firstSecondOther == first)
                test(firstSecondOther == firstOtherLink)
            }
            class A {
                a = 0
                b = 1
            }
            fun changeB(aInstance) {aInstance.b = "b"
                return aInstance
            }
        """
        )
    }

    @Test
    fun checkInheritance() {
        eval(
            """
           class A {
                a = 2
                fun a() {return "a"}
           }
           class B:A {
            b = 3
            fun b() {return b}
            fun a() {return "b"}
           }
           fun main() {
            b = B()
            print(b.properties)
            print(b.a())
           }
        """
        )
    }

    @Test
    fun testTripleInheritance() {
        eval(
            """
            fun main() {
                a = A()
                print(a.c)
                print(a.b)
                print(a.p)
            }
            class A : B {
                p = 2
            }
            class B : C {
                p = 1
                b = p
            }
            class C {
                p = 0
                c = p
            }
        """
        )
    }

    @Test
    fun testInvocationInParent() {
        eval(
            """
            fun main() {
                a = A()
                test(a.b.one == 1)
            }
            class A {
                b = B()
                
                fun get1() {return 1}
            }
            class B {
                one = parent.get1()
            }
        """
        )
    }

    @Test
    fun testNotCreatedInvocation() {
        eval(
            """
            fun main() {
                a = A()
                test(a.b.one == 1)
            }
            class A {
                b = B()
                b.c = C()
                fun get1() {return 1}
            }
            class B {
                one = c.func()
            }
            class C {
                fun func(){return 1}
            }
        """
        )
    }

    @Test
    fun createFromTypeFunction() {
        eval(
            """
            fun main() {
                a = A()
                b = type(a)()
                test(b is A)
            }
            
            class A {
                p = 1
            }
        """
        )
    }

    @Test
    fun testNonLinkedSupertype() {
        eval(
            """
            import std.svg as svg
            import std.geometry2D as geom

            fun main() {
            	s=Stem()
            }

            class Stem:Segment {
            }
        """
        )
    }

    @Test
    fun uninitializedProperty() {
        eval("""
            fun main() {
                c = C()
                print(c.b.properties)
                print(c.b.points[0].properties)
                print(c.b.points[1].properties)
                print(c.b.points[2].properties)
            }

            class A {
                x = 0
                y = 0
                f = [Z(), Z(x=1),Z(y=1)]

                fun toString() { return str(x) + " " + y }
            }
            
            class B {
                points = [A(), A(x=1), A(y=1)]
                r = checkPoints()
                
                fun checkPoints() {
                    z = Z()
                    print("z: " + z.properties)
                    print(points[0].properties)
                    print(points[0].f[0].properties)
                }
            }
            
            class C {
                b = B()
                t = 1
            }
            
            class Z {
                x = 1
                y = 2
            }
        """)
    }

    @Test
    fun beforeAfterTest() {
        eval("""
            fun main() {a = A()}    
            object test {p = 0}
            
            class A {
                fun before() {
                    test(test.p == 0)
                    test.p = 1
                }
                a = initProp()
                b = a
                fun after() {test(test.p == 2)}
                fun afterAll() {print("1")}
                fun initProp() {test(test.p == 1);test.p = 2; return 3;}
            }
            
            class B:A {}
        """)
    }
}