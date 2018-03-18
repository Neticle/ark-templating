package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.expressions.Expression;

import java.util.HashMap;
import java.util.Map;

public class InternalScope implements Scope
{
    private final Scope parent;
    private final Map<String, Object> data;
    private final Map<Expression, Object> evaluatedExpressions;

    public InternalScope (Scope parent)
    {
        this(parent, new HashMap<>());
    }

    protected InternalScope (Scope parent, Map<String, Object> data)
    {
        this.parent = parent;
        this.data = data;
        this.evaluatedExpressions = new HashMap<>();
    }

    public void put (String key, Object value)
    {
        this.data.put(key, value);
    }

    public void putAll (Map<String, Object> data)
    {
        this.data.putAll(data);
    }

    @Override
    public Scope getParent ()
    {
        return parent;
    }

    @Override
    public boolean available (String key)
    {
        return data.containsKey(key) || (parent != null && parent.available(key));
    }

    @Override
    public Object get (String key)
    {
        return data.getOrDefault(key, parent != null ? parent.get(key) : null);
    }

    @Override
    public Object evaluate (Expression expr)
    {
        if(evaluatedExpressions.containsKey(expr))
        {
            return evaluatedExpressions.get(expr);
        }

        final Object r = expr.getResolver().apply(this);
        evaluatedExpressions.put(expr, r);

        return r;
    }

    @Override
    public void reset ()
    {
        data.clear();
        evaluatedExpressions.clear();
    }
}
