package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

@FunctionalInterface
public interface FunctionHandler<T>
{
    T apply(Object[] args) throws RenderingException;
}
