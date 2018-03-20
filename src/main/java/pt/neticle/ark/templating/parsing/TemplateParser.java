package pt.neticle.ark.templating.parsing;

import pt.neticle.ark.templating.structure.TemplateRootElement;

import java.io.InputStream;
import java.text.ParseException;

public interface TemplateParser
{
    /**
     * Parses the contents of the template declaration provided in text format in the input stream
     * into the provided template root element.
     *
     * @param provided A blank root element to be populated
     * @param is An input stream containing the template declaration in text format
     * @return The root element instance provided
     * @throws ParseException
     */
    TemplateRootElement parse (TemplateRootElement provided, InputStream is) throws ParseException;
}
