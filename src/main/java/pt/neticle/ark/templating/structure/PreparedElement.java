package pt.neticle.ark.templating.structure;

import pt.neticle.ark.templating.renderer.MainScope;
import pt.neticle.ark.templating.renderer.Scope;
import pt.neticle.ark.templating.structure.Node;
import pt.neticle.ark.templating.structure.ReadableAttribute;
import pt.neticle.ark.templating.structure.ReadableElement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class PreparedElement implements ReadableElement
{
    private final MainScope scope;

    public PreparedElement ()
    {
        scope = new MainScope();
    }

    public final void accept (Scope other)
    {
        for(AttributeHandler ah : attributeHandlers())
        {
            scope.put(ah.name, ah.filterFn.apply(other.get(ah.name)));
        }

        for(SlotHandler sh : slotHandlers())
        {
            scope.addSlottedElements
            (
                sh.name,

                other.getElementsForSlot(sh.name)
                .stream()
                .map((el) -> sh.filterFn.apply(el))
                .filter((el) -> el != null)
                .collect(Collectors.toList())
            );
        }

        if(unassignedSlotHandler() != null)
        {
            SlotHandler us = unassignedSlotHandler();

            scope.addSlottedNodes
            (
                other.getUnassignedSlotNodes()
                    .stream()
                    .map((n) -> us.filterFn.apply(n))
                    .filter((n) -> n != null)
                    .collect(Collectors.toList())
            );
        }
    }

    public final Scope getPreparedScope ()
    {
        return scope;
    }

    protected final MainScope scope ()
    {
        return scope;
    }

    protected AttributeHandler[] attributeHandlers ()
    {
        return null;
    }

    protected SlotHandler[] slotHandlers()
    {
        return null;
    }

    public SlotHandler unassignedSlotHandler ()
    {
        return null;
    }

    protected final AttributeHandler attributeHandler (String name, Function<Object, Object> filterFn)
    {
        return new AttributeHandler(name, filterFn);
    }

    protected final SlotHandler slotHandler (String name, Function<Node, ReadableElement> filterFn)
    {
        return new SlotHandler(name, filterFn);
    }

    protected final Node noFilter (Node element)
    {
        return element;
    }

    protected final Object noFilter (Object object)
    {
        return object;
    }

    @Override
    public final ReadableAttribute getAttribute (String qualifiedName)
    {
        return null;
    }

    @Override
    public final Collection<? extends ReadableAttribute> getAttributes ()
    {
        return Collections.emptyList();
    }

    @Override
    public final boolean hasAttribute (String qualifiedName)
    {
        return false;
    }

    @Override
    public final List<Node> getChilds ()
    {
        return Collections.emptyList();
    }

    @Override
    public final Types getType ()
    {
        return Types.ELEMENT;
    }

    public static final class SlotHandler
    {
        private final String name;
        private final Function<Node, ReadableElement> filterFn;

        SlotHandler (String name, Function<Node, ReadableElement> filterFn)
        {
            this.name = name;
            this.filterFn = filterFn;
        }
    }

    public static final class AttributeHandler
    {
        private String name;
        private final Function<Object, Object> filterFn;

        AttributeHandler (String name, Function<Object, Object> filterFn)
        {
            this.name = name;
            this.filterFn = filterFn;
        }
    }
}
