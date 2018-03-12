package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

/**
 * boolean Equals(Object arg0, Object arg1)
 *
 * Checks if the given arguments are equal. In case both argument's
 * are Strings, the equals() method will be used.
 */
public class EqualsFunction extends DefaultFunctionHandler<Boolean>
{
    @Override
    public String getName ()
    {
        return "Equals";
    }

    @Override
    public Boolean apply (Object[] args) throws RenderingException
    {
        if(args.length != 2)
        {
            return false;
        }

        if(matchSignatureArgs(args, true, String.class, String.class))
        {
            return args[0].equals(args[1]);
        }

        return args[0] == args[1];
    }
}
