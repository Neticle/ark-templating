package pt.neticle.ark.templating.processing;

import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.structure.*;
import pt.neticle.ark.templating.structure.expressions.OutputExpression;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A preprocessed set of instructions to render a given XML node.
 *
 * This class visits a given XML structure and produces instructions
 * that can be used by our PreprocessedRenderer to render output.
 */
public class PreprocessedInstructionSet
{
    private final TemplatingEngine engine;
    private final Instruction root;
    private Instruction current;

    public PreprocessedInstructionSet (TemplatingEngine engine, Node node)
    {
        this.engine = engine;

        root = current = new RawOutputInstruction();

        visit(node);
    }

    public Instruction getRoot ()
    {
        return root;
    }

    public void dump (PrintStream out)
    {
        Instruction current = root;
        while(current != null)
        {
            out.println(current.toString());
            current = current.getNext();
        }
    }

    private void visit (Node node)
    {
        switch(node.getType())
        {
            case TEXT:
                visitText((ReadableText) node);
                break;
            case ELEMENT:
                visitElement((ReadableElement) node);
                break;
            case ATTRIBUTE:
                visitAttribute((ReadableAttribute) node);
                break;
        }
    }

    private void visitElement (ReadableElement element)
    {
        if(element instanceof TemplateRootElement)
        {
            element.childs().forEach(this::visit);
            return;
        }

        if(element.getTagName().equals("slot"))
        {
            String slotName = element.hasAttribute("name") ?
                element.getAttribute("name").getValue().getContent() :
                null;

            current = current.setNext(new ExpandSlotInstruction(slotName));
            return;
        }

        if(element.getTagName().equals("template") || engine.hasTemplate(element.getTagName()))
        {
            TemplateRootElement templateElement = !element.getTagName().equals("template") ?
                (TemplateRootElement) engine.getTemplate(element.getTagName()) :
                null;

            Set<String> whitelistedSlots = templateElement != null ?
                templateElement.getSlots() :
                Stream.of("else", "empty").collect(Collectors.toSet());

            Map<String, List<Instruction>> preprocessedSlotMembers = new HashMap<>();
            for(String reqSlotName : whitelistedSlots)
            {
                List<Instruction> pl = element.childs()
                    .filter(e -> e instanceof ReadableElement)
                    .map(e -> (ReadableElement)e)
                    .filter(e -> e.hasAttribute("slot") &&
                                 e.getAttribute("slot").getValue().getContent().equals(reqSlotName))
                    .map(e -> new PreprocessedInstructionSet(engine, e).getRoot())
                    .collect(Collectors.toList());

                // Grab any children of the declaring element that have an attribute "slot" and the value
                // is one of the whitelisted slots for this template
                preprocessedSlotMembers.put(reqSlotName, pl);
            }

            if(templateElement == null || templateElement.hasUnassignedSlot())
            {
                List<Instruction> pl = element.childs()
                    .filter((n) -> (n instanceof ReadableElement) &&
                            (!((ReadableElement)n).hasAttribute("slot")))
                    .map(n -> new PreprocessedInstructionSet(engine, n).getRoot())
                    .collect(Collectors.toList());

                // Grab any children of the declaring element that don't have an attribute "slot"
                // These go as unassigned children
                preprocessedSlotMembers.put("@unassigned", pl);
            }

            Map<String, List<TemplateExpressionText.Segment>> attributes =
                element.attributes()
                .collect(Collectors.toMap(
                    a -> a.getName(),
                    a ->
                    {
                        // Grab the attributes of the declaring element.

                        if(a.getValue() instanceof TemplateExpressionText)
                        {
                            return ((TemplateExpressionText)a.getValue()).getSegments();
                        }

                        return Collections.singletonList(
                            new TemplateExpressionText.Segment(
                                TemplateExpressionText.Segment.Type.TEXT,
                                a.getValue().getContent()));
                    }
                ));

            current = current.setNext(new ExpandTemplateInstruction(
                    element.getTagName(), preprocessedSlotMembers, attributes));

            return;
        }

        // If we reached here, it means the element is just a plain old html element
        // and we're simply rendering it as it is.

        appendRawOutput("<" + element.getTagName());

        for(ReadableAttribute attr : element.getAttributes())
        {
            visit(attr);
        }

        appendRawOutput(">");

        element.childs().forEach(this::visit);

        appendRawOutput("</" + element.getTagName() + ">");
    }

    private void visitAttribute (ReadableAttribute attribute)
    {
        if(attribute.getName().equals("slot"))
        {
            // slots don't get rendered, and they are grabbed when visiting a template node
            // so we do nothing here.
            return;
        }

        appendRawOutput(" " + attribute.getName().replace("[^a-zA-Z0-9-]", ""));

        if(attribute.getValue() != null)
        {
            appendRawOutput("=\"");

            if(attribute.getValue() instanceof TemplateExpressionText)
            {
                TemplateExpressionText extx = (TemplateExpressionText) attribute.getValue();

                // Expression Text objects are made up of "sub segments" that may be plain
                // text or an expression

                for(TemplateExpressionText.Segment segment : extx.getSegments())
                {
                    if(segment.getType() == TemplateExpressionText.Segment.Type.TEXT)
                    {
                        appendRawOutput((String)segment.getObject());
                    }
                    else if(segment.getType() == TemplateExpressionText.Segment.Type.EXPRESSION)
                    {
                        // For expression segments, we only care about those that contain an
                        // output expression here

                        if(segment.getObject() instanceof OutputExpression)
                        {
                            current =
                                current.setNext(new ExpressionResultOutputInstruction(
                                    (OutputExpression)segment.getObject()));
                        }
                    }
                }
            }
            else
            {
                appendRawOutput(attribute.getValue().getContent().replace("\"", "\\\""));
            }

            appendRawOutput("\"");
        }
    }

    private void visitText (ReadableText text)
    {
        if(text instanceof TemplateExpressionText)
        {
            TemplateExpressionText extx = (TemplateExpressionText) text;

            // Expression Text objects are made up of "sub segments" that may be plain
            // text or an expression

            for(TemplateExpressionText.Segment segment : extx.getSegments())
            {
                if(segment.getType() == TemplateExpressionText.Segment.Type.TEXT)
                {
                    appendRawOutput((String) segment.getObject());
                } else if(segment.getType() == TemplateExpressionText.Segment.Type.EXPRESSION)
                {
                    // For expression segments, we only care about those that contain an
                    // output expression here

                    if(segment.getObject() instanceof OutputExpression)
                    {
                        current =
                            current.setNext(new ExpressionResultOutputInstruction(
                                (OutputExpression) segment.getObject()));
                    }
                }
            }

            return;
        }

        appendRawOutput(text.getContent());
    }

    private void appendRawOutput (String str)
    {
        if(!current.is(Instruction.Type.RAW_OUTPUT))
        {
            current = current.setNext(new RawOutputInstruction());
        }

        ((RawOutputInstruction)current).append(str);
    }
}
