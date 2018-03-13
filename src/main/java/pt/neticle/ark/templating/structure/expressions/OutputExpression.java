package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.renderer.Scope;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches an expression that starts with '=' or '~' and has optionally a default value, such
 * as: = [main-expression] || [default-expression]
 *
 * The '=' operator indicates that the resulting output should be escaped - so lesser/greater than
 * characters should be replaced with html entities.
 *
 * The '~' operator indicates that no escaping should be done on the resulting output.
 */
public class OutputExpression implements Expression
{
    private static final Pattern operatorPt = Pattern.compile("^\\s*(=|~)\\s*[^\\s]+");

    public enum Operator
    {
        RAW,
        ESCAPED
    }

    private final Operator operator;
    private final Expression getterExpression;
    private final Expression defaultExpression;

    OutputExpression (ExpressionMatcher matcher, String text) throws ParseException
    {
        Matcher m = operatorPt.matcher(text);

        if(!m.find())
        {
            throw new ParseException("Invalid format or unknown operator for output expression", 0);
        }

        switch (m.group(1).trim())
        {
            case "~":
                operator = Operator.RAW;
                break;
            case "=":
            default:
                operator = Operator.ESCAPED;
        }

        String expBody = text.substring(m.end(1)).trim();
        String[] subExpressions = expBody.split("\\|{2}");

        if(subExpressions.length > 2)
        {
            throw new ParseException("Output expression may only contain one default value specifier", 0);
        }

        getterExpression = matcher.match(subExpressions[0].trim());
        defaultExpression = subExpressions.length > 1 ?
            matcher.match(subExpressions[1].trim()) : null;
    }

    @Override
    public Object resolve (Scope scope)
    {
        Object result = null;
        if(getterExpression != null)
        {
            result = getterExpression.resolve(scope);
        }

        if(result == null && defaultExpression != null)
        {
            result = defaultExpression.resolve(scope);
        }

        return result;
    }

    static boolean matches (String str)
    {
        return operatorPt.matcher(str).find();
    }
}
