package pt.neticle.ark.templating.parsing;

import pt.neticle.ark.templating.exception.ParsingException;
import pt.neticle.ark.templating.structure.TemplateRootElement;

import java.io.IOException;
import java.io.InputStream;

public interface TemplateParser
{
    /**
     * Parses the contents of the template declaration provided in text format in the input stream
     * into the provided template root element.
     *
     * @param provided A blank root element to be populated
     * @param is An input stream containing the template declaration in text format
     * @return The root element instance provided
     * @throws ParsingException Thrown for any errors during the parsing process
     * @throws IOException Thrown for any errors reading from the provided stream
     */
    TemplateRootElement parse (TemplateRootElement provided, InputStream is) throws ParsingException, IOException;
}
