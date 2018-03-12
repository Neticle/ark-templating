package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.exception.RenderingException;
import pt.neticle.ark.templating.renderer.Scope;
import pt.neticle.ark.templating.structure.functions.FunctionHandler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A function call expression matches a signature such as <function-name>([<argument-expression>,...])
 */
public class FunctionCallExpression implements Expression
{
    private static final Pattern signaturePt = Pattern.compile("^(\\w+)\\s*\\((.*)\\)$");

    private final String functionName;
    private final Expression[] argumentExpressions;
    private final ExpressionMatcher origin;

    FunctionCallExpression (ExpressionMatcher matcher, String text) throws ParseException
    {
        origin = matcher;

        Matcher m = signaturePt.matcher(text);

        if(!m.find())
        {
            throw new ParseException("Unable to parse function expression", 0);
        }

        functionName = m.group(1);
        String args = m.group(2).trim();

        ArrayList<Expression> argList = new ArrayList<>();
        if(args.length() > 0)
        {
            int offset = 0, nesting = 0;
            boolean inQuotes = false;
            for(int i = 0; i <= args.length(); i++)
            {
                char c = i < args.length() ? args.charAt(i) : 0;
                if(nesting == 0 && (c == ',' && !inQuotes) || c == 0)
                {
                    Expression expression;

                    try {
                        expression = matcher.match(args.substring(offset, i).trim());
                    } catch(RenderingException e)
                    {
                        throw new ParseException("Could not parse expression of argument " + argList.size(), 0);
                    }

                    offset = i + 1;
                    argList.add(expression);
                    continue;
                }

                if(c == '\'')
                {
                    inQuotes = !inQuotes;
                }

                if(!inQuotes && c == '(')
                {
                    nesting++;
                }

                if(!inQuotes && c == ')')
                {
                    nesting--;
                }
            }
        }

        argumentExpressions = argList.stream().toArray(Expression[]::new);
    }

    @Override
    public Object resolve (Scope scope)
    {
        FunctionHandler handler = origin.getFunctionCatalog().getHandler(functionName);
        Object result;

        if(handler != null)
        {
            result = handler.apply
            (
                Arrays.stream(argumentExpressions)
                    .map((x) -> x.resolve(scope))
                    .toArray(Object[]::new)
            );
        }
        else
        {
            throw new RenderingException("Unknown function: " + functionName);
        }

        return result;
    }

    public String getFunctionName ()
    {
        return functionName;
    }

    public Expression[] getArgumentExpressions ()
    {
        return argumentExpressions;
    }

    static boolean matches (String text)
    {
        return signaturePt.matcher(text).find();
    }
}
