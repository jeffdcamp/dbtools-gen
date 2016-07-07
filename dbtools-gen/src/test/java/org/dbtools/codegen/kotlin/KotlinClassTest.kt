
import junit.framework.TestCase
import org.dbtools.codegen.kotlin.KotlinClass
import org.dbtools.codegen.kotlin.KotlinFun
import org.dbtools.codegen.kotlin.KotlinObjectClass
import org.dbtools.codegen.kotlin.KotlinVal

class HelloTest : TestCase() {

    fun testSimpleClass() : Unit {
        val myClass = KotlinClass("Test", "org.sample")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testDefaultConstructor() : Unit {
        val myClass = KotlinClass("DefaultCustructor", "org.sample")

        myClass.addVar("name", "String")
        myClass.addVal("age", "Int")
        myClass.createDefaultConstructor = true

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testConstructor2() : Unit {
        val myClass = KotlinClass("Custructor2", "org.sample")

        myClass.addVar("name", "String")
        myClass.addVal("age", "Int")
        myClass.addConstructor(
                parameters = listOf(KotlinVal("name", "String"), KotlinVal("age", "Int", "0")),
                content = "this.name = name\nthis.age = age")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    /**
     * Test Injection
     */
    fun testInjection() : Unit {
        val myClass = KotlinClass("InjectTest", "org.sample")

        myClass.addAnnotation("@Singleton")

        myClass.addImport("javax.inject.Inject")
        myClass.addImport("javax.inject.Singleton")

        var manager = myClass.addVar("manager", "SomeManager")
        manager.addAnnotation("@Inject")
        manager.lateInit = true;

        var construct1 = myClass.addConstructor(
                parameters = listOf(KotlinVal("name", "String"), KotlinVal("age", "Int", "0")),
                content = "this.name = name\nthis.age = age")
        construct1.addAnnotation("@Inject")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testFunc1() : Unit {
        val myClass = KotlinClass("Func1", "org.sample")

        myClass.addFun(
                name = "message",
                returnType = "String",
                content = "return \"Hello World\"")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testFunc2() : Unit {
        val myClass = KotlinClass("Func2", "org.sample")

        val fun2 = myClass.addFun(
                name = "doWork",
                parameters = listOf(KotlinVal("name", "String"), KotlinVal("age", "Int", "0")),
                content = "return \"Hello World\"")
        fun2.addAnnotation("OnClick")


        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testConst1() : Unit {
        val myClass = KotlinClass("Const1", "org.sample")

        myClass.addConstant("C_ID", "String", "_id")
        myClass.addConstant("C_NAME", "String", "name")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testStaticFunction() : Unit {
        val myClass = KotlinClass("StaticFunc", "org.sample")

        myClass.addStaticFun(KotlinFun("doWork", listOf(KotlinVal("name", "String")), content = "println(name)\nprintln(\"Hello World\")"))

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testStaticInit() : Unit {
        val myClass = KotlinClass("StaticInit", "org.sample")

        myClass.addConstant("list", "ArrayList<String>()", "List<String>");
        myClass.appendStaticInitializer("list.add(\"Dude\")}")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testConstVar1() : Unit {
        val myClass = KotlinClass("ConstVar1", "org.sample")

        myClass.addConstant("C_ID", "_id", "String")
        myClass.addConstant("C_NAME", "name", "String")

        myClass.addVar("id", "Long")
        myClass.addVar("name", "String")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testConstVar2() : Unit {
        val myClass = KotlinClass("ConstVar2", "org.sample")

        myClass.addConstant("C_ID", "_id")
        myClass.addConstant("C_NAME", "name")

        myClass.addVar("id", "Long")
        myClass.addVar("name", "String")

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testSingletonClass() : Unit {
        val myClass = KotlinObjectClass("Singleton1", "org.sample")

        myClass.addConstant("C_ID", "_id")
        myClass.addConstant("C_NAME", "name")

        myClass.addFun(KotlinFun("doWork", listOf(KotlinVal("name", "String")), content = "println(name)\nprintln(\"Hello World\")"))

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }

    fun testDatatype() : Unit {
        val myClass = KotlinObjectClass("Singleton1", "org.sample")

        myClass.addVar("id", "int")

        myClass.addFun(KotlinFun("doWork", listOf(KotlinVal("name", "String")), content = "println(name)\nprintln(\"Hello World\")"))

        myClass.writeToDisk("build/test-src/src/main/kotlin/org/sample")
    }
}