package com.conditionevaluator;

import org.junit.Test;
import org.junit.Assert;

public class ConditionEvaluatorTests {

    private static void Check(boolean expected, String conditionExpression){
        Assert.assertEquals(expected, ConditionEvaluator.evaluateCondition(conditionExpression));
    }

    private static void checkThrows(String conditionExpression){
        try {
            ConditionEvaluator.evaluateCondition(conditionExpression);
        }catch (IllegalArgumentException ex){
            return;
        }
        Assert.fail("Expected format exception from expression " + conditionExpression);
    }

    @Test
    public void testEquals(){
        Check(true, "'A' == 'A'");
        Check(true, "B == b");
        Check(false, "A == b");
        checkThrows("'A' = 'A'");
    }

    @Test
    public void testNotEqual(){
        Check(false, "A != A");
        Check(false, "B != b");
        Check(true, "A != b");
    }

    @Test
    public void testParenthesis(){
        Check(true, "(A == A)");
        Check(false, "((((((A!=A))))))");
        checkThrows("((A))");
    }

    public void testNegation(){
        Check(false, "!(A==A))");
        Check(true, "!!!(A!=A))");
    }

    @Test
    public void testAnd(){
        Check(true, "A==A and B==b");
        //Check(false, "A==A and B==''");
        Check(false, "A==B and C==D");
    }

    @Test
    public void testOr(){
        Check(true, "A==A or B==b");
        Check(true, "A==A or B==''");
        Check(false, "A==B or C==D");
    }

    @Test
    public void testPrecedence(){
        Check(true, "A==B and C==C or D==D");
        Check(false, "A==B and (C==C or D==D)");
    }

    @Test
    public void testExtraJunk(){
        checkThrows("A == A == A");
    }
}
