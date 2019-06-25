package de.ahbnr.sessiontypeabs.compiler

/**
 * Modifies an ABS model, such that it complies with a given set of Global Session Types at runtime and then compiles them
 * to Erlang.
 */
fun compile(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>) {
    val typeBuild = buildTypes(typeSourceFileNames)
    val modelBuild = buildModel(absSourceFileNames, typeBuild)

    modelToErlang(modelBuild.model)
}
