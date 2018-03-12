package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

import java.util.Collection;

/**
 * boolean NotEmpty(Collection||Object[]||String arg0)
 *
 * Checks if the given Collection, array or String is not empty.
 */
public class NotEmptyFunction extends DefaultFunctionHandler<Boolean>
{

    @Override
    public String getName ()
    {
        return "NotEmpty";
    }

    @Override
    public Boolean apply (Object[] args) throws RenderingException
    {
        if(args[0] == null)
        {
            return false;
        }

        if(args[0] instanceof Collection)
        {
            return ((Collection)args[0]).size() > 0;
        }

        if(args[0].getClass().isArray())
        {
            return ((Object[])args[0]).length > 0;
        }

        if(args[0] instanceof String)
        {
            return !((String)args[0]).isEmpty();
        }

        return false;
    }
}
