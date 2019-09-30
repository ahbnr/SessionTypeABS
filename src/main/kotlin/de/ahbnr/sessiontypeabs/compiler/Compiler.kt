package de.ahbnr.sessiontypeabs.compiler

/**
 * Modifies an ABS model, such that it complies with a given set of Global Session Types at runtime and then compiles them
 * to Erlang.
 */
fun compile(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>) {
    val typeBuilds = buildTypes(typeSourceFileNames)
    val modelBuild = buildModel(absSourceFileNames, typeBuilds)

    modelToErlang(modelBuild.model)
}
