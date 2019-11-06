package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.staticverification.VerificationConfig

/**
 * Modifies an ABS model, such that it complies with a given set of Global Session Types at runtime and then compiles them
 * to Erlang.
 */
fun compile(
    absSourceFileNames: Iterable<String>,
    typeSourceFileNames: Iterable<String>,
    verificationConfig: VerificationConfig = VerificationConfig(),
    enforcementConfig: EnforcementConfig = EnforcementConfig(),
    compilerConfig: CompilerConfig = CompilerConfig()
): ModelBuild {
    val model = parseModel(absSourceFileNames)

    val typeBuilds = buildTypes(typeSourceFileNames, model)
    val modelBuild = buildModel(model, typeBuilds, verificationConfig, enforcementConfig)

    modelToErlang(modelBuild.model)

    return modelBuild
}
