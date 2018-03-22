package pt.neticle.ark.templating.parsing;

import pt.neticle.ark.templating.exception.SanityException;
import pt.neticle.ark.templating.structure.TemplateElement;
import pt.neticle.ark.templating.structure.TemplateRootElement;

import java.text.ParseException;
import java.util.Map;

public class DefaultTemplateHandler implements TemplateHandler
{
    private final TemplateRootElement providedTemplate;
    private TemplateRootElement rootElement;
    private TemplateElement currentElement;

    public DefaultTemplateHandler (TemplateRootElement provided)
    {
        providedTemplate = provided;
    }

    @Override
    public boolean startElement (String qName, Map<String, String> attributes) throws SanityException
    {
        if(rootElement == null)
        {
            if(!qName.equals("template"))
            {
                throw new SanityException("Root element must be a template element");
            }

            currentElement = rootElement = providedTemplate;
        }
        else
        {
            TemplateElement next = rootElement.createElement(qName);
            currentElement.addChild(next);
            currentElement = next;
        }

        for(Map.Entry<String,String> attr : attributes.entrySet())
        {
            try
            {
                currentElement.setAttribute(attr.getKey(), rootElement.createText(attr.getValue()));
            } catch(ParseException e)
            {
                throw new SanityException("Mal-formed expression", e);
            }
        }

        return !qName.equals("script") && !attributes.containsKey("text-content");
    }

    @Override
    public void textNode (String text) throws SanityException
    {
        if(currentElement != null)
        {
            if(currentElement.hasAttribute("text-content"))
            {
                text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            }

            try
            {
                currentElement.addText(rootElement.createText(text));
            } catch(ParseException e)
            {
                throw new SanityException("Mal-formed expression", e);
            }
        }
    }

    @Override
    public void endElement (String qName) throws SanityException
    {
        if(currentElement == null || !currentElement.getTagName().equals(qName))
        {
            throw new SanityException("Closing tag mismatch. </" + qName + "> found" +
                (currentElement == null ? "." :
                " while closing " + currentElement.getTagName() + " element."));
        }

        currentElement.removeAttribute("text-content");

        rootElement.elementReady(currentElement);
        currentElement = currentElement.getParent();
    }
}
