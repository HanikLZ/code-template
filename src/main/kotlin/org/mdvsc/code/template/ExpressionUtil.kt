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

    private fun computeFunction(expression: String, vars: Stack<String>, functionProcessor: FunctionProcessor) = expression.run {
        // 函数操作符形式为 函数名(参数数量)
        var firstBracketIndex = expression.indexOf('(')
        val funName = expression.substring(0, firstBracketIndex++)
        var paramCount = expression.run { substring(firstBracketIndex, lastIndexOf(')')).toInt() }
        val paramList = ArrayList<String>(paramCount)
        while (paramCount-- > 0) {
            paramList.add(vars.pop())
        }
        functionProcessor.processFunction(funName, paramList) ?: ""
    }

    fun getExpressionValue(expression: String, functionProcessor: FunctionProcessor, variableProcessor: VariableProcessor): String {
        val splitter = StringSplitter(' ').apply { string = processExpression(expression) }
        val varStack = Stack<String>()
        while (splitter.hasNext()) {
            varStack.push(splitter.next().run {
                if (isOperator()) {
                    computeExpression(this, varStack)
                } else if (isFunction()) {
                    computeFunction(this, varStack, functionProcessor)
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
        val functionParamBuilder = StringBuilder()

        var functionName = ""
        var functionParamCount = 0
        var functionBracketCount = 0
        var matchedPriority: Int = 0
        var stringStartChar: Char? = null

        fun pushOperatorPriority() {
            while (operatorStack.isNotEmpty() && operatorStack.peek().second >= matchedPriority) {
                outputBuilder.push(operatorStack.pop().first)
            }
            operatorStack.push(operatorBuilder.toString(), matchedPriority)
            operatorBuilder.clear()
            matchedPriority = 0
        }

        fun testOperator(it: Char) {
            val priority = operatorPriorityMap[operatorBuilder.append(it).toString()]
            if (priority == null) {
                if (matchedPriority != 0) {
                    operatorBuilder.setLength(operatorBuilder.length - 1)
                    pushOperatorPriority()
                } else if (operatorBuilder.length == 1) {
                    operatorBuilder.clear()
                    varBuilder.append(it)
                    if (it == '\'' || it == '\"') {
                        stringStartChar = it
                    }
                } else {
                    operatorBuilder.clear()
                    testOperator(it)
                }
            } else {
                when (it) {
                    '(' -> {
                        if (varBuilder.isNotBlank()) { // 函数开始
                            functionParamCount = 0
                            functionBracketCount = 1
                            functionName = varBuilder.toString().trim()
                            varBuilder.clear()
                        } else {
                            operatorStack.push(it.toString(), priority)
                        }
                    }
                    ')' -> {
                        while (operatorStack.isNotEmpty()) {
                            val o = operatorStack.pop()
                            if (o.second == matchedPriority) {
                                break
                            }
                            outputBuilder.push(o.first)
                        }
                    }
                }
                if (varBuilder.isNotEmpty()) {
                    outputBuilder.push(varBuilder.toString())
                    varBuilder.clear()
                }
                matchedPriority = priority
            }
        }

        expression.forEach {
            if (stringStartChar != null) {
                varBuilder.append(it)
                if (stringStartChar == it) {
                    stringStartChar = null
                }
            } else if (functionBracketCount > 0) {
                if (it == ')' && --functionBracketCount == 0 || (it == ',' && functionBracketCount == 1)) {
                    if (functionParamBuilder.isNotBlank()) {
                        functionParamCount++
                        outputBuilder.push(processExpression(functionParamBuilder.toString().trim()))
                    }
                    if (functionBracketCount < 1) { // 函数结束
                        outputBuilder.push("$functionName($functionParamCount)")
                    }
                    functionParamBuilder.clear()
                } else {
                    if (it == '(') {
                        functionBracketCount++
                    }
                    functionParamBuilder.append(it)
                }
            } else if (it != ' ') {
                testOperator(it)
            } else if (operatorBuilder.isNotEmpty()) {
                pushOperatorPriority()
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

