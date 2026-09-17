package com.vasmarfas.card.tools.design

object SchemeExport {
    fun kotlin(dark: Boolean, roles: List<Pair<String, Rgba>>): String = buildString {
        append(if (dark) "darkColorScheme(\n" else "lightColorScheme(\n")
        roles.forEach { (name, color) ->
            append("    ").append(name).append(" = ").append(color.composeLiteral).append(",\n")
        }
        append(")")
    }

    fun css(roles: List<Pair<String, Rgba>>): String = buildString {
        append(":root {\n")
        roles.forEach { (name, color) ->
            append("    --md-sys-color-").append(kebab(name)).append(": ").append(color.hex().lowercase()).append(";\n")
        }
        append("}")
    }

    fun kebab(name: String): String = buildString {
        name.forEachIndexed { i, c ->
            if (c.isUpperCase()) {
                if (i > 0) append('-')
                append(c.lowercaseChar())
            } else {
                append(c)
            }
        }
    }
}
