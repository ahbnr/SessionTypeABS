package de.ahbnr.sessiontypeabs.staticanalysis

import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.compiler.checkAndRewriteModel
import de.ahbnr.sessiontypeabs.compiler.parseModel
import de.ahbnr.sessiontypeabs.staticverification.resolveActorNames
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkModel
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import org.apache.commons.io.FileUtils

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * * Creation of the same future in different scopes is ok, if their method projections
 *   are equivalent
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModelAnalysisErrorTests {
    @ParameterizedTest
    @MethodSource("inputFilesProvider")
    fun `static model analysis`(testName: String) {
        val modelInput = ClassLoader.getSystemResourceAsStream("globaltypes/modelanalysis/ErrorExamples/$testName/$testName.abs")

        val modelFile = File.createTempFile("$testName", ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val model = parseModel(listOf(modelFile.absolutePath))
        checkAndRewriteModel(model)

        val typeInput = ClassLoader.getSystemResourceAsStream("globaltypes/modelanalysis/ErrorExamples/$testName/$testName.st")

        val globalType =
            resolveActorNames(
                model,
                parseGlobalType(typeInput!!, "$testName.st")
            )

        val typeBuild = buildTypes(listOf(globalType), model)

        assertThrows<ModelAnalysisException> {
            checkModel(
                model,
                typeBuild.typeBuilds.head
            )
        }
    }

    fun inputFilesProvider() = Stream.of(
        Arguments.of("InitBlock"),
        Arguments.of("RecoveryBlock"),
        Arguments.of("RunMethod")
    )

}