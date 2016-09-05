package org.mdvsc.code.template

import java.io.BufferedReader
import java.io.File
import java.util.*

/**
 * 模板处理器
 * @author HanikLZ
 * @since 2016/8/29
 */
object TemplateParser {

    private const val lineUnfinishedFlag = "!{\\}"

    private fun Template.compressLine() {
        baseCodeList.compressLine()
        blockMap.values.forEach {
            if (it is MutableList<String>) {
                it.compressLine()
            }
        }
    }

    private fun MutableList<String>.compressLine() {
        val newList = ArrayList<StringBuilder>()
        var lineBuilder: StringBuilder? = null
        var appendLine = true
        forEach {
            if (it == lineUnfinishedFlag) {
                appendLine = true
            } else {
                if (appendLine && lineBuilder != null) {
                    lineBuilder!!.append(it)
                } else {
                    newList.add(StringBuilder(it).apply { lineBuilder = this })
                }
                appendLine = false
            }
        }
        clear()
        newList.mapTo(this, { it.toString() })
    }

    private fun MutableList<String>.addUnfinishedFlag() = add(lineUnfinishedFlag)

    /**
     * 处理模板中的一行数据
     * 模板代码块表
     * 根据 !{if} / !{repeat} / !{elif} / !{else} 语句块作为 key，对应其具体的语句块
     * 按文件代码块入口为 !{file}
     * 未被块语句包括的部分，key 为 ""
     * @param reader reader
     * @param line 文本行
     * @param blockStack 所属代码块列表
     * @return 如果遇到标签
     */
    private fun processLine(reader: BufferedReader, template: Template, line: String, blockStack: Stack<MutableList<String>>) {
        val splitter = StringSplitter(' ')
        var currentLines = blockStack.peek()
        with(controlPattern.matcher(line)) {
            if (find()) { // 查找到 !{} 标签，将其前面的部分加入到代码块中
                line.substring(0, start()).run { if (length > 0) currentLines.add(this) }
                line.substring(end()).run {
                    if (isNotEmpty()) {
                        currentLines.addUnfinishedFlag()
                    }
                    val expression = group().expression()
                    val command = splitter.apply { string = expression }.next().trim()
                    // 遇到特殊标签时，跳出语句块
                    when (command) {
                        "end", "elif", "else" -> {
                            blockStack.pop()
                            currentLines = blockStack.peek()
                        }
                    }
                    when (command) {
                        "end" -> if (isNotEmpty()) { processLine(reader, template, this, blockStack) } else Unit
                        else -> {
                            // 压入标签
                            val blockLabel = "!{${template.commandBlockSize++}:$expression}"
                            blockStack.apply { currentLines.add(blockLabel) }.push(ArrayList<String>().apply { template.blockMap[blockLabel] = this })
                            if (isEmpty()) {
                                process(reader, template, blockStack)
                            } else {
                                currentLines.addUnfinishedFlag()
                                processLine(reader, template, this, blockStack)
                            }
                        }
                    }
                }
            } else {
                currentLines.add(line)
            }
        }
    }

    private fun process(reader: BufferedReader, template: Template, blockStack: Stack<MutableList<String>>) {
        // 初始化所有的语句块
        do {
            val line = reader.readLine()
            if (line != null) {
                processLine(reader, template, line, blockStack)
            }
        } while (line != null)
    }

    private fun startProcess(reader: BufferedReader, templateName: String = "temp.template") = Template(templateName).apply {
        process(reader, this, Stack<MutableList<String>>().apply { push(baseCodeList) })
        compressLine()
    }

    fun parseFromPath(path: String) = parseFromFile(File(path))
    fun parseFromFile(file: File) = startProcess(file.bufferedReader(), file.absolutePath)
    fun parseFromString(string: String) = startProcess(string.reader().buffered())

}

