package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.renderer.Scope;

public interface Expression
{
    Object resolve (Scope context);
}
