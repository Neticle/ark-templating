package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

public class IfFunction extends DefaultFunctionHandler<Object>
{
    @Override
    public String getName ()
    {
        return "If";
    }

    @Override
    public Object apply (Object[] args) throws RenderingException
    {
        return nonNullArgument(args, 0, Boolean.class) ?
            (args.length > 1 ? args[1] : null) :
            (args.length > 2 ? args[2] : null);
    }
}
