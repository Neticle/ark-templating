package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

/**
 * String[] Explode(String delimiter, String text)
 *
 * Splits the given text around matches of the specified delimiter
 */
public class ExplodeFunction extends DefaultFunctionHandler<String[]>
{
    @Override
    public String getName ()
    {
        return "Explode";
    }

    @Override
    public String[] apply (Object[] args) throws RenderingException
    {
        ensureSignatureArgs(args, false, String.class, String.class);

        return ((String)args[1]).split((String)args[0]);
    }
}
