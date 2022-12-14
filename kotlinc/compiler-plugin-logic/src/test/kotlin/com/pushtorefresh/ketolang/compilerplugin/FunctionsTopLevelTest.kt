package com.pushtorefresh.ketolang.compilerplugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class FunctionsTopLevelTest {

    @Test
    fun `return type Unit is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f() {
                
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions returning Unit are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `return type Any is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(): Any {
                return Unit
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions returning Any are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `0 parameters are not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(): Int {
                return 0
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions without parameters are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `Array parameter is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(array: Array<String>): Int {
                return array.size
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions accepting potentially mutable parameters are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `Int parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: Int): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `nullable Int parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: Int?): String {
                return b?.toString() ?: "null"
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `data class parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            data class D(val i: Int)
            fun f(d: D): String {
                return d.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `String parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: String): String {
                return b
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `nullable String parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: String): String {
                return b
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(Int) parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: List<Int>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(nullable Int) parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: List<Int?>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(String) parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: List<String>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(nullable String) parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: List<String?>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(data class) parameter is allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            data class D(val i: Int)

            fun f(b: List<D>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `List(Any) parameter is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: List<Any>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions accepting potentially mutable parameters are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `MutableList(String) parameter is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            fun f(b: MutableList<Any>): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: functions accepting potentially mutable parameters are not allowed!, node name = 'f'"
        )
    }

    @Test
    fun `suspend function is not allowed`() {
        val aKt = SourceFile.kotlin(
            "a.kt", """
            package p

            suspend fun f(b: Int): String {
                return b.toString()
            }
        """
        )

        val result = compile(aKt)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "Ketolang error: suspend functions are not allowed!, node name = 'f'"
        )
    }
}
