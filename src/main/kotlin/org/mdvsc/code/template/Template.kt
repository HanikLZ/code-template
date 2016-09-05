package org.mdvsc.code.template

import java.io.File
import java.util.*

/**
 * @author HanikLZ
 * @since 2016/8/30
 */
class Template internal constructor(val templateName : String) {

    companion object {
        private const val emptyString = ""
        private val defaultFunctionProcessor = InternalFunctionProcessor()
        private val defaultVariableProcessor = InternalVariableProcessor()
    }

    var lineWrap = "\n"
    var functionProcessor: FunctionProcessor? = null
    var variableProcessor: VariableProcessor? = null

    private val functionProcessorWrapper = object : FunctionProcessor {
        override fun processFunction(function: String, args: List<String>) = functionProcessor?.processFunction(function, args)?: defaultFunctionProcessor.processFunction(function, args)
    }

    private val variableProcessorWrapper = object : VariableProcessor {
        override fun processKeyCollection(variable: String) = variableProcessor?.processKeyCollection(variable) ?: defaultVariableProcessor.processKeyCollection(variable)
        override fun processVariable(variable: String) = variableProcessor?.processVariable(variable) ?: defaultVariableProcessor.processVariable(variable)
    }

    internal val baseCodeList = ArrayList<String>()

    /**
     * 模板代码块表
     * 根据 !{x:if} / !{x:repeat} / !{x:elif} / !{x:else} 语句块作为 key，对应其具体的语句块
     * 按文件代码块入口为 !{x:file}
     * 未被块语句包括的部分，key 为 ""
     */
    internal val blockMap = HashMap<String, List<String>>()
    var commandBlockSize = 0
        internal set

    private fun genContent(codeList: List<String>, varMap: MutableMap<String, String>, targetFile: File): String {
        val splitter = StringSplitter(' ')
        val builder = StringBuilder()
        val contentBuilder = StringBuilder()
        var shouldProcessIfBlock = true
        codeList.forEach {
            val line = builder.apply {
                var lastEnd = 0
                setLength(0)
                expressionPattern.matcher(it).run {
                    while (find()) {
                        append(it.substring(lastEnd, start()))
                        append(processExpression(group().expression(), varMap))
                        lastEnd = end()
                    }
                }
                append(it.substring(lastEnd))
            }.toString()

            var lineWithoutContent = false
            contentBuilder.append(builder.apply {
                setLength(0)
                var lastEnd = 0
                controlPattern.matcher(line).run {
                    while (find()) {
                        append(line.substring(lastEnd, start()))
                        lastEnd = end()
                        val group = group()
                        lineWithoutContent = group.length == line.length
                        val command = splitter.apply { string = group.expression() }.next().run { substring(indexOf(':') + 1) }
                        val childContent = when (command) {
                            "file" -> processFileBlock(group, splitter.resetString().trim(), varMap, targetFile)
                            "repeat" -> processRepeatBlock(group, splitter.toList(), varMap, targetFile)
                            "if" -> {
                                shouldProcessIfBlock = processBooleanExpression(splitter.resetString(), varMap)
                                if (shouldProcessIfBlock) genContent(blockMap[group] ?: emptyList(), varMap, targetFile) else emptyString
                            }
                            "elif" -> {
                                shouldProcessIfBlock = !shouldProcessIfBlock && processBooleanExpression(splitter.resetString(), varMap)
                                if (shouldProcessIfBlock) genContent(blockMap[group] ?: emptyList(), varMap, targetFile) else emptyString
                            }
                            "else" -> {
                                if (!shouldProcessIfBlock) genContent(blockMap[group] ?: emptyList(), varMap, targetFile) else emptyString
                            }
                            else -> emptyString
                        }
                        // 去除子代码块的换行
                        append(if (lastEnd != line.length && childContent.endsWith(lineWrap)) {
                            childContent.substring(0, childContent.length - lineWrap.length)
                        } else {
                            childContent
                        })
                    }
                }
                append(line.substring(lastEnd))
            }.toString())
            if (!lineWithoutContent) {
                contentBuilder.append(lineWrap)
            }
        }
        return contentBuilder.toString()
    }

    /**
     * 处理boolean表达式
     * 条件表达式的输出为boolean类型，要求每个条件均为boolean类型输出
     * 逻辑运算优先级别为 !, &&、||
     */
    private fun processBooleanExpression(expression: String, varMap: Map<String, String>): Boolean {
        return try {processExpression(expression, varMap).toBoolean()} catch (e: Exception) {false}
    }

    private fun processExpression(expression: String, varMap: Map<String, String>)
            = ExpressionUtil.getExpressionValue(expression, functionProcessorWrapper, mappedVariableProcessor(varMap))

    private fun mappedVariable(variable: String, varMap: Map<String, String>) = variable.lastIndexOf('.').run {
        if (this > 0) {
            val prefix = variable.substring(0, this)
            (varMap[prefix] ?: prefix) + variable.substring(this)
        } else {
            varMap[variable] ?: variable
        }
    }

    private fun mappedVariableProcessor(varMap: Map<String, String>) = if (varMap.isNotEmpty()) {
        object : VariableProcessor {

            override fun processVariable(variable: String): String {
                return variableProcessorWrapper.processVariable(mappedVariable(variable, varMap))
            }

            override fun processKeyCollection(variable: String): Collection<String> {
                return variableProcessorWrapper.processKeyCollection(mappedVariable(variable, varMap))
            }
        }
    } else {
        variableProcessorWrapper
    }

    private fun processFileBlock(blockLabel: String, pathExpression: String, varMap: MutableMap<String, String>, targetFile: File): String {
        val codeBlock = blockMap[blockLabel]
        return if (codeBlock != null) {
            val path = processExpression(pathExpression, varMap)
            val file = if (!path.startsWith('/')) {
                File(if (targetFile.isDirectory) targetFile else targetFile.parentFile, path)
            } else {
                File(path)
            }
            val content = genContent(codeBlock, varMap, file)
            if (file.absolutePath == targetFile.absolutePath) {
                content
            } else if (content.isNotEmpty()) {
                file.writeText(content)
                emptyString
            } else {
                emptyString
            }
        } else {
            emptyString
        }
    }

    /**
     * 处理循环语句块
     */
    private fun processRepeatBlock(blockLabel: String, args: List<String>, varMap: MutableMap<String, String>, targetFile: File): String {
        val contentBuilder = StringBuilder()
        val repeatBlock = blockMap[blockLabel]
        if (repeatBlock != null) {
            var collectionStr = ""
            var variableStr = ""
            args.forEach {
                it.pair().run {
                    when (first) {
                        "collection" -> collectionStr = second.stringValue()
                        "var" -> variableStr = second.stringValue()
                    }
                }
            }
            val collection = mappedVariableProcessor(varMap).processKeyCollection(collectionStr) ?: emptyList()
            for (v in collection) {
                val oldV = varMap[variableStr]
                varMap[variableStr] = "${mappedVariable(collectionStr, varMap)}.$v"
                contentBuilder.append(genContent(repeatBlock, varMap, targetFile))
                if (oldV != null) {
                    varMap[variableStr] = oldV
                }
            }
        }
        return contentBuilder.toString()
    }

    @JvmOverloads fun writeToPath(path: File = File(templateName)) {
        val content = genContent(baseCodeList, HashMap<String, String>(), path)
        if (content.isNotBlank()) {
            (if (path.isDirectory) File(path, templateName) else path).writeText(content)
        }
    }
}

