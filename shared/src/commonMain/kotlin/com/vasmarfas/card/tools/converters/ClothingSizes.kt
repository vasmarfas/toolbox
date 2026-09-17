package com.vasmarfas.card.tools.converters

import com.vasmarfas.card.resources.Res
import com.vasmarfas.card.resources.men_s_clothing
import com.vasmarfas.card.resources.men_s_shoes
import com.vasmarfas.card.resources.women_s_clothing
import com.vasmarfas.card.resources.women_s_shoes
import org.jetbrains.compose.resources.StringResource

class SizeTable(val title: StringResource, val columns: List<String>, val rows: List<List<String>>) {
    fun values(column: Int): List<String> = rows.map { it[column] }.distinct()

    fun lookup(column: Int, value: String): List<List<String>> = rows.filter { it[column] == value }

    fun lines(): List<String> {
        val widths = columns.indices.map { c -> (rows.map { it[c].length } + columns[c].length).max() + 2 }
        return listOf(columns.mapIndexed { c, name -> name.padEnd(widths[c]) }.joinToString("")) +
            rows.map { row -> row.mapIndexed { c, cell -> cell.padEnd(widths[c]) }.joinToString("") }
    }
}

object ClothingSizes {
    val womenClothing = SizeTable(
        Res.string.women_s_clothing,
        listOf("RU", "EU", "US", "UK", "INT", "Bust", "Waist", "Hips"),
        listOf(
            listOf("40", "34", "2", "6", "XXS", "80", "62", "88"),
            listOf("42", "36", "4", "8", "XS", "84", "65", "92"),
            listOf("44", "38", "6", "10", "S", "88", "68", "96"),
            listOf("46", "40", "8", "12", "M", "92", "72", "100"),
            listOf("48", "42", "10", "14", "L", "96", "76", "104"),
            listOf("50", "44", "12", "16", "XL", "100", "80", "108"),
            listOf("52", "46", "14", "18", "XXL", "104", "84", "112"),
            listOf("54", "48", "16", "20", "XXXL", "108", "88", "116"),
            listOf("56", "50", "18", "22", "4XL", "112", "92", "120"),
            listOf("58", "52", "20", "24", "5XL", "116", "96", "124"),
        ),
    )

    val menClothing = SizeTable(
        Res.string.men_s_clothing,
        listOf("RU", "EU", "US", "UK", "INT", "Chest", "Waist"),
        listOf(
            listOf("44", "44", "34", "34", "XS", "88", "76"),
            listOf("46", "46", "36", "36", "S", "92", "80"),
            listOf("48", "48", "38", "38", "M", "96", "84"),
            listOf("50", "50", "40", "40", "L", "100", "88"),
            listOf("52", "52", "42", "42", "XL", "104", "92"),
            listOf("54", "54", "44", "44", "XXL", "108", "96"),
            listOf("56", "56", "46", "46", "XXXL", "112", "100"),
            listOf("58", "58", "48", "48", "4XL", "116", "104"),
            listOf("60", "60", "50", "50", "5XL", "120", "108"),
        ),
    )

    val womenShoes = SizeTable(
        Res.string.women_s_shoes,
        listOf("cm", "RU", "EU", "US", "UK"),
        listOf(
            listOf("22", "34", "35", "4.5", "2.5"),
            listOf("22.5", "35", "36", "5", "3"),
            listOf("23", "36", "37", "6", "4"),
            listOf("23.5", "37", "38", "6.5", "4.5"),
            listOf("24", "37.5", "38.5", "7", "5"),
            listOf("24.5", "38", "39", "7.5", "5.5"),
            listOf("25", "39", "40", "8", "6"),
            listOf("25.5", "39.5", "40.5", "8.5", "6.5"),
            listOf("26", "40", "41", "9", "7"),
            listOf("26.5", "41", "42", "9.5", "7.5"),
            listOf("27", "42", "43", "10", "8"),
        ),
    )

    val menShoes = SizeTable(
        Res.string.men_s_shoes,
        listOf("cm", "RU", "EU", "US", "UK"),
        listOf(
            listOf("24.5", "38", "39", "6.5", "5.5"),
            listOf("25", "39", "40", "7", "6"),
            listOf("25.5", "40", "40.5", "7.5", "6.5"),
            listOf("26", "40.5", "41", "8", "7"),
            listOf("26.5", "41", "42", "8.5", "7.5"),
            listOf("27", "42", "42.5", "9", "8"),
            listOf("27.5", "42.5", "43", "9.5", "8.5"),
            listOf("28", "43", "44", "10", "9"),
            listOf("28.5", "44", "44.5", "10.5", "9.5"),
            listOf("29", "44.5", "45", "11", "10"),
            listOf("29.5", "45", "46", "11.5", "10.5"),
            listOf("30", "46", "47", "12", "11"),
            listOf("30.5", "47", "48", "13", "12"),
        ),
    )

    val tables = listOf(womenClothing, menClothing, womenShoes, menShoes)
}
