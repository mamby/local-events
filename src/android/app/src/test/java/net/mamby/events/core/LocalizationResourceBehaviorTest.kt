package net.mamby.events.core

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourceBehaviorTest {
    private val resDirectory = File("src/main/res")

    @Test
    fun localizedResources_matchDefaultStringAndPluralKeys() {
        val defaultResources = readResources(File(resDirectory, "values/strings.xml"))

        localizedValueDirectories().forEach { directory ->
            val localizedResources = readResources(File(directory, "strings.xml"))
            assertEquals(
                "String keys differ for ${directory.name}",
                defaultResources.stringNames,
                localizedResources.stringNames
            )
            assertEquals(
                "Plural keys differ for ${directory.name}",
                defaultResources.pluralQuantities.keys,
                localizedResources.pluralQuantities.keys
            )
            localizedResources.pluralQuantities.forEach { (name, quantities) ->
                assertTrue("${directory.name}/$name must define an other quantity", "other" in quantities)
            }
        }
    }

    @Test
    fun supportedLanguages_matchLocalizedResourceDirectories() {
        val resourceLanguages = localizedValueDirectories()
            .mapNotNull { it.name.toLanguageTagOrNull() }
            .plus("en")
            .sorted()
        val supportedLanguages = SupportedAppLanguages.map { it.tag }.sorted()

        assertEquals(resourceLanguages, supportedLanguages)
    }

    @Test
    fun appLocalizer_mapsEveryUsedLocalizationKey() {
        val mappedKeys = AppLocalizer.stringResourceIds.keys + AppLocalizer.pluralResourceIds.keys
        val usedKeys = File("src/main/java/net/mamby/events")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> localizationKeyRegex.findAll(file.readText()).map { it.groupValues[1] } }
            .filter { it.isNotBlank() }
            .toSet()

        assertTrue(
            "Localization keys are used but not mapped: ${(usedKeys - mappedKeys).sorted()}",
            mappedKeys.containsAll(usedKeys)
        )
    }

    @Test
    fun appLocalizer_resourceIdsExistInDefaultResources() {
        val defaultResources = readResources(File(resDirectory, "values/strings.xml"))
        val stringNamesById = resourceNamesById(net.mamby.events.R.string::class.java)
        val pluralNamesById = resourceNamesById(net.mamby.events.R.plurals::class.java)
        val stringResourceNames = AppLocalizer.stringResourceIds.values
            .map { resourceId -> stringNamesById.getValue(resourceId) }
            .toSet()
        val pluralResourceNames = AppLocalizer.pluralResourceIds.values
            .map { resource -> pluralNamesById.getValue(resource.id) }
            .toSet()

        assertTrue(defaultResources.stringNames.containsAll(stringResourceNames))
        assertTrue(defaultResources.pluralQuantities.keys.containsAll(pluralResourceNames))
    }

    private fun localizedValueDirectories(): List<File> =
        resDirectory.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .sortedBy { it.name }

    private fun readResources(file: File): ResourceSnapshot {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val root = document.documentElement
        val strings = root.elements("string")
            .map { it.getAttribute("name") }
            .toSet()
        val plurals = root.elements("plurals")
            .associate { plural ->
                plural.getAttribute("name") to plural.elements("item")
                    .map { it.getAttribute("quantity") }
                    .toSet()
            }

        return ResourceSnapshot(strings, plurals)
    }

    private fun resourceNamesById(resourceClass: Class<*>): Map<Int, String> =
        resourceClass.fields.associate { field -> field.getInt(null) to field.name }

    private fun String.toLanguageTagOrNull(): String? =
        when {
            this == "values-b+zh+Hans" -> "zh-Hans"
            this.startsWith("values-b+") -> removePrefix("values-b+")
            this.startsWith("values-") -> removePrefix("values-")
            else -> null
        }

    private fun Element.elements(tagName: String): List<Element> {
        val nodes = getElementsByTagName(tagName)
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) as? Element }
    }

    private data class ResourceSnapshot(
        val stringNames: Set<String>,
        val pluralQuantities: Map<String, Set<String>>
    )

    private companion object {
        val localizationKeyRegex = Regex(
            "(?:(?:viewModel\\.)?(?:string|formatString)|localizer\\.(?:get|format))\\(\"([^\"]+)\""
        )
    }
}
