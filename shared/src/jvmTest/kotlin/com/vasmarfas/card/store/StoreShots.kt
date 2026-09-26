package com.vasmarfas.card.store

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.Density
import com.vasmarfas.card.App
import com.vasmarfas.card.tools.documents.fixture
import com.vasmarfas.card.tools.documents.pdf.pdf
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
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
        Device("ipad", 2752, 2064, 2f, homeRow = 4),
        Device("desktop", 2880, 1800, 2f, homeRow = 4),
    )

    // the home screen puts every category in its own section, one full row each: three tiles on tablets, four on
    // the desktop
    private val home = listOf(
        listOf("image-compressor", "photo-editor", "video-converter", "audio-converter"),
        listOf("pdf-editor", "merge-pdf", "document-converter", "images-to-pdf"),
        listOf("decision-wheel", "dice-roller", "team-splitter", "tally-counter"),
        listOf("qr-generator", "color-converter", "barcode-generator", "image-palette"),
        listOf("password-generator", "totp", "pwned-check", "password-strength"),
        listOf("json-formatter", "base64", "hash-generator", "regex-tester"),
        listOf("speed-test", "subnet-calculator", "ping", "dns-lookup"),
        listOf("filament-guide", "print-cost", "filament-length-weight", "print-time-estimate"),
    )

    private val shots = listOf(
        Shot("01-home", "#my"),
        Shot("02-pdf", "#tools/pdf-editor") { ru ->
            pick(sampleLease(ru))
            tap(if (ru) "Выбрать PDF" else "Choose a PDF")
            bringToTop(if (ru) "Договор аренды квартиры.pdf" else "Apartment lease.pdf", 90f)
            val page = pageBounds()
            onRoot().performTouchInput { click(Offset(page.left + page.width * 0.4f, page.top + page.height * 0.318f)) }
        },
        Shot("03-currency", "#tools/currency-converter", dark = true) { ru ->
            waitUntil(timeoutMillis = 15_000) { onAllNodes(hasText(if (ru) "Курсы обновлены" else "Rates updated")).fetchSemanticsNodes().isNotEmpty() }
        },
        Shot("04-catalog", "#tools"),
        Shot("05-qr", "#tools/qr-generator") {
            type("https://vasmarfas.com")
            bringToTop("vasmarfas.com", 90f)
        },
        Shot("06-passwords", "#tools/password-generator", dark = true),
        Shot("07-filament", "#tools/filament-guide") { ru ->
            tap(if (ru) "Будет на улице и солнце" else "Outdoors in the sun")
            tap(if (ru) "Закрытый, без подогрева камеры" else "Enclosed, no chamber heating")
            bringToTop(if (ru) "Подходят лучше всего" else "Suits best", 90f)
        },
        Shot("08-json", "#tools/json-formatter", dark = true) {
            type(SAMPLE_JSON)
            bringToTop("JSON", 70f)
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

    // FileKit keeps the desktop picker, an internal interface, in a lazy: a scene puts a proxy there that hands out a
    // sample file
    private fun pick(file: File) {
        val type = Class.forName("io.github.vinceglb.filekit.dialogs.platform.PlatformFilePicker")
        val picker = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "openFilePicker" -> file
                "openFilesPicker" -> listOf(file)
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                "toString" -> "sample picker"
                else -> null
            }
        }
        val lazy = Class.forName("${type.name}\$Companion").getDeclaredField("current\$delegate").apply { isAccessible = true }.get(null)
        lazy.javaClass.getDeclaredField("_value").apply { isAccessible = true }.set(lazy, picker)
    }

    private fun sampleLease(ru: Boolean): File {
        val fonts = "src/commonMain/composeResources/files/fonts"
        val bytes = pdf { doc ->
            val regular = PDType0Font.load(doc, File("$fonts/MobitoolSerif-Regular.ttf"))
            val bold = PDType0Font.load(doc, File("$fonts/MobitoolSerif-Bold.ttf"))
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                var y = 760f
                fun text(value: String, font: PDFont, size: Float, x: Float) {
                    cs.beginText()
                    cs.setFont(font, size)
                    cs.newLineAtOffset(x, y)
                    cs.showText(value)
                    cs.endText()
                }
                for (line in fixture(if (ru) "lease-ru.txt" else "lease-en.txt").decodeToString().trimEnd().lines()) {
                    when {
                        line.startsWith("# ") -> {
                            text(line.drop(2), bold, 17f, 72f)
                            y -= 30f
                        }
                        line.startsWith("## ") -> {
                            y -= 8f
                            text(line.drop(3), bold, 12f, 72f)
                            y -= 18f
                        }
                        '|' in line -> {
                            val (left, right) = line.split('|')
                            text(left, regular, 11f, 72f)
                            text(right, regular, 11f, 523f - regular.getStringWidth(right) / 1000 * 11f)
                            y -= 26f
                        }
                        else -> {
                            text(line, regular, 11f, 72f)
                            y -= 15.5f
                        }
                    }
                }
            }
        }
        val dir = Files.createTempDirectory("store").toFile()
        return File(dir, if (ru) "Договор аренды квартиры.pdf" else "Apartment lease.pdf").apply { writeBytes(bytes) }
    }

    // the page is the only pure white area of the editor
    private fun SkikoComposeUiTest.pageBounds(): Rect {
        settle()
        val pixels = captureToImage().toPixelMap()
        var left = pixels.width
        var right = 0
        var top = pixels.height
        var bottom = 0
        for (y in 0 until pixels.height) {
            var count = 0
            var first = -1
            var last = -1
            for (x in 0 until pixels.width) {
                if (pixels[x, y] != Color.White) continue
                count++
                if (first < 0) first = x
                last = x
            }
            if (count < pixels.width / 5) continue
            top = min(top, y)
            bottom = y
            left = min(left, first)
            right = max(right, last)
        }
        return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
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
