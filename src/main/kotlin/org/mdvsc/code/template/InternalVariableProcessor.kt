package org.mdvsc.code.template

/**
 * @author HanikLZ
 * @since 2016/8/31
 */
internal class InternalVariableProcessor(): VariableProcessor {

    override fun processKeyCollection(variable: String) = emptyList<String>()

    override fun processVariable(variable: String): String {
        return variable
    }

}

