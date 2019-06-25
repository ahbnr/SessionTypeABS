package de.ahbnr.sessiontypeabs.types.parser

data class FileContext(
    val startLine: Int,
    val startColumn: Int,
    val file: String
)