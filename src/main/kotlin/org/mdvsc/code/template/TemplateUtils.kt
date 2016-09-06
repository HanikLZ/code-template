package org.mdvsc.code.template

import java.util.*
import java.util.regex.Pattern

/**
 * 代码块控制，定义为 !{expression}....!{end}
 */
internal val controlPattern = Pattern.compile("!\\{.*?}")!!
internal val expressionPattern = Pattern.compile("#\\{.*?}")!!
internal val stringPattern = Pattern.compile("'[^.]*'|\"[^\"]*\"")!!

/**
 * 提取模板标签中的表达式字符串
 */
internal fun String.expression() = substring(2, length - 1)
internal fun String.pair(split: String = "=") = indexOf(split).run { if (this > 0) Pair(substring(0, this).trim(), substring(this + split.length).trim()) else Pair("", this@pair)}
internal fun String.isStringValue() = stringPattern.matcher(this).matches()
internal fun String.stringValue() = if (isStringValue()) substring(1, length - 1) else this
internal fun StringSplitter.toList() = ArrayList<String>().apply { while (hasNext()) {add(next())} }


/**
 * @author HanikLZ
 * @since 2016/8/29
 */
internal class StringSplitter(val delimiter: Char): Iterable<String>, Iterator<String> {

    private var position = 0
    private var length = 0

    var string = ""
        set(value) {
            field = value
            position = 0
            length = field.length
        }

    override operator fun iterator(): Iterator<String> {
        return this
    }

    override fun hasNext(): Boolean {
        return position < length
    }

    override fun next(): String {
        var end = string.indexOf(delimiter, position)
        if (end == -1) {
            end = length
        }
        val nextString = string.substring(position, end)
        position = end + 1 // Skip the delimiter.
        return nextString
    }

    fun resetString() = string.substring(position)

}

