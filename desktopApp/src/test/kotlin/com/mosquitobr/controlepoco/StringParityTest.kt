package com.mosquitobr.controlepoco

import com.mosquitobr.controlepoco.platform.StringCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringParityTest {
    private fun keys(path: String): Set<String> {
        val file = File(path)
        assertTrue("Arquivo de strings ausente: ${file.absolutePath}", file.exists())
        val regex = Regex("<string\\s+name=\"([^\"]+)\"")
        return regex.findAll(file.readText(Charsets.UTF_8)).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `strings pt e en mantem paridade de chaves`() {
        val pt = keys("src/main/resources/i18n/values/strings.xml")
        val en = keys("src/main/resources/i18n/values-en/strings.xml")
        val missingEn = pt - en
        val missingPt = en - pt
        assertTrue("Chaves sem tradução EN: $missingEn", missingEn.isEmpty())
        assertTrue("Chaves sem tradução PT: $missingPt", missingPt.isEmpty())
    }

    @Test
    fun `catalogo de strings carrega pt e en pelo classpath com todas as chaves geradas`() {
        val ptMap = StringCatalog.forLanguage("pt")
        val enMap = StringCatalog.forLanguage("en")
        assertEquals(R.keys.size, ptMap.size)
        assertEquals(R.keys.size, enMap.size)
        assertTrue(StringCatalog.get("pt", R.string.home_title).isNotBlank())
        assertTrue(StringCatalog.get("en", R.string.home_title).isNotBlank())
        val formattedPt = StringCatalog.get("pt", R.string.delete_result_confirm, "Cenário X")
        assertTrue(formattedPt.contains("Cenário X"))
    }
}
