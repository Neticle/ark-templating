package pt.neticle.ark.templating.structure;

import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.structure.expressions.Expression;

import java.text.ParseException;
import java.util.*;

public class TemplateRootElement extends TemplateElement
{
    private final Set<String> childElementTypes;
    private final Set<String> slots;
    private final Map<String, String> metaData;
    private final TemplatingEngine engine;
    private ReadableElement catchUnassignedSlot = null;

    public TemplateRootElement (TemplatingEngine engine)
    {
        super(null);

        this.engine = engine;
        childElementTypes = new HashSet<>();
        slots = new HashSet<>();
        metaData = new HashMap<>();

        setTagName("template");
    }

    public TemplatingEngine getEngine ()
    {
        return engine;
    }

    public Expression createExpression (String text) throws ParseException
    {
        return engine.getExpressionMatcher().match(text);
    }

    public String getTemplateName ()
    {
        return this.getAttribute("name").getValue().getContent();
    }

    public Map<String, String> getMetaData ()
    {
        return metaData;
    }

    public TemplateElement createElement (String qualifiedName)
    {
        childElementTypes.add(qualifiedName);
        return new TemplateElement(this, qualifiedName);
    }

    public ReadableElement getUnassignedSlot ()
    {
        return catchUnassignedSlot;
    }

    public boolean hasUnassignedSlot ()
    {
        return catchUnassignedSlot != null;
    }

    public void elementReady (TemplateElement element)
    {
        if(element.getTagName().equals("slot"))
        {
            if(element.hasAttribute("name"))
            {
                slots.add(element.getAttribute("name").getValue().getContent());
            }
            else
            {
                catchUnassignedSlot = element;
            }
        }

        else if (element.getTagName().equals("t:meta") &&
            element.hasAttribute("key") &&
            element.hasAttribute("value"))
        {
            ReadableAttribute keyAttr = element.getAttribute("key");
            ReadableAttribute valueAttr = element.getAttribute("value");

            metaData.put(keyAttr.getValue().getContent(), valueAttr.getValue().getContent());
        }
    }

    public Set<String> getSlots ()
    {
        return slots;
    }

    public TemplateText createText (String textContent) throws ParseException
    {
        if(TemplateExpressionText.checkForReferences(textContent))
        {
            return new TemplateExpressionText(this, textContent);
        }

        return new TemplateText(this, textContent);
    }

    public void dump ()
    {

    }
}
