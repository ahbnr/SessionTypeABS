package session_type_abs

import org.abs_models.frontend.ast.*;

fun introduceFields(clazz: ClassDecl, automaton: SessionAutomaton) =
  clazz.addField(
    FieldDecl(
      "q",
      intT(),
      Opt(
        IntLiteral(automaton.q0.toString())
      ),
      List(), // Annotations
      false // ABS.ast notes: "For Component Model". 
    )
  )

fun introduceRegisters(clazz: ClassDecl, automaton: SessionAutomaton) {
  automaton.registers.forEach {
    clazz.addField(
      FieldDecl(
        "r" + it.toString(),
        registerT(),
        Opt(
          nothingC()
        ),
        List(), // Annotations
        false // ABS.ast notes: "For Component Model". 
      )
    )
  }
}

fun registerName(register: Int) =
  "r" + register.toString()

fun registerNames(automaton: SessionAutomaton) =
  automaton.registers.map{
    register -> registerName(register)
  }

fun introduceSchedulerAnnotation(clazz: ClassDecl, schedfun: String, automaton: SessionAutomaton) =
  clazz.addAnnotation(
    schedulerAnnotation(
      schedfun,
      "queue",
      "q",
      *registerNames(automaton).toTypedArray()
    )
  )

fun stateModBlock(transition: Transition): Block {
  val stateMod = AssignStmt(
    List(), //Annotations
    FieldUse("q"),
    IntLiteral(transition.q2.toString())
  );

  val registerMod: AssignStmt? =
    when (transition.verb) {
      is TransitionVerb.InvocREv ->
        AssignStmt(
          List(), //Annotations
          FieldUse(registerName(transition.verb.register)),
          justC(ThisDestinyExp())
        )
      else -> null
    };

  return Block(
    List(), // Annotations
    List(
      *listOf(
        registerMod,
        stateMod
      ).filterNotNull().toTypedArray()
    )
  );
}


fun stateModSwitchCase(transitions: Set<Transition>) =
  CaseStmt(
    List(), // Annotations
    VarUse("q"),
    List(
      *(
        transitions.map{t ->
          CaseBranchStmt(
            LiteralPattern(IntLiteral(t.q1.toString())),
            stateModBlock(t)
          )
        } + List(
          CaseBranchStmt(
            UnderscorePattern(),
            Block(
              List(), // Annotations
              List(
                AssertStmt(
                  List(), // Annotations
                  falseC()
                )
              )
            )
          )
        )
      ).toTypedArray()
    )
  )

fun prependStmt(block: Block, stmt: Stmt) {
  val origStmtList = block.getStmtListNoTransform();

  // TODO: Inserting a node in front of the list does not work without a full
  // tree copy. I do not yet know why.
  val newStmtList = origStmtList.treeCopyNoTransform();
  newStmtList.insertChild(stmt, 0);

  block.setStmtList(newStmtList);
}

fun introduceStateModifications(methodImpl: MethodImpl, automaton: SessionAutomaton) {
  val transitions = automaton
    .transitionsForMethod(methodImpl.getMethodSigNoTransform().getName());

  val stateModStmt = stateModSwitchCase(transitions);

  prependStmt(methodImpl.getBlockNoTransform(), stateModStmt);
}

fun introduceStateModifications(clazz: ClassDecl, automaton: SessionAutomaton) {
  val affectedMethods = automaton.affectedMethods();

  //INFO: Use lookupMethod() instead?
  clazz.getMethodsNoTransform().forEach {
    if (affectedMethods.contains(it.getMethodSigNoTransform().getName())) {
      introduceStateModifications(it, automaton);
    }
  }
}

//fun findCallPoints(clazz: ClassDecl, automaton) = 0
// ERLANG: #process_info{destiny=Fut} = get(process_info),
// TODO: ContractInference:_ src/main/java/deadlock/analyser/inference/ContractInference.java
// CallResolver?: src/main/java/org/abs_models/frontend/delta/OriginalCallResolver.jadd
