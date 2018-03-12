package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.Node;
import pt.neticle.ark.templating.structure.ReadableElement;

import java.util.*;

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

    public void addSlottedElement (String slot, ReadableElement element)
    {
        if(element == null)
        {
            return;
        }

        slotElements.computeIfAbsent(slot, (k) -> new ArrayList<>())
            .add(element);
    }

    public void addSlottedElements (String slot, List<ReadableElement> elements)
    {
        slotElements.computeIfAbsent(slot, (k) -> new ArrayList<>())
            .addAll(elements);
    }

    public void addSlottedNode (Node node)
    {
        if(node == null)
        {
            return;
        }

        unassignedSlotElements.add(node);
    }

    public void addSlottedNodes (List<Node> nodes)
    {
        unassignedSlotElements.addAll(nodes);
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
    public List<ReadableElement> getElementsForSlot (String slot)
    {
        return slotElements.getOrDefault(slot, new ArrayList<>(0));
    }

    @Override
    public List<Node> getUnassignedSlotNodes ()
    {
        return unassignedSlotElements;
    }
}
