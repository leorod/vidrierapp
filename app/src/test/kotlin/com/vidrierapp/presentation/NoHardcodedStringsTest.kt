package com.vidrierapp.presentation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NoHardcodedStringsTest {

    private val sourceRoot = File("src/main/kotlin")

    private val literalPatterns = listOf(
        Regex("""\bText\(\s*(text\s*=\s*)?""""),
        Regex("""\b(text|label|title|placeholder|supportingText|actionLabel|message)\s*=\s*"""),
        Regex("""\b(contentDescription|stateDescription|paneTitle)\s*=\s*""""),
        Regex("""\bshowSnackbar\(\s*(message\s*=\s*)?""""),
        Regex("""\bToast\.makeText\([^)]*,\s*""""),
        Regex("""\b(CustomAccessibilityAction)\(\s*""""),
        Regex("""\bstringResource\([^)]*\)\s*\+\s*"""),
        Regex("""\+\s*stringResource\("""),
        Regex("""\bgetString\([^)]*\)\s*\+""")
    )

    private val allowlist = emptySet<String>()

    private fun kotlinFiles(): List<File> =
        sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun relativePath(file: File): String =
        file.relativeTo(File(sourceRoot, "com/vidrierapp")).invariantSeparatorsPath

    private fun violationsIn(file: File): List<String> =
        file.readLines().withIndex().filter { (_, line) ->
            val code = line.trim()
            !code.startsWith("*") && !code.startsWith("/*") && !code.startsWith("//") &&
                !code.startsWith("import ") && literalPatterns.any { it.containsMatchIn(code) }
        }.filter { (_, line) -> isUserFacingLiteral(line) }
            .map { (index, line) -> "${relativePath(file)}:${index + 1}: ${line.trim()}" }

    private fun isUserFacingLiteral(line: String): Boolean {
        val withoutKeys = line.replace(Regex("""\bkey\s*=\s*\{[^}]*\}"""), "")
        return literalPatterns.any { pattern ->
            pattern.find(withoutKeys)?.let { match ->
                val tail = withoutKeys.substring(match.range.last + 1)
                val matchedText = match.value
                matchedText.endsWith("+") || matchedText.contains("+") ||
                    matchedText.endsWith("\"") || tail.trimStart().startsWith("\"")
            } ?: false
        }
    }

    @Test
    fun sourceRootIsFound() {
        assertTrue("No se encontro ${sourceRoot.absolutePath}", kotlinFiles().size > 50)
    }

    @Test
    fun noUserFacingStringLiteralsInKotlinSources() {
        val violations = kotlinFiles()
            .filter { relativePath(it) !in allowlist }
            .flatMap { violationsIn(it) }

        assertTrue(
            "Strings hardcodeados (mover a strings.xml):\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun allowlistEntriesExist() {
        val missing = allowlist.filter { !File(sourceRoot, "com/vidrierapp/$it").exists() }
        assertTrue("Entradas de allowlist inexistentes: $missing", missing.isEmpty())
    }

    @Test
    fun detectorFlagsKnownBadPatterns() {
        val samples = listOf(
            "Text(\"hola\")",
            "Text(text = \"hola\")",
            "contentDescription = \"icono\"",
            "snackbarHostState.showSnackbar(\"error\")",
            "text = stringResource(R.string.a) + \" \" + name",
            "label = \"Nombre\""
        )
        samples.forEach { sample ->
            assertTrue("No detecto: $sample", isUserFacingLiteral(sample))
        }
    }

    @Test
    fun detectorAcceptsResourceBackedCode() {
        val samples = listOf(
            "Text(text = stringResource(R.string.a))",
            "Text(text = label)",
            "contentDescription = null",
            "contentDescription = description",
            "snackbarHostState.showSnackbar(message)",
            "label = stringResource(R.string.a)"
        )
        samples.forEach { sample ->
            assertTrue("Falso positivo: $sample", !isUserFacingLiteral(sample))
        }
    }
}
