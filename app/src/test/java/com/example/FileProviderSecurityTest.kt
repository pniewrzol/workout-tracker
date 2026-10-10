package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileProviderSecurityTest {

    @Test
    fun `file_paths xml must not contain wildcard or root directory paths`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Find file_paths.xml in project
        val candidateLocations = listOf(
            File("src/main/res/xml/file_paths.xml"),
            File("app/src/main/res/xml/file_paths.xml"),
            File("../app/src/main/res/xml/file_paths.xml")
        )
        val filePathsXml = candidateLocations.firstOrNull { it.exists() }
            ?: throw IllegalStateException("file_paths.xml not found in search paths")

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(filePathsXml)
        val root = doc.documentElement

        assertEquals("paths", root.nodeName)

        val childNodes = root.childNodes
        val exposedPaths = mutableListOf<Pair<String, String>>() // Tag to Path

        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element) {
                val tagName = node.tagName
                val pathAttr = node.getAttribute("path")
                exposedPaths.add(tagName to pathAttr)

                // 1. Must never expose the root of filesDir or cacheDir ("." or "")
                assertFalse(
                    "Ścieżka nie może być rootem ('.'): $tagName path='$pathAttr'",
                    pathAttr == "." || pathAttr.isEmpty()
                )

                // 2. Must never expose databases or shared_prefs
                assertFalse(
                    "Nie wolno udostępniać katalogu baz danych!",
                    pathAttr.contains("databases", ignoreCase = true)
                )
                assertFalse(
                    "Nie wolno udostępniać katalogu ustawień prywatnych (shared_prefs)!",
                    pathAttr.contains("shared_prefs", ignoreCase = true)
                )
            }
        }

        // Verify only expected directories are mapped
        val tagToPathMap = exposedPaths.toMap()
        assertTrue("Wymagana ścieżka do reports w cache", tagToPathMap.containsKey("cache-path"))
        assertEquals("reports/", tagToPathMap["cache-path"])

        assertTrue("Wymagana ścieżka do exercise_media w files", tagToPathMap.containsKey("files-path"))
        assertEquals("exercise_media/", tagToPathMap["files-path"])
    }

    @Test
    fun `AndroidManifest specifies FileProvider with exported false and grantUriPermissions true`() {
        val candidateLocations = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidateLocations.firstOrNull { it.exists() }
            ?: throw IllegalStateException("AndroidManifest.xml not found")

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(manifestFile)

        val providers = doc.getElementsByTagName("provider")
        var fileProviderFound = false

        for (i in 0 until providers.length) {
            val provider = providers.item(i) as Element
            val name = provider.getAttribute("android:name")
            if (name.contains("FileProvider")) {
                fileProviderFound = true
                val exported = provider.getAttribute("android:exported")
                val grantUri = provider.getAttribute("android:grantUriPermissions")

                assertEquals(
                    "FileProvider MUSI mieć android:exported='false' ze względów bezpieczeństwa",
                    "false",
                    exported
                )
                assertEquals(
                    "FileProvider MUSI mieć android:grantUriPermissions='true'",
                    "true",
                    grantUri
                )
            }
        }

        assertTrue("FileProvider musi być zadeklarowany w AndroidManifest.xml", fileProviderFound)
    }
}
