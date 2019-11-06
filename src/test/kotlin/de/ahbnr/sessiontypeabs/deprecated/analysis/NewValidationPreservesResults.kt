package de.ahbnr.sessiontypeabs.deprecated.analysis

import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.SuspensionInfo
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.assertj.core.api.Assertions.assertThat
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewValidationPreservesResults {
    @ParameterizedTest
    @MethodSource("inputFilesProvider")
    fun `compare results of old and new session type validation`(fileName: String) {
        val typeInput = ClassLoader.getSystemResourceAsStream( fileName )
        val parsedType =
            parseGlobalType(typeInput!!, fileName)

        val newExecution =
            de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.execute(
                CombinedAnalysis(), parsedType
            )

        val oldExecution =
            execute(CombinedDomain(), parsedType)

        assertThat(newExecution.postState.getParticipants())
            .containsExactlyInAnyOrderElementsOf(oldExecution.postState.getParticipants())

        assertThat(newExecution.postState.getActiveFutures())
            .containsExactlyInAnyOrderElementsOf(oldExecution.postState.getActiveFutures())

        assertThat(newExecution.postState.getFuturesToTargetMapping().map {
                (future, binding) -> future to Pair(binding.actor, binding.method)
        })
            .containsExactlyInAnyOrderElementsOf(
                oldExecution.postState.getFuturesToTargetMapping().map { (actor, binding) -> actor to binding }
            )

        assertThat(newExecution.postState.getActiveFutures().map {
            newExecution.postState.getSuspensionsOnFuture(it)
                .map { SuspensionInfo(it.suspendedClass, it.futureAfterReactivation) }
                .toList()
        })
            .containsExactlyInAnyOrderElementsOf(oldExecution.postState.getActiveFutures().map {
                oldExecution.postState.getSuspensionsOnFuture(it)
            })

        assertThat(newExecution.postState.getParticipants().map {
            newExecution.postState.getActiveFuture(it)
        })
            .containsExactlyInAnyOrderElementsOf(oldExecution.postState.getParticipants().map {
                oldExecution.postState.getActiveFuture(it)
            })
    }

    fun inputFilesProvider() = Stream.of(
        Arguments.of("globaltypes/modelanalysis/Initialization/Initialization.st"),
        Arguments.of("globaltypes/modelanalysis/Release/Release.st"),
        Arguments.of("globaltypes/modelanalysis/Fetching/Fetching.st"),
        Arguments.of("globaltypes/modelanalysis/Interactions/Interactions.st"),
        Arguments.of("globaltypes/modelanalysis/Repetition/Repetition.st"),
        Arguments.of("globaltypes/modelanalysis/Branching1/Branching1.st"),
        Arguments.of("globaltypes/modelanalysis/Branching2/Branching2.st"),
        Arguments.of("globaltypes/modelanalysis/Branching3/Branching3.st"),
        Arguments.of("reordering/consecutive_calls/ConsecutiveCalls.st"),
        Arguments.of("reordering/repeated_branchings/RepeatedBranchings.st"),
        Arguments.of("reordering/repeated_react/RepeatedReact.st"),
        Arguments.of("scenarios/grading_system/GradingSystem.st"),
        Arguments.of("scenarios/printer/Printer.st"),
        Arguments.of("scenarios/responsive_ui/ResponsiveUI.st"),
        Arguments.of("scenarios/responsive_ui/ResponsiveUIWrongPostCond.st"),
        Arguments.of("scenarios/same_prefix_different_branches/SamePrefixDifferentBranches.st"),
        Arguments.of("scenarios/same_prefix_different_branches/SamePrefixDifferentBranches2.st"),
        Arguments.of("evaluation_examples/consecutive_calls/model.st"),
        Arguments.of("evaluation_examples/grading_system/GradingSystem.st"),
        Arguments.of("evaluation_examples/notification_service/NotificationService.st"),
        Arguments.of("evaluation_examples/responsive_ui/ResponsiveUI.st")
    )
}