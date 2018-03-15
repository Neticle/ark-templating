package pt.neticle.ark.templating.processing;

import pt.neticle.ark.templating.structure.expressions.OutputExpression;

/**
 * Instructs the renderer to output the result of an expression.
 */
public class ExpressionResultOutputInstruction extends Instruction
{
    private final OutputExpression expression;

    ExpressionResultOutputInstruction (OutputExpression expression)
    {
        super(Type.EXPRESSION_RESULT_OUTPUT);
        this.expression = expression;
    }

    public OutputExpression getExpression ()
    {
        return expression;
    }

    @Override
    public String toString ()
    {
        return super.toString() + "evaluate:'" + expression.getClass().getSimpleName() + "']";
    }
}
