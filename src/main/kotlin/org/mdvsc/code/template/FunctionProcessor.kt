package org.mdvsc.code.template

/**
 * @author HanikLZ
 * @since 2016/8/30
 */
interface FunctionProcessor {
    fun processFunction(function: String, args: List<String>): String?
}

