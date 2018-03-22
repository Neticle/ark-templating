package pt.neticle.ark.templating.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TemplateElement extends TemplateNode implements Element
{
    private String qualifiedName;
    private final HashMap<String, Attribute> attributes;
    private final List<Node> childs;
    private TemplateElement parent = null;

    TemplateElement (TemplateRootElement templateRoot, String qualifiedName)
    {
        this(templateRoot);
        this.qualifiedName = qualifiedName;
    }

    TemplateElement (TemplateRootElement templateRoot)
    {
        super(templateRoot);
        attributes = new HashMap<>();
        childs = new ArrayList<>();
    }

    private void setParent (TemplateElement parent)
    {
        this.parent = parent;
    }

    public final TemplateElement getParent ()
    {
        return this.parent;
    }

    @Override
    public final void setTagName (String qualifiedName)
    {
        this.qualifiedName = qualifiedName;
    }

    @Override
    public final void setAttribute (String qualifiedName, ReadableText text)
    {
        attributes.put(qualifiedName, new TemplateAttribute(getTemplateRootElement(), qualifiedName, text));
    }

    @Override
    public void removeAttribute (String qualifiedName)
    {
        attributes.remove(qualifiedName);
    }

    @Override
    public final void addChild (ReadableElement child)
    {
        if(child instanceof TemplateElement)
        {
            ((TemplateElement)child).setParent(this);
        }

        childs.add(child);
    }

    @Override
    public final void addText (ReadableText text)
    {
        childs.add(text);
    }

    @Override
    public final String getTagName ()
    {
        return qualifiedName;
    }

    @Override
    public final ReadableAttribute getAttribute (String qualifiedName)
    {
        return attributes.get(qualifiedName);
    }

    @Override
    public final Collection<? extends ReadableAttribute> getAttributes ()
    {
        return attributes.values();
    }

    @Override
    public final boolean hasAttribute (String qualifiedName)
    {
        return attributes.containsKey(qualifiedName);
    }

    @Override
    public final List<Node> getChilds ()
    {
        return childs;
    }

    @Override
    public final Types getType ()
    {
        return Types.ELEMENT;
    }
}
