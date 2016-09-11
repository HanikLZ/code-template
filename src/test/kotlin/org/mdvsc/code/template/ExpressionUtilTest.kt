package org.mdvsc.code.template

import org.testng.annotations.Test

import org.testng.Assert.*

/**
 * @author haniklz
 * *
 * @since 16-9-11
 */
class ExpressionUtilTest {

    @Test
    fun testComputeFunction() {

    }

    @Test
    fun testGetExpressionValue() {

    }

    @Test
    fun testProcessExpression() {
        println(ExpressionUtil.processExpression("\"2\" + 3 * 5 + fun(1 + 2, 3 + fun2(4, 2, show(5 * 3)))"))
    }

}
