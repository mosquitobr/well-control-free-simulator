package com.mosquitobr.controlepoco

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringParityTest {
    private fun keys(path: String): Set<String> {
        val file = File(path)
        assertTrue("Arquivo de strings ausente: ${file.absolutePath}", file.exists())
        val regex = Regex("<string\\s+name=\"([^\"]+)\"")
        return regex.findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `strings pt e en mantem paridade de chaves`() {
        val pt = keys("src/main/res/values/strings.xml")
        val en = keys("src/main/res/values-en/strings.xml")
        val missingEn = pt - en
        val missingPt = en - pt
        assertTrue("Chaves sem tradução EN: $missingEn", missingEn.isEmpty())
        assertTrue("Chaves sem tradução PT: $missingPt", missingPt.isEmpty())
    }
}
