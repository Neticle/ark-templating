package pt.neticle.ark.templating.parsing;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import pt.neticle.ark.templating.structure.TemplateElement;
import pt.neticle.ark.templating.structure.TemplateRootElement;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public class TemplateParser
{
    private final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();


    protected XmlTemplateParser createHandler (TemplateRootElement provided)
    {
        return new DefaultXmlTemplateParser(provided);
    }

    public TemplateRootElement parse (TemplateRootElement provided, InputStream is) throws ParserConfigurationException, SAXException, IOException
    {
        XmlTemplateParser handler = createHandler(provided);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        /*saxParser.getXMLReader().setEntityResolver((publicId, systemId) -> {
            System.out.println("entity " + publicId + " " + systemId);
        });*/

        saxParser.parse(is, handler);

        return handler.getRootElement();
    }

    private static class DefaultXmlTemplateParser extends XmlTemplateParser
    {
        private TemplateRootElement providedRoot;
        private TemplateRootElement rootElement;
        private TemplateElement currentElement;

        public DefaultXmlTemplateParser (TemplateRootElement providedRoot)
        {
            this.providedRoot = providedRoot;
        }

        @Override
        public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if(rootElement == null)
            {
                if(!qName.equals("template"))
                {
                    throw new SAXException("Root element must be a template element.");
                }

                currentElement = rootElement = providedRoot;
            }
            else
            {
                TemplateElement next = rootElement.createElement(qName);
                currentElement.addChild(next);
                currentElement = next;
            }

            for(int i = 0; i < attributes.getLength(); i++)
            {
                try
                {
                    currentElement.setAttribute(attributes.getQName(i), rootElement.createText(attributes.getValue(i)));
                } catch(ParseException e)
                {
                    throw new SAXException(e);
                }
            }
        }

        @Override
        public void endElement (String uri, String localName, String qName) throws SAXException
        {
            rootElement.elementReady(currentElement);
            currentElement = currentElement.getParent();
        }

        @Override
        public void characters (char[] ch, int start, int length) throws SAXException
        {
            String str = String.valueOf(ch, start, length);

            if(length == 0)
            {
                return;
            }

            try
            {
                currentElement.addText(rootElement.createText(str));
            } catch(ParseException e)
            {
                throw new SAXException(e);
            }
        }

        @Override
        public TemplateRootElement getRootElement ()
        {
            return rootElement;
        }
    }

    private static abstract class XmlTemplateParser extends DefaultHandler
    {
        public abstract TemplateRootElement getRootElement ();
    }
}
