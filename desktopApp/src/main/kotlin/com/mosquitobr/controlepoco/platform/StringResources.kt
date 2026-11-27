package com.mosquitobr.controlepoco.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mosquitobr.controlepoco.R
import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Carregador bilíngue (PT-BR / EN) compatível com os arquivos Android `strings.xml`.
 * Permite troca instantânea de idioma em tempo de execução no Desktop (Windows 11 e Linux/WSL).
 */
object StringCatalog {
    private val cache = mutableMapOf<String, Map<String, String>>()

    fun forLanguage(lang: String): Map<String, String> {
        val normalized = if (lang.lowercase(Locale.ROOT).startsWith("en")) "en" else "pt"
        return synchronized(cache) {
            cache.getOrPut(normalized) {
                val base = loadXml("/i18n/values/strings.xml")
                if (normalized == "en") {
                    base + loadXml("/i18n/values-en/strings.xml")
                } else {
                    base
                }
            }
        }
    }

    fun get(lang: String, id: Int): String {
        val key = R.keys.getOrNull(id) ?: return ""
        return forLanguage(lang)[key] ?: key
    }

    fun get(lang: String, id: Int, vararg formatArgs: Any): String {
        val template = get(lang, id)
        if (formatArgs.isEmpty()) return template
        val locale = if (lang.lowercase(Locale.ROOT).startsWith("en")) Locale.US else Locale.forLanguageTag("pt-BR")
        return runCatching { String.format(locale, template, *formatArgs) }.getOrDefault(template)
    }

    internal fun parseXml(stream: InputStream): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val doc = factory.newDocumentBuilder().parse(stream)
        val nodes = doc.getElementsByTagName("string")
        val map = LinkedHashMap<String, String>(nodes.length)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            map[name] = decodeAndroidString(node.textContent ?: "")
        }
        return map
    }

    private fun loadXml(resourcePath: String): Map<String, String> {
        val stream = StringCatalog::class.java.getResourceAsStream(resourcePath)
            ?: error("Recurso de strings não encontrado no classpath: $resourcePath")
        return stream.use { parseXml(it) }
    }

    private fun decodeAndroidString(raw: String): String {
        val trimmed = raw.trim()
        val unquoted = if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
        val sb = StringBuilder(unquoted.length)
        var i = 0
        while (i < unquoted.length) {
            val c = unquoted[i]
            if (c == '\\' && i + 1 < unquoted.length) {
                when (val next = unquoted[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '\'' -> sb.append('\'')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i += 1
            }
        }
        return sb.toString()
    }
}

val LocalAppStringsLanguage = staticCompositionLocalOf { "pt" }

@Composable
fun stringResource(id: Int): String {
    val lang = LocalAppStringsLanguage.current
    return StringCatalog.get(lang, id)
}

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String {
    val lang = LocalAppStringsLanguage.current
    return StringCatalog.get(lang, id, *formatArgs)
}
