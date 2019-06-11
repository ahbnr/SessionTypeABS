package session_type_abs

import org.abs_models.frontend.ast.*;

// Types
fun registerT() = 
  UnresolvedTypeUse(
    "SessionTypeABS.SchedulerHelpers.Register",
    List() //Annotations
  )

// Common parameters
fun queueParameter() =
  ParamDecl(
    "queue",
    listT(processT()),
    List() // Annotations
  )

fun stateParameter() =
  ParamDecl(
    "q",
    intT(),
    List() // Annotations
  )

fun registerParameters(automaton: SessionAutomaton) =
  registerNames(automaton)
    .map{registerName ->
      ParamDecl(
        registerName,
        registerT(),
        List() // Annotations
      )
    }
    .toTypedArray()

fun processParameter() =
  ParamDecl(
    "p",
    processT(),
    List() // Annotations
  )

// Functions

fun matchNamesOrRegisters(whitelist: Set<String>, registers: Set<Int>) =
  FnApp(
    "SessionTypeABS.SchedulerHelpers.matchNamesOrRegisters",
    List(
      setC(*whitelist.map{methodName -> StringLiteral(methodName)}.toTypedArray()),
      setC(*registers.map{register -> VarUse(registerName(register))}.toTypedArray()),
      VarUse("queue")
    )
  )
  
fun applyProtocol(protocol: ParFnAppParam, participants: Set<String>, queue: PureExp) =
  callHigherOrderFun(
    "SessionTypeABS.SchedulerHelpers.applyProtocol",
    protocol,
    setC(*participants.map{methodName -> StringLiteral(methodName)}.toTypedArray()),
    queue
  )

fun forceInit(schedulerFun: ParFnAppParam, queue: PureExp) =
  callHigherOrderFun(
    "SessionTypeABS.SchedulerHelpers.forceInit",
    schedulerFun,
    queue
  )

fun selectAsideBlacklist(blacklist: Set<String>, queue: PureExp) =
  FnApp(
    "SessionTypeABS.SchedulerHelpers.selectAsideBlacklist",
    List(
      setC(*blacklist.map{methodName -> StringLiteral(methodName)}.toTypedArray()),
      queue
    )
  )
