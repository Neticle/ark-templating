package pt.neticle.ark.templating.parsing;

import pt.neticle.ark.templating.exception.ParsingException;
import pt.neticle.ark.templating.exception.SanityException;
import pt.neticle.ark.templating.structure.TemplateRootElement;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static Pattern tagNamePt = Pattern.compile("^([\\w\\d-:]+)");

    @Override
    public TemplateRootElement parse (TemplateRootElement provided, InputStream is) throws ParsingException, IOException
    {
        DefaultTemplateHandler handler = new DefaultTemplateHandler(provided);

        parseFromReader(new InputStreamReader(is, StandardCharsets.UTF_8), handler);

        return provided;
    }

    private void parseFromReader (InputStreamReader reader, TemplateHandler handler) throws IOException, ParsingException
    {
        TemplateParserContext ctx = new TemplateParserContext();
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

                if(handle(c, ctx, handler))
                {
                    ctx.setStateEscapeSeq(c == '\\');

                    if(!ctx.isStateEscapeSeq())
                    {
                        ctx.appendToBuffer(c);
                    }
                }
                else
                {
                    ctx.setStateEscapeSeq(c == '\\');
                }
            }
        }
        catch (SanityException e)
        {
            throw new ParsingException("Parsing error: " + e.getMessage(), e, index, line, column);
        }
    }

    /**
     * Handles character being read, given a context and an handler
     *
     * @param c The current character
     * @param ctx The parsing context
     * @param handler The template handler
     * @return True if current character is to be appended to buffer, false otherwise.
     * @throws SanityException Propagates sanity exceptions thrown by the handler
     */
    private boolean handle (char c, TemplateParserContext ctx, TemplateHandler handler) throws SanityException
    {
        if(ctx.isStateTextOnly())
        {
            // When in text-only mode we'll save anything until we encounter the closing-tag
            ctx.appendToBuffer(c);

            if(c == '>')
            {
                // Only doing the check when we encounter a > character saves us the trouble of
                // pointless checks

                Matcher m = ctx.getTextOnlyUntil().matcher(ctx.getBufferString());

                if(m.find())
                {
                    handler.textNode(m.group(1));
                    handler.endElement(ctx.getTextOnlyUntilTag());

                    ctx.disableTextOnlyState();
                    ctx.resetBuffer();
                }
            }

            return false;
        }

        // When we encounter double-quotes within a tag definition we're entering
        // an attribute-value. We set this flag so that we can test later if we can
        // ignore certain characters such as < and >
        ctx.setStateInAttrValue
        (
            ctx.isStateInAttrValue() ?

            (c != '"' || ctx.isStateEscapeSeq()) :
            (ctx.isStateTagOpen() && c == '"')
        );

        if(!ctx.isStateInAttrValue())
        {
            if(c == '<')
            {
                // if we encounter a '<' character after we entered a tag definition, we treat the
                // previous match as a false-positive, building a text node with the content we've
                // read so far

                handler.textNode(ctx.getBufferString());
                ctx.resetBuffer();
                ctx.setStateTagOpen(true);
            }

            else if(c == '>' && ctx.isStateTagOpen())
            {
                // In a tag-open state, we only consider it to really be a tag-opening if
                // - we encounter the closing '>' character
                // - what we've collected so far makes sense as a tag

                handlePossibleTag(ctx.getBufferString().trim().substring(1), ctx, handler);

                ctx.setStateTagOpen(false);
                ctx.resetBuffer();

                return false;
            }
        }

        return true;
    }

    private void handlePossibleTag (String tagBody, TemplateParserContext ctx, TemplateHandler handler) throws SanityException
    {
        String tagName;
        boolean closeTag = tagBody.matches("^\\s*/.*");
        boolean selfClosing = !closeTag && tagBody.matches("^[\\S\\s]+\\s*/\\s*$");

        if(closeTag)
        {
            handler.endElement(tagBody.substring(tagBody.indexOf('/') + 1).trim());
        }

        else
        {
            if(selfClosing)
            {
                tagBody = tagBody.substring(0, tagBody.lastIndexOf('/')).trim();
            }

            Map<String, String> attributes;
            Matcher tagNameMatcher = tagNamePt.matcher(tagBody);

            if(tagNameMatcher.find())
            {
                tagName = tagNameMatcher.group(1);

                if(tagBody.substring(tagNameMatcher.end(1)).matches("^\\s[\\S\\s]+$"))
                {
                    attributes = new AttributeParser(tagBody.substring(tagBody.indexOf(' ') + 1)).getMap();
                } else
                {
                    attributes = Collections.emptyMap();
                }

                boolean parseChildsNormally = handler.startElement(tagName, attributes);

                if(selfClosing)
                {
                    handler.endElement(tagName);
                }

                if(!selfClosing && !parseChildsNormally && tagName != null)
                {
                    // the handler can instruct the parser not to parse an element's children
                    // if that's the case we go into text-only mode until we find the closing tag

                    ctx.enableTextOnlyState(Pattern.compile("([\\S\\s]*)</" + Pattern.quote(tagName) + ">$"), tagName);
                }
            }
        }
    }
}
