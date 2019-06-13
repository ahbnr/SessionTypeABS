package de.ahbnr.sessiontypeabs

import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.genAutomaton
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.parser.Main
import java.io.File

import org.abs_models.frontend.ast.*
import java.io.PrintWriter
import java.util.*

fun compile(absSourceFileNames: List<String>, classToType: Map<String, LocalType>) {
    val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    // TODO make printing optional:
    val printer = PrintWriter(System.out)
    val formatter = DefaultABSFormatter(printer)

    try {
        val model = Main.parseFiles(true, files)

        for (m in model.moduleDecls) {
            // TODO: Check if import is already present. Only import, if a class is present for which types are available
            m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

            var schedulerNameCounter = 0

            for (decl in m.decls) {
                if (decl is ClassDecl && classToType.contains(decl.qualifiedName)) {
                    val type = classToType[decl.qualifiedName]!!
                    val automaton = genAutomaton(type)

                    val schedulerName = "sched" + schedulerNameCounter++
                    val scheduler = scheduler(schedulerName, automaton)

                    m.addDecl(scheduler)

                    // TODO Extract into one external method
                    introduceFields(decl, automaton)
                    introduceRegisters(decl, automaton)
                    introduceStateModifications(decl, automaton)
                    introduceSchedulerAnnotation(decl, schedulerName, automaton)
                    introduceReactivationTransitions(decl, automaton)

                    // TODO make printing optional:
                    println("Modified ${decl.name}:")
                    scheduler.doPrettyPrint(printer, formatter)
                    printer.flush()

                    System.out.println("\n")

                    decl.doPrettyPrint(printer, formatter)
                    printer.flush()
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