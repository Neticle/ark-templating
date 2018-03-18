package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.renderer.Scope;

import java.util.function.Function;

public interface Expression
{
    Function<Scope, Object> getResolver();
}
