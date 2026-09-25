import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.util.Locale

// files/mac-vendors.tsv from the four IEEE registries: prefix, organisation and country per line, the
// longest prefix of an address names its vendor. Run by hand before a release: ./gradlew :shared:updateMacVendors

tasks.register("updateMacVendors") {
    group = "resources"
    description = "Rebuilds composeResources/files/mac-vendors.tsv from the IEEE MA-L, MA-M, MA-S and IAB registries"
    val target = layout.projectDirectory.file("src/commonMain/composeResources/files/mac-vendors.tsv").asFile
    doLast {
        val countries = Locale.getISOCountries().toSet()

        fun csvRows(text: String): List<List<String>> {
            val rows = mutableListOf<List<String>>()
            var row = mutableListOf<String>()
            val field = StringBuilder()
            var quoted = false
            var i = 0
            while (i < text.length) {
                val c = text[i]
                when {
                    quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> quoted = !quoted
                    !quoted && c == ',' -> {
                        row.add(field.toString())
                        field.clear()
                    }
                    !quoted && (c == '\n' || c == '\r') -> {
                        if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                        row.add(field.toString())
                        field.clear()
                        if (row.any { it.isNotBlank() }) rows.add(row)
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
                i++
            }
            row.add(field.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
            return rows
        }

        // the address ends with the country and the postcode, "San Diego CA US 92130", and a British postcode has a space
        fun country(address: String): String =
            address.trim().split(Regex("\\s+")).takeLast(3).reversed().firstOrNull { it.length == 2 && it in countries }.orEmpty()

        fun download(path: String): String {
            val connection = URI("https://standards-oui.ieee.org/$path").toURL().openConnection() as HttpURLConnection
            // the IEEE front end turns away clients that do not look like a browser
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.connectTimeout = 30_000
            connection.readTimeout = 600_000
            return connection.inputStream.use { it.readBytes().decodeToString() }
        }

        val blocks = sortedMapOf<String, String>()
        for (path in listOf("oui/oui.csv", "oui28/mam.csv", "oui36/oui36.csv", "iab/iab.csv")) {
            for (row in csvRows(download(path)).drop(1)) {
                val prefix = row.getOrNull(1)?.trim()?.uppercase() ?: continue
                if (!prefix.matches(Regex("[0-9A-F]{6}|[0-9A-F]{7}|[0-9A-F]{9}"))) continue
                val name = row.getOrNull(2).orEmpty().replace(Regex("\\s+"), " ").trim().ifEmpty { "Private" }
                blocks[prefix] = name + "\t" + country(row.getOrNull(3).orEmpty())
            }
        }
        target.writeText(buildString {
            appendLine("#" + LocalDate.now())
            blocks.forEach { (prefix, line) -> appendLine(prefix + "\t" + line.trimEnd('\t')) }
        })
        println("${blocks.size} blocks, ${target.length() / 1024} KB")
    }
}
