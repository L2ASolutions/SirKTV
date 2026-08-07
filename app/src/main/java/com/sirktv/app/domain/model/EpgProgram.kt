package com.sirktv.app.domain.model

data class EpgProgram(
    val title: String,
    val description: String,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long
)

data class EpgNowNext(
    val now: EpgProgram?,
    val next: EpgProgram?
)
