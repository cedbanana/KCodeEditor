package com.app

data class OutputLine(
    var text: String?,
    val isError: Boolean,
    val line: Int?,
    val column: Int?
)