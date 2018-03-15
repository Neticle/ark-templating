package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.Node;
import pt.neticle.ark.templating.structure.ReadableElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalScope implements Scope
{
    private final Scope parent;
    private final Map<String, Object> data;
    private final Map<String, List<ReadableElement>> slotElements;
    private final List<Node> unassignedSlotElements;

    public InternalScope (Scope parent)
    {
        this(parent, new HashMap<>());
    }

    protected InternalScope (Scope parent, Map<String, Object> data)
    {
        this.parent = parent;
        this.data = data;
        this.slotElements = new HashMap<>();
        this.unassignedSlotElements = new ArrayList<>();
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
}
