package de.ahbnr.sessiontypeabs.types.analysis

interface Transferable<L, T> {
   fun transfer(label: L): T
}