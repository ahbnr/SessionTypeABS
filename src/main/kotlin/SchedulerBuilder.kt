package session_type_abs

import org.abs_models.frontend.ast.*;

fun matchNamesOrRegistersForState(state: Int, automaton: SessionAutomaton): FnApp {
  val transitionVerbs = automaton.transitionsForState(state).map{t -> t.verb};
  val methodNames = mutableSetOf<String>();
  val registers = mutableSetOf<Int>();

  transitionVerbs.forEach {
    when(it) {
      is TransitionVerb.InvocREv -> methodNames.add(it.method)
      is TransitionVerb.ReactEv -> registers.add(it.register)
    }
  }

  return matchNamesOrRegisters(methodNames, registers);
}

fun stateSwitchCase(stateParam: String, automaton: SessionAutomaton) =
  CaseExp(
    VarUse(stateParam),
    List(
      *(
        automaton.Q.map{q ->
          CaseBranch(
            LiteralPattern(IntLiteral(q.toString())),
            matchNamesOrRegistersForState(q, automaton)
          )
        } + List(
          CaseBranch(
            UnderscorePattern(),
            nothingC()
          )
        )
      ).toTypedArray()
    )
  )

fun scheduler(name: String, automaton: SessionAutomaton) =
  funDecl(
    maybeT(processT()),
    name,
    List(
      queueParameter(),
      stateParameter(),
      *registerParameters(automaton)
    ),
    applyProtocol(
      lambdaDecl(
        List(queueParameter()),
        stateSwitchCase("q", automaton)
      ),
      automaton.affectedMethods(),
      VarUse("queue")
    )
  )

fun schedulerAnnotation(schedfun: String, vararg params: String) =
  TypedAnnotation(
    FnApp(
      schedfun,
      List(*(params.map{s -> VarUse(s)}.toTypedArray()))
    ),
    DataTypeUse("Scheduler", List())
  )
