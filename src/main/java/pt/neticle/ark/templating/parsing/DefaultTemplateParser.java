package pt.neticle.ark.templating.parsing;

import pt.neticle.ark.templating.exception.ParsingException;
import pt.neticle.ark.templating.exception.SanityException;
import pt.neticle.ark.templating.structure.TemplateRootElement;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A simple template parser. This is not a full-fledged XML parser. It is far more lenient than your
 * average parser.
 *
 * This implementation, together with the DefaultTemplateHandler, will parse pretty much anything as
 * long as open and close tags match.
 *
 * lesser-than and greater-than symbols can be used in text nodes, the parser will consider those text
 * if they don't form a valid tag.
 *
 * Some requirements are:
 * * the usage of double-quotes for attribute values;
 * * root node must be a named template node
 * * all opened tags must be closed, but you can write self-closing tags
 *
 * If you add the attribute "text-content" to an element, the default handler will instruct this parser
 * to not parse the contents of that element as child elements. Instead, the content of the element will
 * be treated as text regardless of any markup contained in it.
 *
 * "script" elements are implied to be text-content elements and thus, the content of these is always
 * treated as text.
 */
public class DefaultTemplateParser implements TemplateParser
{
    @Override
    public TemplateRootElement parse (TemplateRootElement provided, InputStream is) throws ParsingException, IOException
    {
        DefaultTemplateHandler handler = new DefaultTemplateHandler(provided);

        parseFromReader(new InputStreamReader(is, StandardCharsets.UTF_8), handler);

        return provided;
    }

    private void parseFromReader (InputStreamReader reader, TemplateHandler handler) throws IOException, ParsingException
    {
        boolean stateTagOpen = false;
        boolean stateInAttrValue = false;
        boolean stateEscapeSeq = false;

        boolean stateTextOnly = false;
        Pattern textOnlyUntil = null;
        String textOnlyUntilTag = null;

        StringBuilder builder = new StringBuilder();

        int ci, index = -1, line = 1, column = 0;

        try
        {
            while((ci = reader.read()) != -1)
            {
                index++;
                column++;

                char c = (char) ci;

                if(c == '\n')
                {
                    line++;
                    column = 0;
                }

                if(stateTextOnly)
                {
                    // When in text-only mode we'll save anything until we encounter the closing-tag
                    builder.append(c);

                    if(c == '>')
                    {
                        // Only doing the check when we encounter a > character saves us the trouble of
                        // pointless checks

                        Matcher m = textOnlyUntil.matcher(builder.toString());

                        if(m.find())
                        {
                            handler.textNode(m.group(1));
                            handler.endElement(textOnlyUntilTag);

                            stateTextOnly = false;
                            textOnlyUntil = null;
                            textOnlyUntilTag = null;
                            builder = new StringBuilder();
                        }
                    }

                    continue;
                }

                if(!stateInAttrValue && stateTagOpen && c == '"')
                {
                    // When we encounter double-quotes within a tag definition we're entering
                    // an attribute-value. We set this flag so that we can test later if we can
                    // ignore certain characters such as < and >
                    stateInAttrValue = true;
                } else if(stateInAttrValue && c == '"' && !stateEscapeSeq)
                {
                    // Unless in an escaping sequence, we've encountered the closing double-quote
                    stateInAttrValue = false;
                }

                if(!stateTagOpen && c == '<')
                {
                    // We might be entering a tag definition, so flush what we add before into
                    // a text node and set the flag

                    handler.textNode(builder.toString());
                    builder = new StringBuilder();
                    stateTagOpen = true;
                } else if(stateTagOpen && c == '>' && !stateInAttrValue)
                {
                    // In a tag-open state, we only consider it to really be a tag-opening if
                    // - we encounter the closing '>' character
                    // - what we've collected so far makes sense as a tag

                    String tagBody = builder.toString().trim().substring(1);
                    String tagName = null;

                    boolean parseChilds = true;
                    if(tagBody.matches("^[\\w\\d-:]+$"))
                    {
                        // valid tag name with no attributes

                        tagName = tagBody;
                        parseChilds = handler.startElement(tagBody, Collections.emptyMap());
                    } else if(tagBody.matches("^[\\w\\d-:]+\\s[\\S\\s]+$"))
                    {
                        // valid tag name with attributes

                        String attributes = tagBody.substring(tagBody.indexOf(' ') + 1);
                        tagName = tagBody.substring(0, tagBody.indexOf(' '));

                        parseChilds = handler.startElement(tagName, new AttributeParser(attributes).getMap());

                        // match self-closing tags
                        if(tagBody.matches("[\\S\\s]+/\\s*$"))
                        {
                            handler.endElement(tagName);
                            parseChilds = true;
                        }
                    }

                    if(tagBody.matches("^\\s*/.*"))
                    {
                        // match closing tag
                        handler.endElement(tagBody.substring(tagBody.indexOf('/') + 1));
                    } else if(!parseChilds && tagName != null)
                    {
                        // the handler can instruct the parser not to parse an element's children
                        // if that's the case we go into text-only mode until we find the closing tag

                        textOnlyUntil = Pattern.compile("([\\S\\s]*)</" + Pattern.quote(tagName) + ">$");
                        textOnlyUntilTag = tagName;
                        stateTextOnly = true;
                    }

                    stateTagOpen = false;
                    builder = new StringBuilder();
                    continue;
                } else if(stateTagOpen && c == '<' && !stateInAttrValue)
                {
                    // if we encounter a '<' character after we entered a tag definition, we treat the
                    // previous match as a false-positive, building a text node with the content we've
                    // read so far

                    handler.textNode(builder.toString());
                    builder = new StringBuilder();
                }

                // always reset the escape sequence flag
                stateEscapeSeq = false;

                if(c == '\\')
                {
                    // This flag is set here so that in the next iteration we know to ignore any relevant
                    // character.

                    stateEscapeSeq = true;
                }

                builder.append(c);
            }
        }
        catch (SanityException e)
        {
            throw new ParsingException("Parsing error: " + e.getMessage(), e, index, line, column);
        }
    }

    private static class AttributeParser
    {
        private final Map<String, String> attributeMap;

        public AttributeParser (String attributes)
        {
            attributeMap = new HashMap<>();

            String current = attributes.trim();

            // Matches anything resembling an attibute name followed by an equals sign
            while(current.matches("^[\\w\\d-:]+[$#]?=[\\S\\s]*$"))
            {
                String name = current.substring(0, current.indexOf('='));

                // In order to take escaped quotes in account, we iterate over the
                // value string to determine where the quotes start and end
                int quoteStart = -1, quoteEnd = -1;
                for(int i = current.indexOf('=')+1; i < current.length(); i++)
                {
                    if(quoteStart == -1 && current.charAt(i) == '"')
                    {
                        quoteStart = i;
                        continue;
                    }

                    if(quoteStart != -1 && current.charAt(i) == '\\' && current.charAt(i+1) == '"')
                    {
                        i++;
                        continue;
                    }

                    if(quoteStart != -1 && current.charAt(i) == '"')
                    {
                        quoteEnd = i;
                        break;
                    }
                }

                if(quoteEnd == -1 || quoteEnd == -1)
                {
                    attributeMap.put(name, null);
                    break;
                }

                attributeMap.put(name, current.substring(quoteStart+1, quoteEnd));

                current = current.substring(quoteEnd+1).trim();
            }
        }

        public Stream<Map.Entry<String,String>> attributes ()
        {
            return attributeMap.entrySet().stream();
        }

        public Map<String,String> getMap ()
        {
            return attributeMap;
        }
    }
}
