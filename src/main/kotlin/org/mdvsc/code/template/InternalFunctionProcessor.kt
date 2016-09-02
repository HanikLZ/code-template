package org.mdvsc.code.template

/**
 * @author HanikLZ
 * @since 2016/8/31
 */
internal class InternalFunctionProcessor(): FunctionProcessor {

    override fun processFunction(function: String, args: List<String>) = when (function) {
        "toUpperCase" -> args[0].toUpperCase()
        "toLowerCase" -> args[0].toLowerCase()
        "firstUpperCase" -> args[0].run { if (isNotEmpty()) "${this[0].toUpperCase()}${substring(1)}" else this }
        "firstLowerCase" -> args[0].run { if (isNotEmpty()) "${this[0].toLowerCase()}${substring(1)}" else this }
        else -> ""
    }

}
