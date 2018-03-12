package pt.neticle.ark.templating.structure.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Container object for all known functions that are callable from a FunctionCallExpression
 */
public class FunctionCatalog
{
    private final Map<String, FunctionHandler> handlers;

    public FunctionCatalog ()
    {
        this.handlers = new HashMap<>();

        registerHandler(new ImplodeFunction());
        registerHandler(new ExplodeFunction());
        registerHandler(new NotEmptyFunction());
        registerHandler(new EmptyFunction());
        registerHandler(new EqualsFunction());
        registerHandler(new NotEqualsFunction());
    }

    public void registerHandler (String name, FunctionHandler handler)
    {
        handlers.put(name, handler);
    }

    public void registerHandler (DefaultFunctionHandler handler)
    {
        registerHandler(handler.getName(), handler);
    }

    public FunctionHandler getHandler (String fn)
    {
        return handlers.get(fn);
    }

    public Optional<FunctionHandler> handler (String fn)
    {
        return Optional.ofNullable(handlers.get(fn));
    }
}
