package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.types.Future
import org.abs_models.frontend.ast.Exp
import org.abs_models.frontend.ast.FieldUse
import org.abs_models.frontend.ast.VarUse

data class StmtsEnvironment (
    val Futures: Set<Future>, // All futures to ever be created in the protocol being verified
    val actorModelMapping: ActorModelMapping, // Allows to match interfaces to actors
    val participants: Collection<String>, // names of participants
    val GetVariables: Map<Future, Set<String>> = emptyMap() // Maps futures to variables where their result has been stored
) {
    fun doesVariableStoreAGetValue(varUse: VarUse) =
        GetVariables.values.any{ varUse.name in it }

    fun doesFieldStoreFuture(fieldUse: FieldUse) =
        Future(fieldUse.name) in Futures

    fun isActor(exp: Exp) =
        actorModelMapping.findActorByType(exp.type) != null

}