package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.compiler.checkAndRewriteModel
import de.ahbnr.sessiontypeabs.compiler.parseModel
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.model.checkModel
import org.apache.commons.io.FileUtils

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * FIXME: Note the following changes in the thesis:
 *
 * * Creation of the same future in different scopes is ok, if their method projections
 *   are equivalent
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModelAnalysisTests {
    @ParameterizedTest
    @MethodSource("inputFilesProvider")
    fun `static model analysis`(testName: String) {
        val modelInput = ClassLoader.getSystemResourceAsStream("globaltypes/modelanalysis/$testName/$testName.abs")

        val modelFile = File.createTempFile("$testName", ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val model = parseModel(listOf(modelFile.absolutePath))
        checkAndRewriteModel(model)

        val typeInput = ClassLoader.getSystemResourceAsStream("globaltypes/modelanalysis/$testName/$testName.st")

        val globalType =
            resolveActorNames(
                model,
                parseGlobalType(typeInput!!, "$testName.st")
            )

        val typeBuild = buildTypes(listOf(globalType))

        assertDoesNotThrow(
            "Analysis decided model does not fulfill the given session type, although that should be the case."
        ) {
            checkModel(
                model,
                typeBuild.typeBuilds.head
            )
        }
    }

    fun inputFilesProvider() = Stream.of(
        Arguments.of("Initialization"),
        Arguments.of("Release"),
        Arguments.of("Fetching"),
        Arguments.of("Interactions"),
        Arguments.of("Repetition"),
        Arguments.of("Branching1"),
        Arguments.of("Branching2"),
        Arguments.of("Branching3")
    )

}