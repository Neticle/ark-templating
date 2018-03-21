package test.ark.templating;

import org.junit.Assert;
import org.junit.Test;
import pt.neticle.ark.templating.renderer.MainScope;
import pt.neticle.ark.templating.structure.expressions.*;
import pt.neticle.ark.templating.structure.functions.FunctionCatalog;

import java.text.ParseException;

public class ExpressionsTest
{
    private final ExpressionMatcher matcher;

    public ExpressionsTest ()
    {
        matcher = new ExpressionMatcher(new FunctionCatalog());
    }

    @Test
    public void outputExp () throws ParseException
    {
        Assert.assertTrue(matcher.match("=reference") instanceof OutputExpression);
        Assert.assertTrue(matcher.match("= reference.member") instanceof OutputExpression);
        Assert.assertTrue(matcher.match("= reference || 'default'") instanceof OutputExpression);
        Assert.assertTrue(matcher.match("= reference || other.reference") instanceof OutputExpression);
        Assert.assertTrue(matcher.match("=reference||other") instanceof OutputExpression);
    }

    @Test
    public void referenceExp () throws ParseException
    {
        Expression exp = matcher.match("foo.bar");

        Assert.assertTrue(exp instanceof ObjectReferenceExpression);

        ObjectReferenceExpression ref = (ObjectReferenceExpression) exp;

        Assert.assertArrayEquals(new String[] { "foo", "bar" }, ref.getSegments());

        Object result = ref.resolve
        (
            MainScope.builder()
                .withMap("foo", (foo) -> foo.put("bar", "something"))
                .build()
        );

        Assert.assertTrue(result instanceof String);
        Assert.assertTrue(((String)result).equals("something"));
    }

    @Test
    public void stringLiteralExp () throws ParseException
    {
        testStringLiteral("this is a string literal", "'this is a string literal'");
        testStringLiteral("this is 'escaped'", "'this is \\'escaped\\''");
    }

    private void testStringLiteral (String expected, String expr) throws ParseException
    {
        Expression exp = matcher.match(expr);

        Assert.assertTrue(exp instanceof StringLiteralExpression);
        Assert.assertEquals(expected, ((StringLiteralExpression) exp).getContent());
    }

    @Test
    public void functionCallExp () throws ParseException
    {
        FunctionCallExpression fn;

        fn = testFunctionCall("foo(bar)");
        fn = testFunctionCall("foo(bar,'text', ref)");
        fn = testFunctionCall("a_function ()");

        fn = testFunctionCall("foo(obj.ref, 'test')");
        Assert.assertEquals("foo", fn.getFunctionName());
        Assert.assertEquals(2, fn.getArgumentExpressions().length);
        Assert.assertTrue(fn.getArgumentExpressions()[0] instanceof ObjectReferenceExpression);
        Assert.assertTrue(fn.getArgumentExpressions()[1] instanceof StringLiteralExpression);

        fn = testFunctionCall("foo('bar', nestedFn('test', 'test2'))");
        Assert.assertEquals(2, fn.getArgumentExpressions().length);
        Assert.assertTrue(fn.getArgumentExpressions()[0] instanceof StringLiteralExpression);
        Assert.assertTrue(fn.getArgumentExpressions()[1] instanceof FunctionCallExpression);

        fn = (FunctionCallExpression) fn.getArgumentExpressions()[1];
        Assert.assertEquals(2, fn.getArgumentExpressions().length);
        Assert.assertTrue(fn.getArgumentExpressions()[0] instanceof StringLiteralExpression);
        Assert.assertTrue(fn.getArgumentExpressions()[1] instanceof StringLiteralExpression);
    }

    private FunctionCallExpression testFunctionCall (String expr) throws ParseException
    {
        Expression exp = matcher.match(expr);

        Assert.assertTrue(exp instanceof FunctionCallExpression);

        return (FunctionCallExpression) exp;
    }
}
