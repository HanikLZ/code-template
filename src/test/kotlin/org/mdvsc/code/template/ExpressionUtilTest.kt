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
        assertEquals(ExpressionUtil.processExpression("\"com/alimusic/api/\" + info.title + \"/data/\" + simpleName(data) + \".java\""),
                "\"com/alimusic/api/\" info.title + \"/data/\" + data simpleName(1) + \".java\" +")
        assertEquals(ExpressionUtil.processExpression("\"2\" + 3 * 5 + fun(1 + 2, 3 + fun2(4, 2, show(5 * 3)))"),
                "\"2\" 3 5 * + 1 2 + 3 4 2 5 3 * show(1) fun2(3) + fun(2) +")
        assertEquals(ExpressionUtil.processExpression("p.type=\"string\""), "p.type \"string\" =")
        assertEquals(ExpressionUtil.processExpression("p.type=\"str\\\"ing\""), "p.type \"str\"ing\" =")
    }

}
