package pt.neticle.ark.templating.structure;

import pt.neticle.ark.templating.renderer.Scope;
import pt.neticle.ark.templating.structure.expressions.Expression;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateExpressionText extends TemplateText
{
    private static Pattern refCheck = Pattern.compile("\\{{2}(.+?)\\}{2}");

    private final List<Segment> segments;
    private final String templateContent;

    public TemplateExpressionText (TemplateRootElement templateRoot, String content) throws ParseException
    {
        super(templateRoot, content);

        segments = new ArrayList<>();

        Matcher m;
        while((m = refCheck.matcher(content)).find())
        {
            Expression expHandler = templateRoot.createExpression(m.group(1).trim());
            if(expHandler != null)
            {
                addSegment(new Segment(Segment.Type.TEXT, content.substring(0, m.start(1) - 2)));
                addSegment(new Segment(Segment.Type.EXPRESSION, expHandler));
            }
            else
            {
                addSegment(new Segment(Segment.Type.TEXT, "{{" + m.group(1) + "}}"));
            }

            content = content.substring(m.end(1) + 2);
        }

        addSegment(new Segment(Segment.Type.TEXT, content));

        templateContent = content;
    }

    private void addSegment(Segment segment)
    {
        if(segment.type == Segment.Type.TEXT && ((String)segment.object).length() == 0)
        {
            return;
        }

        segments.add(segment);
    }

    public Expression getExpressionIfOnlySegment ()
    {
        return segments.size() == 1 && segments.get(0).type == Segment.Type.EXPRESSION ?
            (Expression)segments.get(0).object :
            null;
    }

    public String getContent (Scope scope)
    {
        return segments.stream()
            .map((segment) ->
            {
                String content = null;

                if(segment.type == Segment.Type.TEXT)
                {
                    content = (String)segment.object;
                }

                else if(segment.type == Segment.Type.EXPRESSION)
                {
                    Object r = ((Expression)segment.object).resolve(scope);

                    return r != null ? r.toString() : null;
                }

                return content;
            })
            .collect(Collectors.joining());
    }

    public static boolean checkForReferences (String str)
    {
        return refCheck.matcher(str).find();
    }

    private static class Segment
    {
        public enum Type
        {
            TEXT,
            EXPRESSION
        }

        public final Type type;
        public final Object object;

        public Segment (Type type, Object object)
        {
            this.type = type;
            this.object = object;
        }
    }
}
