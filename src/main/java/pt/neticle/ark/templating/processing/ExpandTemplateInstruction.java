package pt.neticle.ark.templating.processing;

import pt.neticle.ark.templating.structure.TemplateExpressionText;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Instructs the renderer to expand template content.
 *
 * This instruction contains information about the data passed to the declaring node
 * for this expansion.
 *
 * Included here are
 *  - The template name -- can be the qualified name of a known template, or simply "template"
 *    for inner-templates
 *  - A map containing instructions for each slot. For each slot there is a list of instructions
 *    on how to render the slotted content.
 *  - A map containing attributes passed on declaration. Each attribute is mapped to a list of
 *    segments because attribute values may be composed of both plain-text and expressions.
 */
public class ExpandTemplateInstruction extends Instruction
{
    private final String templateName;
    private final Map<String, List<Instruction>> preprocessedSlotMembers;
    private final Map<String, List<TemplateExpressionText.Segment>> attributes;

    ExpandTemplateInstruction (String templateName,
                                      Map<String, List<Instruction>> preprocessedSlotMembers,
                                      Map<String, List<TemplateExpressionText.Segment>> attributes)
    {
        super(Type.EXPAND_TEMPLATE);

        this.templateName = templateName;
        this.preprocessedSlotMembers = preprocessedSlotMembers;
        this.attributes = attributes;
    }

    public String getTemplateName ()
    {
        return templateName;
    }

    public Map<String, List<Instruction>> getPreprocessedSlotMembers ()
    {
        return preprocessedSlotMembers;
    }

    public Map<String, List<TemplateExpressionText.Segment>> getAttributes ()
    {
        return attributes;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString() + templateName + ":");

        preprocessedSlotMembers.entrySet()
        .forEach(e ->
        {
            sb.append("(Slot ");
            sb.append(e.getKey());
            sb.append(":sz " + e.getValue().size());
            sb.append("){");

            e.getValue().stream().forEach((i) ->
            {
                Instruction ci = i;
                while(ci != null)
                {
                    sb.append(ci.toString());
                    ci = ci.getNext();
                }
            });

            sb.append("}");
        });

        sb.append(")(Attributes: ");
        sb.append(attributes.keySet().stream().collect(Collectors.joining(", ")));
        sb.append(")]");

        return sb.toString();
    }
}
