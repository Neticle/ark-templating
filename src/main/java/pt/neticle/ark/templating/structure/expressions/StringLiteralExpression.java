package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.renderer.Scope;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches a string literal expression such as '<content>'
 * All string literals must be surrounded by single-quotes. Any single-quotes that are part of
 * the string content must be escaped like this: \'
 */
public class StringLiteralExpression implements Expression
{
    private static final Pattern pt = Pattern.compile("^'(\\\\.|[^'])*'$"/*"^'([^'\\n\\r]*)?'$"*/);

    private final String content;

    StringLiteralExpression (ExpressionMatcher matcher, String text)
    {
        Matcher m = pt.matcher(text);

        String str = null;

        if(m.find())
        {
            str = m.group(0);
            str = str.substring(1, str.length()-1);
            str = str.replace("\\'", "'");
        }

        content = str;
    }

    @Override
    public Object resolve (Scope scope)
    {
        return content;
    }

    static boolean matches (String text)
    {
        return pt.matcher(text).find();
    }

    public String getContent ()
    {
        return content;
    }
}
