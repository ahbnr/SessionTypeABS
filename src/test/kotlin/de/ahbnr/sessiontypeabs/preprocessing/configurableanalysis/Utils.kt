package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import org.apache.commons.io.IOUtils

fun loadType(input: String): GlobalType =
    parseGlobalType(
        IOUtils.toInputStream(input)
    )

