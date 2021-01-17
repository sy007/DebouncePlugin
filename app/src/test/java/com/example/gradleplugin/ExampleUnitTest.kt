package com.example.gradleplugin

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun classLoaderTest() {



    }


    private fun ptintln(cls: Class<*>) {
        println("canonicalName:" + cls.canonicalName)
        println("name:" + cls.name)
        println("simpleName:" + cls.simpleName)
        println("typeName:" + cls.typeName)

        println("isInterface:" + cls.isInterface)
        println("superclass:" + cls.superclass)
    }

    @Test
    fun addition_isCorrect() {
        val matcher: PathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:com/example/gradleplugin/MainActivity*.class")
        Assert.assertEquals(
            true, matcher.matches(File("com/example/gradleplugin/MainActivity.class").toPath())
        )
        Assert.assertEquals(
            true, matcher.matches(File("com/example/gradleplugin/MainActivity$0.class").toPath())
        )
        Assert.assertEquals(
            true,
            matcher.matches(File("com/example/gradleplugin/MainActivity\$InnearClickListener.class").toPath())
        )
        //AssertionError
        Assert.assertEquals(
            true,
            matcher.matches(File("com/example/gradleplugin/Main.class").toPath())
        )
    }

}