package pt.neticle.ark.templating.parsing;

import java.text.ParseException;
import java.util.Map;

public interface TemplateHandler
{
    /**
     * Invoked on the beginning of an element declaration (opening-tag parsed)
     *
     * @param qName The element name, will contain the full name with namespaces if present
     * @param attributes A map of the attributes passed on element declaration
     *
     * @return Return true to instruct the parser to continue parsing the element's children
     * normally, or false to instruct the parser to treat the element's content as text.
     *
     * @throws ParseException
     */
    boolean startElement (String qName, Map<String, String> attributes) throws ParseException;

    /**
     * Invoked when a text node is encountered
     *
     * @param text The text encountered
     *
     * @throws ParseException
     */
    void textNode (String text) throws ParseException;

    /**
     * Invoked at the end of an element declaration (closing-tag parsed)
     *
     * Note: The DefaultTemplateParser doesn't test for tag matching, it simply emits
     * these callbacks whenever an event occurs. It's the handler's role to check for the sanity
     * of the data, and in this case, check if the tag that is closing is the one of the current
     * opened element.
     *
     * @param qName The element name, will contain the full name with namespaces if present
     *
     * @throws ParseException
     */
    void endElement (String qName) throws ParseException;
}
