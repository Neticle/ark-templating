package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

/**
 * boolean NotEquals(Object arg0, Object arg1)
 *
 * Checks if the given arguments are not equal. In case both argument's
 * are Strings, the equals() method will be used.
 */
public class NotEqualsFunction extends DefaultFunctionHandler<Boolean>
{
    @Override
    public String getName ()
    {
        return "NotEquals";
    }

    @Override
    public Boolean apply (Object[] args) throws RenderingException
    {
        return !(new EqualsFunction().apply(args));
    }
}
