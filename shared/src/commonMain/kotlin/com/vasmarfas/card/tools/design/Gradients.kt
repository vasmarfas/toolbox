package com.vasmarfas.card.tools.design

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Gradients {
    fun cssLinear(angleDegrees: Int, stops: List<Rgba>): String =
        "linear-gradient(${angleDegrees}deg, " + stopList(stops) + ")"

    fun cssRadial(stops: List<Rgba>): String = "radial-gradient(circle, " + stopList(stops) + ")"

    fun composeLinear(angleDegrees: Int, stops: List<Rgba>): String {
        val (x, y) = endOffset(angleDegrees)
        return buildString {
            append("Brush.linearGradient(\n")
            append("    colors = listOf(").append(stops.joinToString(", ") { it.composeLiteral }).append("),\n")
            append("    start = Offset(0f, 0f),\n")
            append("    end = Offset(").append(x).append("f, ").append(y).append("f),\n")
            append(")")
        }
    }

    fun composeRadial(stops: List<Rgba>): String =
        "Brush.radialGradient(colors = listOf(" + stops.joinToString(", ") { it.composeLiteral } + "))"

    fun endOffset(angleDegrees: Int, size: Int = 1000): Pair<Int, Int> {
        val radians = (angleDegrees - 90) * PI / 180
        return (size * cos(radians)).roundToInt() to (size * sin(radians)).roundToInt()
    }

    private fun stopList(stops: List<Rgba>): String =
        stops.mapIndexed { i, c -> "${c.hex().lowercase()} ${percent(i, stops.size)}%" }.joinToString(", ")

    private fun percent(index: Int, size: Int): Int = if (size <= 1) 0 else (index * 100.0 / (size - 1)).roundToInt()
}
