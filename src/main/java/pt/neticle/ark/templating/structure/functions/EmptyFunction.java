package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

/**
 * boolean Empty(Collection||Object[]||String arg0)
 *
 * Checks if the given Collection, array or String is empty.
 */
public class EmptyFunction extends DefaultFunctionHandler<Boolean>
{
    @Override
    public String getName ()
    {
        return "Empty";
    }

    @Override
    public Boolean apply (Object[] args) throws RenderingException
    {
        return !(new NotEmptyFunction().apply(args));
    }
}
