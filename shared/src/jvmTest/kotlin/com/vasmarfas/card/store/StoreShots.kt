package com.vasmarfas.card.store

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.Density
import com.vasmarfas.card.App
import java.io.File
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import org.junit.Assume.assumeTrue

// store screenshots of the real app: ./gradlew :shared:jvmTest --tests "*StoreShots*" -PstoreShots=<folder>,
// -PstoreOnly=phone,ipad limits the devices, -PstorePick=01-home,04-qr the shots
@OptIn(ExperimentalTestApi::class)
class StoreShots {
    private val out = System.getProperty("store.shots")?.let(::File)

    class Device(val name: String, val width: Int, val height: Int, val density: Float, val homeRow: Int = 3)

    class Shot(val name: String, val link: String, val dark: Boolean = false, val prepare: SkikoComposeUiTest.(Boolean) -> Unit = {})

    private val devices = listOf(
        Device("phone", 1080, 2340, 3f),
        Device("iphone", 1320, 2868, 3f),
        Device("tablet", 2560, 1600, 2f),
        Device("ipad", 2064, 2752, 2f),
        Device("desktop", 2880, 1800, 2f, homeRow = 4),
    )

    // the home screen puts every category in its own section, one full row each: three tiles on tablets, four on
    // the desktop
    private val home = listOf(
        listOf("image-compressor", "photo-editor", "video-converter", "audio-converter"),
        listOf("pdf-editor", "merge-pdf", "document-converter", "images-to-pdf"),
        listOf("qr-generator", "color-converter", "barcode-generator", "image-palette"),
        listOf("password-generator", "totp", "pwned-check", "password-strength"),
        listOf("json-formatter", "base64", "hash-generator", "regex-tester"),
        listOf("speed-test", "subnet-calculator", "ping", "dns-lookup"),
        listOf("resistor-color-code", "ohms-law", "led-resistor", "voltage-divider"),
        listOf("filament-guide", "print-cost", "filament-length-weight", "print-time-estimate"),
    )

    private val shots = listOf(
        Shot("01-home", "#my"),
        Shot("02-catalog", "#tools"),
        Shot("03-subnet", "#tools/subnet-calculator"),
        Shot("04-qr", "#tools/qr-generator") {
            type("https://vasmarfas.com")
            bringToTop("vasmarfas.com", 90f)
        },
        Shot("05-filament", "#tools/filament-guide", dark = true) { ru ->
            tap(if (ru) "Будет на улице и солнце" else "Outdoors in the sun")
            tap(if (ru) "Закрытый, без подогрева камеры" else "Enclosed, no chamber heating")
            bringToTop(if (ru) "Подходят лучше всего" else "Suits best", 90f)
        },
        Shot("06-resistor", "#tools/resistor-color-code") { ru -> bringToTop(if (ru) "Допуск" else "Tolerance", 90f) },
        Shot("07-json", "#tools/json-formatter", dark = true) { ru ->
            type(SAMPLE_JSON)
            bringToTop("JSON", 70f)
        },
        Shot("08-morse", "#tools/morse-code") { ru ->
            type("SOS")
            bringToTop(if (ru) "Результат" else "Result", 90f)
        },
        Shot("check-filament-table", "#tools/filament-guide") { ru ->
            tap(if (ru) "Таблица" else "Table")
            bringToTop(if (ru) "Поиск по названию" else "Search by name", 90f)
        },
        Shot("check-filament-detailed", "#tools/filament-guide") { ru ->
            tap(if (ru) "Таблица" else "Table")
            tap(if (ru) "Расширенный" else "Detailed")
            type("PETG")
            bringToTop(if (ru) "Поиск по названию" else "Search by name", 90f)
        },
    )

    private fun seed(device: Device, lang: String, dark: Boolean) {
        MemoryPreferencesFactory.root.reset()
        val prefs = Preferences.userRoot().node("com/vasmarfas/card")
        prefs.put("settings.lang", lang)
        prefs.put("settings.theme", if (dark) "DARK" else "LIGHT")
        prefs.put("settings.dynamic", "false")
        prefs.put("onboarding.status", "DONE")
        prefs.put("tools.favorites", home.flatMap { it.take(device.homeRow) }.joinToString(","))
    }

    private fun SkikoComposeUiTest.settle() {
        repeat(4) {
            waitForIdle()
            mainClock.advanceTimeBy(700)
            Thread.sleep(250)
        }
        waitForIdle()
    }

    private fun SkikoComposeUiTest.tap(text: String) = onNode(hasText(text) and hasClickAction()).performScrollTo().performClick()

    private fun SkikoComposeUiTest.type(text: String) = onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(text)

    // scrolls the page until the node with the text sits a margin below the top, the collapsing bar takes part of
    // every scroll, hence the passes
    private fun SkikoComposeUiTest.bringToTop(text: String, marginDp: Float) {
        repeat(4) {
            settle()
            val top = onAllNodes(hasText(text, substring = true)).onFirst().fetchSemanticsNode().positionInRoot.y
            val delta = top - marginDp * density.density
            if (abs(delta) < 8f) return
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).onFirst()
                .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
        }
    }

    @Test
    fun render() {
        assumeTrue(out != null)
        val only = System.getProperty("store.only")?.split(',')?.toSet()
        val picked = System.getProperty("store.pick")?.split(',')?.toSet()
        for (device in devices.filter { only == null || it.name in only }) {
            for (lang in listOf("RU", "EN")) {
                for (shot in shots.filter { if (picked == null) !it.name.startsWith("check") else it.name in picked }) {
                    seed(device, lang, shot.dark)
                    val file = File(out, "${device.name}/${lang.lowercase()}/${shot.name}.png")
                    // closing the scene can trip the navigation lifecycle, the picture is written by then
                    runCatching { capture(device, shot, lang == "RU", file) }.onFailure { if (!file.exists()) throw it }
                }
            }
        }
    }

    private fun capture(device: Device, shot: Shot, ru: Boolean, file: File) {
        file.delete()
        file.parentFile.mkdirs()
        DesktopComposeUiTest(device.width, device.height, density = Density(device.density), useStandardTestDispatcherForComposition = false).runTest {
            setContent { App(link = "https://vasmarfas.com/${shot.link}") }
            settle()
            runCatching { shot.prepare(this, ru) }.onFailure {
                File(file.parentFile, "${shot.name}.txt").writeText(it.toString() + "\n\n" + onRoot().printToString())
            }
            settle()
            ImageIO.write(captureToImage().toAwtImage(), "png", file)
        }
    }

    companion object {
        const val SAMPLE_JSON = """{"tool":"json-formatter","tags":["format","tree","minify"],"size":{"keys":7,"depth":3},"ok":true}"""

        init {
            if (System.getProperty("store.shots") != null) {
                System.setProperty("java.util.prefs.PreferencesFactory", MemoryPreferencesFactory::class.java.name)
            }
        }
    }
}
