package com.vasmarfas.card.tools.security

import com.vasmarfas.card.tools.Tool

val securityTools: List<Tool> = listOf(
    passwordGeneratorTool,
    randomGeneratorTool,
    passwordStrengthTool,
    pwnedCheckTool,
    totpTool,
    checksumCompareTool,
)
