package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.expressions.Expression;

public interface Scope
{
    Scope getParent ();
    boolean available (String key);
    Object get (String key);
    Object evaluate (Expression expr);
    void reset ();
}
