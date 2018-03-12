package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.Node;
import pt.neticle.ark.templating.structure.ReadableElement;

import java.util.List;

public interface Scope
{
    Scope getParent ();
    boolean available (String key);
    Object get (String key);
    List<ReadableElement> getElementsForSlot (String slot);
    List<Node> getUnassignedSlotNodes ();
}
