package de.ahbnr.sessiontypeabs.compiler

/**
 * Modifies an ABS model, such that it complies with a given set of Global Session Types at runtime and then compiles them
 * to Erlang.
 */
fun compile(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>): ModelBuild {
    val model = parseModel(absSourceFileNames)

    val typeBuilds = buildTypes(typeSourceFileNames, model)
    val modelBuild = buildModel(model, typeBuilds)

    modelToErlang(modelBuild.model)

    return modelBuild
}
