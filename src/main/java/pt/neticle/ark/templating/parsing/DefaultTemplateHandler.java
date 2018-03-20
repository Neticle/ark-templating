package pt.neticle.ark.templating.parsing;

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
    public boolean startElement (String qName, Map<String, String> attributes) throws ParseException
    {
        if(rootElement == null)
        {
            if(!qName.equals("template"))
            {
                throw new RuntimeException("Root element must be a template element");
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
            currentElement.setAttribute(attr.getKey(), rootElement.createText(attr.getValue()));
        }

        return !qName.equals("script") && !attributes.containsKey("text-content");
    }

    @Override
    public void textNode (String text) throws ParseException
    {
        if(currentElement != null)
        {
            if(currentElement.hasAttribute("text-content"))
            {
                text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            }

            currentElement.addText(rootElement.createText(text));
        }
    }

    @Override
    public void endElement (String qName) throws ParseException
    {
        if(currentElement == null || !currentElement.getTagName().equals(qName))
        {
            throw new ParseException("Closing tag mismatch. </" + qName + ">", 0);
        }

        rootElement.elementReady(currentElement);
        currentElement = currentElement.getParent();
    }
}
