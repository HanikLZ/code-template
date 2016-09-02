package org.mdvsc.code.template

import java.util.*
import java.util.regex.Pattern

/**
 * @author HanikLZ
 * @since 2016/8/31
 */
internal object ExpressionUtil {

    private val operatorPriorityMap = mapOf(
            Pair("+", 5)
            , Pair("-", 5)
            , Pair("*", 6)
            , Pair("/", 6)
            , Pair("^", 8)
            , Pair(">", 4)
            , Pair("<", 4)
            , Pair(">=", 4)
            , Pair("<=", 4)
            , Pair("<>", 3)
            , Pair("==", 3)
            , Pair("=", 3)
            , Pair("!=", 3)
            , Pair("&&", 2)
            , Pair("||", 2)
            , Pair("(", -1)
            , Pair(")", -1))
    private val functionPattern = Pattern.compile("[^+\\-*/|&=<>!()\\s]+\\s*\\(.*\\)")!!

    private fun StringBuilder.push(arg: String) = append(arg).append(' ')
    private fun StringBuilder.clear() = setLength(0)
    private fun Stack<Pair<String, Int>>.push(key: String, value: Int) = push(Pair(key, value))
    private fun Stack<String>.twoVarCompute(computer: (String, String) -> Any): String {
        val v2 = pop()
        val v1 = pop()
        return computer(v1, v2).toString()
    }

    private fun String.isOperator() = operatorPriorityMap[this] ?: 0 != 0
    private fun String.isFunction() = functionPattern.matcher(this).matches()

    private fun computeExpression(operator: String, vars: Stack<String>) = when (operator) {
        "+" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toDouble() + v2.toDouble()
            } catch (e: Exception) {
                v1 + v2
            }
        }
        "-" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toDouble() - v2.toDouble()
            } catch (e: Exception) {
                ""
            }
        }
        "*" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toDouble() * v2.toDouble()
            } catch (e: Exception) {
                ""
            }
        }
        "/" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toDouble() / v2.toDouble()
            } catch (e: Exception) {
                ""
            }
        }
        "^" -> vars.twoVarCompute() { v1, v2 ->
            try {
                Math.pow(v1.toDouble(), v2.toDouble())
            } catch (e: Exception) {
                ""
            }
        }
        ">" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) > 0 }
        "<" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) < 0 }
        ">=" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) >= 0 }
        "<=" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) <= 0 }
        "<>", "!=" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) != 0 }
        "==", "=" -> vars.twoVarCompute() { v1, v2 -> v1.compareTo(v2) == 0 }
        "&&" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toBoolean() && v2.toBoolean()
            } catch (e: Exception) {
                false
            }
        }
        "||" -> vars.twoVarCompute() { v1, v2 ->
            try {
                v1.toBoolean() || v2.toBoolean()
            } catch (e: Exception) {
                false
            }
        }
        else -> ""
    }

    fun computeFunction(expression: String, functionProcessor: FunctionProcessor, variableProcessor: VariableProcessor) = expression.run {
        val funSplitter = StringSplitter(',')
        val argList = ArrayList<String>()
        val index = indexOf('(')
        val funName = substring(0, index).trim()
        funSplitter.string = substring(index + 1, lastIndexOf(')'))
        while (funSplitter.hasNext()) {
            argList.add(getExpressionValue(funSplitter.next().trim(), functionProcessor, variableProcessor))
        }
        functionProcessor.processFunction(funName, argList) ?: ""
    }

    fun getExpressionValue(expression: String, functionProcessor: FunctionProcessor, variableProcessor: VariableProcessor): String {
        val splitter = StringSplitter(' ').apply { string = processExpression(expression) }
        val varStack = Stack<String>()
        while (splitter.hasNext()) {
            varStack.push(splitter.next().run {
                if (isOperator()) {
                    computeExpression(this, varStack)
                } else if (isFunction()) {
                    computeFunction(this, functionProcessor, variableProcessor)
                } else if (isStringValue()) {
                    stringValue()
                } else {
                    variableProcessor.processVariable(this) ?: this
                }
            })
        }
        return if (varStack.isEmpty()) "" else varStack.peek().toString()
    }

    fun processExpression(expression: String): String {

        val operatorStack = Stack<Pair<String, Int>>()

        val outputBuilder = StringBuilder()

        val varBuilder = StringBuilder()
        val operatorBuilder = StringBuilder()
        val functionBuilder = StringBuilder()
        val functionSplitter = StringSplitter(',')

        var matchedPriority = 0
        var functionStart = false
        var functionInnerBracketStart = false

        fun test(index: Int?, ch: Char) {
            if (index == null) {
                val builder = if (matchedPriority != 0) {
                    val operator = operatorBuilder.substring(0, operatorBuilder.length - 1)
                    while (operatorStack.isNotEmpty() && operatorStack.peek().second >= matchedPriority) {
                        outputBuilder.push(operatorStack.pop().first)
                    }
                    operatorStack.push(operator, matchedPriority)
                    matchedPriority = 0
                    operatorBuilder
                } else if (operatorBuilder.length == 1) {
                    varBuilder
                } else {
                    operatorBuilder
                }
                operatorBuilder.clear()
                builder.append(ch)
                if (builder == operatorBuilder) {
                    test(operatorPriorityMap[operatorBuilder.toString()], ch)
                }
            } else if (varBuilder.isNotEmpty() && ch == '(') { // 函数开始
                functionStart = true
                varBuilder.append(ch)
                operatorBuilder.clear()
            } else {
                matchedPriority = index
                if (varBuilder.isNotEmpty()) {
                    outputBuilder.push(varBuilder.toString())
                    varBuilder.clear()
                }
                if (when (ch) {
                    '(' -> {
                        operatorStack.push(operatorBuilder.toString(), index)
                        true
                    }
                    ')' -> {
                        while (operatorStack.isNotEmpty()) {
                            val o = operatorStack.pop()
                            if (o.second == matchedPriority) {
                                break
                            }
                            outputBuilder.push(o.first)
                        }
                        true
                    }
                    else -> false
                }) {
                    operatorBuilder.clear()
                    matchedPriority = 0
                }
            }
        }

        expression.filter { it != ' ' }.forEach {
            if (functionStart) {
                if (it == ')' && !functionInnerBracketStart) {
                    functionSplitter.string = functionBuilder.toString().trim()
                    while (functionSplitter.hasNext()) {
                        varBuilder.append(processExpression(functionSplitter.next().trim())).append(',')
                    }
                    if (varBuilder.endsWith(',')) {
                        varBuilder.setCharAt(varBuilder.length - 1, it)
                    } else {
                        varBuilder.append(it)
                    }
                    outputBuilder.push(varBuilder.toString())
                    varBuilder.setLength(0)
                    functionBuilder.setLength(0)
                    functionStart = false
                } else {
                    functionInnerBracketStart = when (it) {
                        '(' -> true
                        ')' -> false
                        else -> functionInnerBracketStart
                    }
                    functionBuilder.append(it)
                }
            } else {
                test(operatorPriorityMap[operatorBuilder.append(it).toString()], it)
            }
        }
        if (varBuilder.isNotEmpty()) {
            outputBuilder.push(varBuilder.toString())
        }
        while (operatorStack.isNotEmpty()) {
            outputBuilder.push(operatorStack.pop().first)
        }
        return outputBuilder.toString().trim()
    }
}

