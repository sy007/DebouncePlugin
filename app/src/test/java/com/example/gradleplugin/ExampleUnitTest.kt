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

    @Test
    fun addition_isCorrect() {
        var matcher: PathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:com/example/gradleplugin/MainActivity*.class")
//        Assert.assertEquals(
//            true, matcher.matches(File("com/example/gradleplugin/MainActivity.class").toPath())
//        )
//        Assert.assertEquals(
//            true, matcher.matches(File("com/example/gradleplugin/MainActivity1.class").toPath())
//        )
//        //AssertionError
//        Assert.assertEquals(
//            false,
//            matcher.matches(File("com/example/gradleplugin/Main.class").toPath())
//        )
//        名称只能由字母、数字、下划线、$符号组成


//        classPath:com/example/gradleplugin/MainActivity_ViewBinding$1.class
//        classPath:com/example/gradleplugin/ListViewActivity_ViewBinding.class
        matcher = FileSystems.getDefault()
            .getPathMatcher("glob:**/*__ViewBinding*.class")
        Assert.assertEquals(
            true,
            matcher.matches(File("com/example/gradleplugin/MainActivity__ViewBinding$1.class").toPath())
        )
        Assert.assertEquals(
            true,
            matcher.matches(File("com/example/gradleplugin/ListViewActivity__ViewBinding.class").toPath())
        )
    }

}