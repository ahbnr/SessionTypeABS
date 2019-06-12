package de.ahbnr.sessiontypeabs;

import org.abs_models.frontend.parser.Main;
import org.abs_models.backend.erlang.ErlangBackend;

import org.abs_models.frontend.ast.*;

import org.abs_models.backend.prettyprint.DefaultABSFormatter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.PrintWriter;

public class SessionTypeABS {
    public static void main(String[] args) {
        final File helperLib = new File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").getFile());

        final java.util.List<File> files = Stream.concat(
                Stream.of(helperLib),
                Arrays
                        .stream(args)
                        .map(File::new)
        ).collect(Collectors.toList());

        try {
            final Main parser = new Main();
            final Model model = parser.parseFiles(true, files);

            for (ModuleDecl m : model.getModuleDecls()) {
                if (m.getName().equals("Test")) {
                    m.addImport(new StarImport("SessionTypeABS.SchedulerHelpers"));

                    final SessionAutomaton automaton = new SessionAutomaton(
                            new HashSet<>(Arrays.asList(0, 1, 2)),
                            0,
                            new HashSet<>(Arrays.asList(
                                    new Transition(
                                            0,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    ),
                                    new Transition(
                                            1,
                                            new TransitionVerb.InvocREv("m2", 1),
                                            2
                                    )
                            )),
                            new HashSet<>()
                    );

                    m.addDecl(SchedulerBuilderKt.scheduler("scheduler", automaton));

                    for (Decl d : m.getDecls()) {
                        if (d.getName().equals("C")) {
                            ClassDecl cd = (ClassDecl) d;

                            ClassModificationsKt.introduceFields(cd, automaton);
                            ClassModificationsKt.introduceRegisters(cd, automaton);
                            ClassModificationsKt.introduceStateModifications(cd, automaton);
                            ClassModificationsKt.introduceSchedulerAnnotation(cd, "scheduler", automaton);
                        }
                    }
                }

                else if (m.getName().equals("FileTest")) {
                    m.addImport(new StarImport("SessionTypeABS.SchedulerHelpers"));

          /*
           -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                   --(p; invocREv m3; d |-> r1)--> q2
                   --(reactEv; d = r0)--> (q3)
                   --(p; invocREv m1; d |-> r0)--> q1
          */

                    final SessionAutomaton automaton = new SessionAutomaton(
                            new HashSet<>(Arrays.asList(0, 1, 2, 3)),
                            0,
                            new HashSet<>(Arrays.asList(
                                    new Transition(
                                            0,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    ),
                                    new Transition(
                                            1,
                                            new TransitionVerb.InvocREv("m3", 1),
                                            2
                                    ),
                                    new Transition(
                                            2,
                                            new TransitionVerb.ReactEv("m1", 0),
                                            3
                                    ),
                                    new Transition(
                                            3,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    )
                            )),
                            new HashSet<>(Arrays.asList(0, 1))
                    );

                    final PrintWriter printer = new PrintWriter(System.out);
                    final DefaultABSFormatter formatter = new DefaultABSFormatter(printer);

                    final FunctionDecl scheduler = SchedulerBuilderKt.scheduler("scheduler", automaton);
                    m.addDecl(scheduler);
                    scheduler.doPrettyPrint(printer, formatter);
                    printer.flush();

                    System.out.println("\n");

                    for (Decl d : m.getDecls()) {
                        if (d.getName().equals("O")) {
                            ClassDecl cd = (ClassDecl) d;

                            ClassModificationsKt.introduceFields(cd, automaton);
                            ClassModificationsKt.introduceRegisters(cd, automaton);
                            ClassModificationsKt.introduceStateModifications(cd, automaton);
                            ClassModificationsKt.introduceSchedulerAnnotation(cd, "scheduler", automaton);
                            ClassModificationsKt.introduceReactivationTransitions(cd, automaton);

                            cd.doPrettyPrint(printer, formatter);
                            printer.flush();
                        }
                    }
                }

                else if (m.getName().equals("Test3")) {
                    m.addImport(new StarImport("SessionTypeABS.SchedulerHelpers"));

          /*
           -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                   --(p; invocREv m3; d |-> r1)--> q2
                   --(reactEv; d = r0)--> (q3)
                   --(p; invocREv m1; d |-> r0)--> q1
          */

                    final SessionAutomaton automaton = new SessionAutomaton(
                            new HashSet<>(Arrays.asList(0, 1, 2, 3)),
                            0,
                            new HashSet<>(Arrays.asList(
                                    new Transition(
                                            0,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    ),
                                    new Transition(
                                            1,
                                            new TransitionVerb.InvocREv("m3", 1),
                                            2
                                    ),
                                    new Transition(
                                            2,
                                            new TransitionVerb.ReactEv("m1", 0),
                                            3
                                    ),
                                    new Transition(
                                            3,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    )
                            )),
                            new HashSet<>(Arrays.asList(0, 1))
                    );

                    final PrintWriter printer = new PrintWriter(System.out);
                    final DefaultABSFormatter formatter = new DefaultABSFormatter(printer);

                    final FunctionDecl scheduler = SchedulerBuilderKt.scheduler("scheduler", automaton);
                    m.addDecl(scheduler);
                    scheduler.doPrettyPrint(printer, formatter);
                    printer.flush();

                    System.out.println("\n");

                    for (Decl d : m.getDecls()) {
                        if (d.getName().equals("O")) {
                            ClassDecl cd = (ClassDecl) d;

                            ClassModificationsKt.introduceFields(cd, automaton);
                            ClassModificationsKt.introduceRegisters(cd, automaton);
                            ClassModificationsKt.introduceStateModifications(cd, automaton);
                            ClassModificationsKt.introduceSchedulerAnnotation(cd, "scheduler", automaton);
                            ClassModificationsKt.introduceReactivationTransitions(cd, automaton);

                            cd.doPrettyPrint(printer, formatter);
                            printer.flush();
                        }
                    }
                }

                else if (m.getName().equals("Test4")) {
                    m.addImport(new StarImport("SessionTypeABS.SchedulerHelpers"));

          /*
           -> (q0) --(p; invocREv m1; d |-> r0)--> q1
                   --(p; invocREv m3; d |-> r1)--> q2
                   --(reactEv; d = r0)--> (q3)
                   --(p; invocREv m1; d |-> r0)--> q1
          */

                    final SessionAutomaton automaton = new SessionAutomaton(
                            new HashSet<>(Arrays.asList(0, 1, 2, 3)),
                            0,
                            new HashSet<>(Arrays.asList(
                                    new Transition(
                                            0,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    ),
                                    new Transition(
                                            1,
                                            new TransitionVerb.InvocREv("m3", 1),
                                            2
                                    ),
                                    new Transition(
                                            2,
                                            new TransitionVerb.ReactEv("m1", 0),
                                            3
                                    ),
                                    new Transition(
                                            3,
                                            new TransitionVerb.InvocREv("m1", 0),
                                            1
                                    )
                            )),
                            new HashSet<>(Arrays.asList(0, 1))
                    );

                    final PrintWriter printer = new PrintWriter(System.out);
                    final DefaultABSFormatter formatter = new DefaultABSFormatter(printer);

                    final FunctionDecl scheduler = SchedulerBuilderKt.scheduler("scheduler", automaton);
                    m.addDecl(scheduler);
                    scheduler.doPrettyPrint(printer, formatter);
                    printer.flush();

                    System.out.println("\n");

                    for (Decl d : m.getDecls()) {
                        if (d.getName().equals("O")) {
                            ClassDecl cd = (ClassDecl) d;

                            ClassModificationsKt.introduceFields(cd, automaton);
                            ClassModificationsKt.introduceRegisters(cd, automaton);
                            ClassModificationsKt.introduceStateModifications(cd, automaton);
                            ClassModificationsKt.introduceSchedulerAnnotation(cd, "scheduler", automaton);
                            ClassModificationsKt.introduceReactivationTransitions(cd, automaton);

                            cd.doPrettyPrint(printer, formatter);
                            printer.flush();
                        }
                    }
                }
            }

            parser.analyzeFlattenAndRewriteModel(model);
            if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) {
                System.out.println("Parsing failed.");
                return;
            }

            new ErlangBackend().compile(model, new File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions.class));
        }

        catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
