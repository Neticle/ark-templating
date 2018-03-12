package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.structure.functions.FunctionCatalog;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ExpressionMatcher
{
    private final Map<Predicate<String>, ExpressionProvider> handlers;
    private final FunctionCatalog functionCatalog;

    public ExpressionMatcher (FunctionCatalog functionCatalog)
    {
        this.functionCatalog = functionCatalog;

        handlers = new HashMap<>();

        handlers.put(OutputExpression::matches, OutputExpression::new);
        handlers.put(ObjectReferenceExpression::matches, ObjectReferenceExpression::new);
        handlers.put(StringLiteralExpression::matches, StringLiteralExpression::new);
        handlers.put(FunctionCallExpression::matches, FunctionCallExpression::new);
    }

    public FunctionCatalog getFunctionCatalog ()
    {
        return functionCatalog;
    }

    /**
     * Matches a given text string for a suitable expression.
     *
     * @param asText
     * @return
     * @throws ParseException
     */
    public Expression match (String asText) throws ParseException
    {
        ExpressionProvider provider = handlers.entrySet().stream()
            .filter((e) -> e.getKey().test(asText))
            .map((e) -> e.getValue())
            .findFirst().orElse(null);

        if(provider != null)
        {
            return provider.apply(this, asText);
        }

        throw new ParseException("No expression to match '" + asText + "'", 0);
    }

    @FunctionalInterface
    public interface ExpressionProvider
    {
        Expression apply (ExpressionMatcher origin, String param) throws ParseException;
    }
}
