package de.ahbnr.sessiontypeabs

import org.abs_models.frontend.parser.Main
import org.abs_models.backend.erlang.ErlangBackend

import org.abs_models.frontend.ast.*

import org.abs_models.backend.prettyprint.DefaultABSFormatter

import java.util.Arrays
import java.util.EnumSet
import java.util.HashSet
import java.io.File
import java.io.PrintWriter

fun main(args: Array<String>) {
    val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val files = args.map{arg -> File(arg)} + listOf(helperLib)

    try {
        val model = Main.parseFiles(true, files)

        for (m in model.moduleDecls) {
            when {
                m.name.equals("Test") -> {
                    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

                    val automaton = SessionAutomaton(
                        HashSet(Arrays.asList(0, 1, 2)),
                        0,
                        HashSet(
                            Arrays.asList(
                                Transition(
                                    0,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                ),
                                Transition(
                                    1,
                                    TransitionVerb.InvocREv("m2", 1),
                                    2
                                )
                            )
                        ),
                        HashSet(),
                        HashSet()
                    )

                    m.addDecl(scheduler("scheduler", automaton))

                    for (d in m.decls) {
                        if (d.name.equals("C")) {
                            val cd = d as ClassDecl

                            introduceFields(cd, automaton)
                            introduceRegisters(cd, automaton)
                            introduceStateModifications(cd, automaton)
                            introduceSchedulerAnnotation(cd, "scheduler", automaton)
                        }
                    }
                }
                m.name.equals("FileTest") -> {
                    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

                    /*
                   -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                           --(p; invocREv m3; d |-> r1)--> q2
                           --(reactEv; d = r0)--> (q3)
                           --(p; invocREv m1; d |-> r0)--> q1
                  */

                    val automaton = SessionAutomaton(
                        HashSet(Arrays.asList(0, 1, 2, 3)),
                        0,
                        HashSet(
                            Arrays.asList(
                                Transition(
                                    0,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                ),
                                Transition(
                                    1,
                                    TransitionVerb.InvocREv("m3", 1),
                                    2
                                ),
                                Transition(
                                    2,
                                    TransitionVerb.ReactEv("m1", 0),
                                    3
                                ),
                                Transition(
                                    3,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                )
                            )
                        ),
                        HashSet(Arrays.asList(0, 1)),
                        HashSet()

                    )

                    val printer = PrintWriter(System.out)
                    val formatter = DefaultABSFormatter(printer)

                    val scheduler = scheduler("scheduler", automaton)
                    m.addDecl(scheduler)
                    scheduler.doPrettyPrint(printer, formatter)
                    printer.flush()

                    System.out.println("\n")

                    for (d in m.decls) {
                        if (d.name.equals("O")) {
                            val cd = d as ClassDecl

                            introduceFields(cd, automaton)
                            introduceRegisters(cd, automaton)
                            introduceStateModifications(cd, automaton)
                            introduceSchedulerAnnotation(cd, "scheduler", automaton)
                            introduceReactivationTransitions(cd, automaton)

                            cd.doPrettyPrint(printer, formatter)
                            printer.flush()
                        }
                    }
                }
                m.name.equals("Test3") -> {
                    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

                    /*
                   -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                           --(p; invocREv m3; d |-> r1)--> q2
                           --(reactEv; d = r0)--> (q3)
                           --(p; invocREv m1; d |-> r0)--> q1
                  */

                    val automaton = SessionAutomaton(
                        HashSet(Arrays.asList(0, 1, 2, 3)),
                        0,
                        HashSet(
                            Arrays.asList(
                                Transition(
                                    0,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                ),
                                Transition(
                                    1,
                                    TransitionVerb.InvocREv("m3", 1),
                                    2
                                ),
                                Transition(
                                    2,
                                    TransitionVerb.ReactEv("m1", 0),
                                    3
                                ),
                                Transition(
                                    3,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                )
                            )
                        ),
                        HashSet(Arrays.asList(0, 1)),
                        HashSet()

                    )

                    val printer = PrintWriter(System.out)
                    val formatter = DefaultABSFormatter(printer)

                    val scheduler = scheduler("scheduler", automaton)
                    m.addDecl(scheduler)
                    scheduler.doPrettyPrint(printer, formatter)
                    printer.flush()

                    System.out.println("\n")

                    for (d in m.decls) {
                        if (d.name.equals("O")) {
                            val cd = d as ClassDecl

                            introduceFields(cd, automaton)
                            introduceRegisters(cd, automaton)
                            introduceStateModifications(cd, automaton)
                            introduceSchedulerAnnotation(cd, "scheduler", automaton)
                            introduceReactivationTransitions(cd, automaton)

                            cd.doPrettyPrint(printer, formatter)
                            printer.flush()
                        }
                    }
                }
                m.name.equals("Test4") -> {
                    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

                    /*
                   -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                           --(p; invocREv m3; d |-> r1)--> q2
                           --(reactEv; d = r0)--> (q3)
                           --(p; invocREv m1; d |-> r0)--> q1
                  */

                    val automaton = SessionAutomaton(
                        HashSet(Arrays.asList(0, 1, 2, 3)),
                        0,
                        HashSet(
                            Arrays.asList(
                                Transition(
                                    0,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                ),
                                Transition(
                                    1,
                                    TransitionVerb.InvocREv("m3", 1),
                                    2
                                ),
                                Transition(
                                    2,
                                    TransitionVerb.ReactEv("m1", 0),
                                    3
                                ),
                                Transition(
                                    3,
                                    TransitionVerb.InvocREv("m1", 0),
                                    1
                                )
                            )
                        ),
                        HashSet(Arrays.asList(0, 1)),
                        HashSet()

                    )

                    val printer = PrintWriter(System.out)
                    val formatter = DefaultABSFormatter(printer)

                    val scheduler = scheduler("scheduler", automaton)
                    m.addDecl(scheduler)
                    scheduler.doPrettyPrint(printer, formatter)
                    printer.flush()

                    System.out.println("\n")

                    for (d in m.decls) {
                        if (d.name == "O") {
                            val cd = d as ClassDecl

                            introduceFields(cd, automaton)
                            introduceRegisters(cd, automaton)
                            introduceStateModifications(cd, automaton)
                            introduceSchedulerAnnotation(cd, "scheduler", automaton)
                            introduceReactivationTransitions(cd, automaton)

                            cd.doPrettyPrint(printer, formatter)
                            printer.flush()
                        }
                    }
                }
            }
        }

        val parser = Main()
        parser.analyzeFlattenAndRewriteModel(model)
        if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) {
            System.out.println("Parsing failed.")
            return
        }

        ErlangBackend().compile(model, File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
    } catch (e: Exception) {
        e.printStackTrace()
    }

}
