package de.ahbnr.sessiontypeabs.compiler

import org.abs_models.frontend.ast.*
import org.junit.jupiter.api.Test
import java.io.File
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy


/**
 * Static analysis requires that some features of the ABS language are rewritten by JastAdd to CoreABS.
 * Otherwise, analysis would be even more complicated.
 * On the other hand, there are some non CoreABS features, which are not rewritten and need to be supported.
 *
 * The tests of this class confirm the following:
 *
 * The following rewrites are expected:
 *
 * * AwaitAsyncCall -> AsyncCall + VarDecl + GetExp (GenerateCoreAbs.jadd)
 * * ForeachStmt -> While + ... (GenerateCoreAbs.jadd)
 * * When "this." is omitted when accessing a field, the VarUse should be rewritten to a FieldUse
 *
 * The following non-CoreABS features are not rewritten:
 *
 *  * CaseStmt
 *  * ExpressionStmt
 */
class RewritesTest {
    @Test
    fun `AwaitAsyncCall is rewritten and not part of final model`() {
        assertRewrite(
            "AwaitAsyncCall",
            AwaitAsyncCall::class.java,
            "There is an AwaitAsyncCall in the tree, although it should have been rewritten to CoreABS by now."
        )
    }

    @Test
    fun `ForeachStmt is rewritten and not part of final model`() {
        assertRewrite(
            "ForeachStmt",
            ForeachStmt::class.java,
            "There is an ForeachStmt in the tree, although it should have been rewritten to CoreABS by now."
        )
    }

    @Test
    fun `VarUse is rewritten to FieldUse, if the user omitted "this" and thus FieldUse should be part of final model`() {
        val filePath = getFilePath("VarUseFieldUse")
        val model = compileModel(filePath)

        // Check first, that we can indeed detect VarUses by var name
        val maybeVarUse = model
            .findChildren(VarUse::class.java)
            .find {
                it.name == "iAmAVar"
            }

        assertThat(maybeVarUse)
            .describedAs("Could not find a VarUse for variable iAmAVar, though it should be there.")
            .isNotNull()

        val maybeVarUseForField = model
            .findChildren(VarUse::class.java)
            .find {
                it.name == "iAmAField"
            }

        assertThat(maybeVarUseForField)
            .describedAs("There is still a VarUse of a field in the tree, although it should have been rewritten to a FieldUse by now.")
            .isNull()

        val maybeFieldUse = model
            .findChildren(FieldUse::class.java)
            .find {
                it.name == "iAmAField"
            }

        assertThat(maybeFieldUse)
            .describedAs("There is no FieldUse in the model, though one should have been generated due to rewrites.")
            .isNotNull()
    }

    @Test
    fun `CaseStmt is not rewritten and thus part of final model`() {
        assertNoRewrite(
            "CaseStmt",
            CaseStmt::class.java,
            "There is no CaseStmt in the tree, although it should not have been rewritten."
        )
    }

    @Test
    fun `ExpressionStmt is not rewritten and thus part of final model`() {
        assertNoRewrite(
            "ExpressionStmt",
            ExpressionStmt::class.java,
            "There is no ExpressionStmt in the tree, although it should not have been rewritten."
        )
    }

    private fun getFilePath(name: String): String {
        val modelInput = ClassLoader.getSystemResourceAsStream("rewriting/$name.abs")

        val modelFile = File.createTempFile(name, ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        return modelFile.absolutePath
    }

    private fun compileModel(filePath: String): Model {
        val model = parseModel(listOf(filePath))
        checkAndRewriteModel(model)

        return model
    }

    private fun <T> assertNodeNotPresent(model: Model, nodeType: Class<T>, message: String) {
        assertThat(model.findChildren(nodeType).isEmpty())
            .describedAs(message)
            .isTrue()
    }

    private fun <T> assertNodePresent(model: Model, nodeType: Class<T>, message: String) {
        assertThat(model.findChildren(nodeType).isNotEmpty())
            .describedAs(message)
            .isTrue()
    }

    private fun <T> assertRewrite(testFileBaseName: String, nodeTypeToBeRewritten: Class<T>, failureMessage: String) {
        val filePath = getFilePath(testFileBaseName)
        val model = compileModel(filePath)
        assertNodeNotPresent(model, nodeTypeToBeRewritten, failureMessage)
        assertThatThrownBy {
            assertNodePresent(model, nodeTypeToBeRewritten, "Checked successfully for absence of node, but it is not present either, which should be impossible.")
        }
        model.doFullTraversal()
        assertNodeNotPresent(model, nodeTypeToBeRewritten, failureMessage)
        assertThatThrownBy {
            assertNodePresent(model, nodeTypeToBeRewritten, "Checked successfully for absence of node, but it is not present either, which should be impossible.")
        }
    }

    private fun <T> assertNoRewrite(testFileBaseName: String, nodeTypeNotToBeRewritten: Class<T>, failureMessage: String) {
        val filePath = getFilePath(testFileBaseName)
        val model = compileModel(filePath)
        assertNodePresent(model, nodeTypeNotToBeRewritten, failureMessage)
        assertThatThrownBy {
            assertNodeNotPresent(model, nodeTypeNotToBeRewritten, "Checked successfully for presence of node, but it is not absent either, which should be impossible.")
        }
        model.doFullTraversal()
        assertNodePresent(model, nodeTypeNotToBeRewritten, failureMessage)
        assertThatThrownBy {
            assertNodeNotPresent(model, nodeTypeNotToBeRewritten, "Checked successfully for presence of node, but it is not absent either, which should be impossible.")
        }
    }

}