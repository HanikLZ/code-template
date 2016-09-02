package org.mdvsc.code.template

/**
 * @author HanikLZ
 * @since 2016/8/30
 */
interface VariableProcessor {
    fun processVariable(variable: String): String?
    fun processKeyCollection(variable: String): Collection<String>?
}

