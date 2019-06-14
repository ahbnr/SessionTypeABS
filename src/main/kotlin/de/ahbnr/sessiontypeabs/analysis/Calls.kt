package de.ahbnr.sessiontypeabs.analysis

import org.abs_models.frontend.ast.*

// Functions to analyse and trace calls

/**
 * Check whether a SyncCall calls into an object of a specific interface
 */
fun callsIntoInterface(call: SyncCall, interfaceDecl: InterfaceDecl) =
    call
        .calleeNoTransform
        .type
        .decl
        .qualifiedName == interfaceDecl.qualifiedName

/**
 * Returns the implementation of the method called by a given SyncCall object
 * if the callee implements one of the interfaces of the given class
 */
fun methodOfLocalSyncCall(call: SyncCall, context: ClassDecl): MethodImpl? {
    val methods = (
            if (context.superTypes.any{ interfaceDecl -> callsIntoInterface(call, interfaceDecl)}){
                context.methodsNoTransform
            }

            else {
                List()
            }
            )

    return methods
        .find{m -> call.method == m.methodSigNoTransform.name}
}
