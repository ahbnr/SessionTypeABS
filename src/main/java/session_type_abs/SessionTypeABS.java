package session_type_abs;

import org.abs_models.frontend.parser.Main;
import org.abs_models.backend.erlang.ErlangBackend;

import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.KindedName.Kind;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;

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
                  new TransitionVerb.InvocREv("m1"),
                  1
                ),
                new Transition(
                  1,
                  new TransitionVerb.InvocREv("m2"),
                  2
                )
            )),
            new HashSet<>()
          );

          m.addDecl(SchedulerBuilderKt.scheduler("scheduler", automaton));

          for (Decl d : m.getDecls()) {
            if (d.getName().equals("C")) {
              ClassDecl cd = (ClassDecl) d;
              cd.addAnnotation(
                  SchedulerBuilderKt.schedulerAnnotation("scheduler", "queue", "q")
              );
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
