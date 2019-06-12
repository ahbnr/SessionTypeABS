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
    .transitionsForMethod(methodImpl.getMethodSigNoTransform().getName())
    .filter{t -> t.verb is TransitionVerb.InvocREv}
    .toSet();

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

/**
 * Build an AST to apply a state transition for reactivations.
 * Outputs a SKIP statement, if the Transition is not a ReactEv.
 */
fun reactivationStateTransition(t: Transition) =
  if (t.verb is TransitionVerb.ReactEv) {
    ifThen(
      AndBoolExp(
        EqExp(FieldUse("q"), IntLiteral(t.q1.toString())),
        EqExp(justC(ThisDestinyExp()), FieldUse(registerName(t.verb.register)))
      ),
      AssignStmt(
        List(), // no annotations
        FieldUse("q"),
        IntLiteral(t.q2.toString())
      )
    )
    /* Result:
      if (q == X && Just(thisDestiny) == rI) { // TODO: Aussagekräftig genug? Werden Register je geleert? Vielleicht relevant nicht im Protokoll genannte Funktionen auch aufgerufen werden können
        q = Y;
      }
    */
  }

  else {
    SkipStmt()
  }

/**
 * Example result:
 *
 * Original:
 * await f?;
 *
 * Replacement:
 * {
 *   await f?;
 *   <state transitions>
 * }
 */
fun awaitReplacement(awaitStmt: AwaitStmt, transitionStmts: Array<Stmt>) =
  Block(
    List(), //no annotations
    List(
      awaitStmt.treeCopyNoTransform(),
      *transitionStmts
    )
  )

/**
 * Example result:
 *
 * Original:
 * suspend;
 *
 * Replacement:
 * {
 *   suspend;
 *   <state transitions>
 * }
 */
fun suspendReplacement(suspendStmt: SuspendStmt, transitionStmts: Array<Stmt>) =
  Block(
    List(), //Annotations
    List(
      suspendStmt.treeCopyNoTransform(),
      *transitionStmts
    )
  )

/**
 * TODO: Handle Unit return type!
 *
 * Example result:
 *
 * Original:
 * if (await o!m()) {
 *   ...
 * }
 *
 * Replacement:
 * {
 *   Bool awaitCallCache0 = await o!m()M
 *   <state transitions>
 *   if (awaitCallCache0) {
 *     ...
 *   }
 * }
 */
fun awaitAsyncCallReplacement(awaitAsyncCall: AwaitAsyncCall, transitionStmts: Array<Stmt>, variableNameCount: Int) {
  val varName = "awaitCallCache" + variableNameCount.toString();
  val expTypeName = awaitAsyncCall.getType().getDecl().getQualifiedName();
  val stmtParent = awaitAsyncCall.closestParent(Stmt::class.java);
  val expDeepCopy = awaitAsyncCall.treeCopyNoTransform();

  awaitAsyncCall.replaceWith(
    VarUse(varName)
  );

  // TODO: Throw exception, if no parent?
  stmtParent?.replaceWith(
    Block(
      List(), //Annotations
      List(
        VarDeclStmt(
          List(), // no annotations
          VarDecl(
            varName,
            UnresolvedTypeUse(
              expTypeName,
              List() // no annotations
            ),
            Opt(expDeepCopy)
          )
        ),
        *transitionStmts,
        stmtParent.treeCopyNoTransform()
      )
    )
  )
}

fun introduceReactivationTransitions(methodImpl: MethodImpl, context: ClassDecl, automaton: SessionAutomaton) {
  val methodName = methodImpl.getMethodSigNoTransform().getName();

  // Gather all reactivation statements concerning the method and build the
  // corresponding state transition ASTs
  val transitionStmts = automaton
    .transitionsForMethod(methodName)
    .filter{t -> t.verb is TransitionVerb.ReactEv}
    .map{t -> reactivationStateTransition(t)}
    .toTypedArray();

  if (!transitionStmts.isEmpty()) {
    // Since we may need to construct additional variables, we need a continous
    // counter to avoid duplicate variable names.
    var variableNameCount = 0;

    findReactivationPoints(methodImpl, context, mutableSetOf()).forEach {
      when (it) {
        is ReactivationPoint.Await -> it.awaitStmt.replaceWith( // TODO: Achtung: AwaitStmt und EffExp mixbar?
          awaitReplacement(it.awaitStmt, transitionStmts)
        )
        is ReactivationPoint.AwaitAsyncCall ->
          awaitAsyncCallReplacement(it.awaitAsyncCall, transitionStmts, variableNameCount++)
        is ReactivationPoint.Suspend -> it.suspendStmt.replaceWith(
          suspendReplacement(it.suspendStmt, transitionStmts)
        )
      }
    }
  }
}
  
fun introduceReactivationTransitions(classDecl: ClassDecl, automaton: SessionAutomaton) {
  val affectedMethods = automaton.affectedMethods();

  classDecl
    .getMethodsNoTransform()
    .filter{m -> affectedMethods.contains(m.getMethodSigNoTransform().getName())}
    .forEach {
      introduceReactivationTransitions(it, classDecl, automaton);
    };
}

//fun findCallPoints(clazz: ClassDecl, automaton) = 0
// ERLANG: #process_info{destiny=Fut} = get(process_info),
// TODO: ContractInference:_ src/main/java/deadlock/analyser/inference/ContractInference.java
// CallResolver?: src/main/java/org/abs_models/frontend/delta/OriginalCallResolver.jadd
