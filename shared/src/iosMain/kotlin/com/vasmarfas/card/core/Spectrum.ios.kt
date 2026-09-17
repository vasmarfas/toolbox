package com.vasmarfas.card.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun microphoneSpectrumFlow(fftSize: Int): Flow<SpectrumFrame> = emptyFlow()
