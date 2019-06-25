package de.ahbnr.sessiontypeabs.types

interface GlobalTypeVisitor<ReturnT> {
    fun visit(type: GlobalType.Repetition): ReturnT
    fun visit(type: GlobalType.Concatenation): ReturnT
    fun visit(type: GlobalType.Branching): ReturnT
    fun visit(type: GlobalType.Fetching): ReturnT
    fun visit(type: GlobalType.Resolution): ReturnT
    fun visit(type: GlobalType.Interaction): ReturnT
    fun visit(type: GlobalType.Initialization): ReturnT
    fun visit(type: GlobalType.Release): ReturnT
}