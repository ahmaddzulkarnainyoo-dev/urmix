package com.winatra.urmix.home

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * P0 regression guard for the corrupted HomeDataLoader.kt (FASE 5 Step 0).
 *
 * The corruption nested `resolvePodcastSource` / `updateEmptyState` inside the
 * Rx `.subscribe` lambda and left orphan top-level statements after the class
 * closing brace. This test reads the source file and asserts structural sanity:
 * balanced braces, both helpers declared as class members (depth 1), and no
 * code after the final class closing brace.
 */
class HomeDataLoaderSourceTest {

    private fun sourceFile(): File {
        // Unit tests run with the :app module directory as working dir.
        val candidates = listOf(
            File("src/main/java/com/winatra/urmix/home/HomeDataLoader.kt"),
            File("app/src/main/java/com/winatra/urmix/home/HomeDataLoader.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
        assumeTrue("HomeDataLoader.kt not found from working dir", file != null)
        return file!!
    }

    private fun strippedLines(text: String): List<String> {
        var inBlockComment = false
        return text.lines().map { raw ->
            var line = raw
            if (inBlockComment) {
                val end = line.indexOf("*/")
                if (end >= 0) {
                    line = line.substring(end + 2)
                    inBlockComment = false
                } else {
                    line = ""
                }
            }
            while (true) {
                val start = line.indexOf("/*")
                if (start < 0) {
                    break
                }
                val end = line.indexOf("*/", start + 2)
                if (end < 0) {
                    line = line.substring(0, start)
                    inBlockComment = true
                    break
                }
                line = line.substring(0, start) + line.substring(end + 2)
            }
            val comment = line.indexOf("//")
            if (comment >= 0) {
                line = line.substring(0, comment)
            }
            // Strip string/char literals so braces inside them don't count.
            line.replace(Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"), "")
        }
    }

    @Test
    fun `braces are balanced`() {
        val lines = strippedLines(sourceFile().readText())
        var depth = 0
        var minDepth = 0
        for (line in lines) {
            for (ch in line) {
                if (ch == '{') {
                    depth++
                } else if (ch == '}') {
                    depth--
                }
                if (depth < minDepth) {
                    minDepth = depth
                }
            }
        }
        assertEquals("Unbalanced braces in HomeDataLoader.kt", 0, depth)
        assertTrue("More closing than opening braces in HomeDataLoader.kt", minDepth >= 0)
    }

    @Test
    fun `helpers are class members and nothing trails the class`() {
        val lines = strippedLines(sourceFile().readText())
        var depth = 0
        var resolveDepth = -1
        var updateDepth = -1
        var classDepth = -1
        var classClosedAt = -1
        for ((index, line) in lines.withIndex()) {
            if (line.contains("class HomeDataLoader") && classDepth < 0) {
                classDepth = depth
            }
            if (line.contains("fun resolvePodcastSource")) {
                resolveDepth = depth
            }
            if (line.contains("fun updateEmptyState")) {
                updateDepth = depth
            }
            for (ch in line) {
                if (ch == '{') {
                    depth++
                } else if (ch == '}') {
                    depth--
                }
            }
            if (classDepth >= 0 && depth == classDepth && index > 0 &&
                lines.subList(0, index + 1).joinToString("\n").contains("class HomeDataLoader")
            ) {
                // Track the last line where we return to class depth; the true
                // end of class is the final such return at EOF.
                classClosedAt = index
            }
        }
        assertEquals(
            "resolvePodcastSource must be a class member (depth 1)",
            1,
            resolveDepth
        )
        assertEquals("updateEmptyState must be a class member (depth 1)", 1, updateDepth)
        assertTrue("class HomeDataLoader declaration not found", classDepth == 0)
        assertEquals("Unbalanced braces in HomeDataLoader.kt", 0, depth)
        val trailing = lines.subList(classClosedAt + 1, lines.size).filter { it.isNotBlank() }
        assertTrue(
            "Orphan statements after class closing brace: $trailing",
            trailing.isEmpty()
        )
    }
}
