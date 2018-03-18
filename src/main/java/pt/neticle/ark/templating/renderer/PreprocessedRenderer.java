package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.exception.RenderingException;
import pt.neticle.ark.templating.processing.*;
import pt.neticle.ark.templating.structure.TemplateExpressionText;
import pt.neticle.ark.templating.structure.TemplateRootElement;
import pt.neticle.ark.templating.structure.expressions.Expression;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Renders output based on a given instruction set.
 */
public class PreprocessedRenderer
{
    private final PreprocessedRenderer parent;
    private final TemplatingEngine engine;
    private Scope scope;
    private final OutputStream ostream;
    private final Map<String, List<Instruction>> preprocessedSlotMembers;

    public PreprocessedRenderer (TemplatingEngine engine, PreprocessedInstructionSet instructionSet,
                                 Scope scope, OutputStream os,
                                 Map<String, List<Instruction>> preprocessedSlotMembers)
    {
        this(null, engine, instructionSet, scope, os, preprocessedSlotMembers);
    }

    private PreprocessedRenderer (PreprocessedRenderer parent, TemplatingEngine engine, PreprocessedInstructionSet instructionSet,
                                 Scope scope, OutputStream os,
                                 Map<String, List<Instruction>> preprocessedSlotMembers)
    {
        this(parent, engine, scope, os, instructionSet.getRoot(), preprocessedSlotMembers);
    }


    private PreprocessedRenderer (PreprocessedRenderer parent, TemplatingEngine engine,
                                 Scope scope, OutputStream os, Instruction instruction,
                                 Map<String, List<Instruction>> preprocessedSlotMembers)
    {
        this.parent = parent;
        this.engine = engine;
        this.scope = scope;
        this.ostream = os;
        this.preprocessedSlotMembers = preprocessedSlotMembers;

        if(instruction != null)
        {
            accept(instruction);
        }
    }

    private void accept (Instruction instruction)
    {
        Instruction current = instruction;
        while(current != null)
        {
            visit(current);
            current = current.getNext();
        }
    }

    private void visit (Instruction instruction)
    {
        switch(instruction.getType())
        {
            case RAW_OUTPUT:
                visitRawOutputInst((RawOutputInstruction) instruction);
                break;
            case EXPRESSION_RESULT_OUTPUT:
                visitExpressionResultOutputInst((ExpressionResultOutputInstruction) instruction);
                break;
            case EXPAND_SLOT:
                visitExpandSlotInst((ExpandSlotInstruction) instruction);
                break;
            case EXPAND_INNER_TEMPLATE:
                break;
            case EXPAND_TEMPLATE:
                visitExpandTemplateInst((ExpandTemplateInstruction) instruction);
                break;
        }
    }

    private void visitRawOutputInst (RawOutputInstruction inst)
    {
        try
        {
            ostream.write(inst.getContent().getBytes(StandardCharsets.UTF_8));
        } catch(IOException e)
        {
            throw new RenderingException(e);
        }
    }

    private void visitExpressionResultOutputInst (ExpressionResultOutputInstruction inst)
    {
        Object result = scope.evaluate(inst.getExpression());

        if(result != null)
        {
            try
            {
                ostream.write(result.toString().getBytes(StandardCharsets.UTF_8));
            }
            catch(IOException e)
            {
                throw new RenderingException(e);
            }
        }
    }

    private void visitExpandSlotInst (ExpandSlotInstruction inst)
    {
        if(preprocessedSlotMembers == null && parent != null)
        {
            // If we're in a nested renderer that has no slotMember information, it means
            // a slot was passed within a slot, and so, the parent if available, should try
            // to render it.

            parent.visitExpandSlotInst(inst);
            return;
        }

        final String slotName = inst.isUnassignedSlot() ? "@unassigned" : inst.getSlotName();

        if(preprocessedSlotMembers.containsKey(slotName))
        {
            final PreprocessedRenderer pr = new PreprocessedRenderer(parent, engine, scope, ostream, null, null);
            preprocessedSlotMembers.get(slotName).stream()
            .forEach(pr::accept);
        }
    }

    private void visitExpandTemplateInst (ExpandTemplateInstruction inst)
    {
        if(inst.getTemplateName().equals("template"))
        {
            // if the template is an inner-template, we have special handlers for that...
            visitExpandInnerTemplateInst(inst);
            return;
        }

        final InternalScope newScope = new InternalScope(scope);

        // Grab the attributes from the expand-template instruction
        inst.getAttributes().entrySet().stream()
        .forEach(e ->
        {
            // Attributes are passed pre-resolved

            // If an attribute value consists of only an expression, we pass the result of that expression

            // Otherwise, if the value has multiple segments, we pass a string containing the concatenation of
            // the expression segment results and the plain-text segments.

            if(e.getValue().size() == 1 && e.getValue().get(0).getObject() instanceof Expression)
            {
                newScope.put(e.getKey(), scope.evaluate((Expression)e.getValue().get(0).getObject()));
            }
            else
            {
                String s = "";

                for(TemplateExpressionText.Segment segment : e.getValue())
                {
                    if(segment.getType() == TemplateExpressionText.Segment.Type.TEXT)
                    {
                        s += (String) segment.getObject();
                    }
                    else if(segment.getType() == TemplateExpressionText.Segment.Type.EXPRESSION)
                    {
                        Object result = scope.evaluate((Expression) segment.getObject());

                        if(result != null)
                            s += result.toString();
                    }
                }

                newScope.put(e.getKey(), s);
            }
        });

        // Fire up a new sub-renderer with the resolved template's instruction set
        new PreprocessedRenderer(
            this,
            engine,
            ((TemplateRootElement) engine.getTemplate(inst.getTemplateName())).getInstructionSet(),
            newScope,
            ostream,
            inst.getPreprocessedSlotMembers()
        );
    }

    private void visitExpandInnerTemplateInst (ExpandTemplateInstruction inst)
    {
        Expression ifExpr;

        // All inner-templates containing an "if" attribute are first evaluated and then handled.

        if(inst.getAttributes().containsKey("if") &&
            (ifExpr = getExpressionIfSingleSegment(inst.getAttributes().get("if"))) != null)
        {
            if(!Boolean.valueOf(scope.evaluate(ifExpr).toString()))
            {
                // If the expression evaluated to false, we'll display the content of the "else" slot,
                // if available, and stop execution here.
                inst.getPreprocessedSlotMembers().get("else").stream()
                    .forEach(this::accept);
                return;
            }
        }

        if(inst.getAttributes().containsKey("is"))
        {
            // Depending on what the inner-template is, we'll find an handler for it.
            String is = getFlattenedSegments(inst.getAttributes().get("is"));

            if(is.equals("foreach"))
            {
                handleForeachTemplate(inst);
                return;
            }
        }
        else
        {
            // If the inner-template isn't of any type and since, if we reached here, it means the if
            // clause evaluated as true, we render the unassigned slotted content.

            inst.getPreprocessedSlotMembers().get("@unassigned").stream()
                .forEach(this::accept);
        }
    }

    private void handleForeachTemplate (ExpandTemplateInstruction inst)
    {
        // ForEach templates require a data attribute with an expression resolving to an iterable object

        final Expression dataExpr = expressionIfSingleSegment(inst.getAttributes().get("data"))
            .orElseThrow(() -> new RenderingException("Template Foreach element must specify a data attribute that is a reference"));

        final String as = flattenedSegments(inst.getAttributes().get("as"))
            .orElse("item");

        // If loop is set, we'll inject an object with the specified name on each iteration's scope. This
        // object would contain loop information such as current index, etc.
        final String loop = flattenedSegments(inst.getAttributes().get("loop"))
            .orElse(null);

        final Object result = scope.evaluate(dataExpr);

        final Instruction repeatable = inst.getPreprocessedSlotMembers().get("@unassigned").stream()
            .findFirst()
            .orElse(null);

        if(result == null || repeatable == null)
        {
            // When empty, we display slotted content from the "empty" slot, if any.
            inst.getPreprocessedSlotMembers().get("empty").stream()
                .forEach(this::accept);
            return;
        }

        final Stream<Object> results;

        // The result of the data expression must either be an Iterable or an array
        if(result instanceof Iterable)
        {
            results = StreamSupport.stream(((Iterable<Object>) result).spliterator(), false);
        }
        else if(result instanceof Object[])
        {
            results = Arrays.stream((Object[]) result);
        } else
        {
            throw new RenderingException("Unable to iterate over data of type " + result.getClass().getName() + " provided in the data attribute. " + result);
        }

        // TODO: Check for any negative implications of this, and possibly replace it with something else if so.
        final Integer[] totalIterated = {0}; // hack: defined like this to be able to change it from within a lambda

        // Instead of creating sub-renderers for this, we'll use current one but swap the scope temporarily
        // Save current scope here so we can come back to it.
        final Scope originalScope = scope;

        // we're making the inner scope a child of the current scope, so we can access variables defined outside
        // the for-each block
        final InternalScope newScope = new InternalScope(scope);
        scope = newScope;

        if(loop == null)
        {
            // If we dont have to provide loop information we can just consume the stream like this

            results.forEach((o) ->
            {
                // current item is supplied in the inner scope with the specified "as" name
                newScope.put(as, o);

                accept(repeatable);

                totalIterated[0]++;

                newScope.reset();
            });
        }
        else
        {
            // Basically the same thing but with an iterator because we need some extra info to populate the
            // loop information object

            Iterator<Object> it = results.iterator();
            totalIterated[0]++;
            while(it.hasNext())
            {
                Object o = it.next();

                newScope.put(as, o);
                newScope.put(loop, new ForeachIterationInfo(totalIterated[0], totalIterated[0] == 0, !it.hasNext()));

                accept(repeatable);

                totalIterated[0]++;

                newScope.reset();
            }
        }

        // Restore original scope after all is rendered
        scope = originalScope;

        if(totalIterated[0] == 0)
        {
            // If we didn't render anything, display contents of "empty" slot

            // This is done in the end because we can't know the size of a stream/iterable without
            // consuming/iterating it.

            inst.getPreprocessedSlotMembers().get("empty").stream()
                .forEach((i) -> accept(i));
        }
    }

    /**
     * Given a list of segments, grabs an expression if that expression is the sole member of the list.
     * @param segments
     * @return
     */
    private Optional<Expression> expressionIfSingleSegment (List<TemplateExpressionText.Segment> segments)
    {
        if(segments == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(getExpressionIfSingleSegment(segments));
    }

    /**
     * Given a list of segments, grabs an expression if that expression is the sole member of the list.
     * @param segments
     * @return
     */
    private Expression getExpressionIfSingleSegment (List<TemplateExpressionText.Segment> segments)
    {
        return segments.size() == 1 && segments.get(0).getType() == TemplateExpressionText.Segment.Type.EXPRESSION ?
                (Expression) segments.get(0).getObject() : null;
    }

    /**
     * Flattens the segments concatenating them into a single string. For expression segments, the
     * values are resolved and then converted to string before concatenating.
     *
     * @param segments
     * @return Optional present only if provided segments wasn't null and result of flattening wasn't
     * an empty string
     */
    private Optional<String> flattenedSegments (List<TemplateExpressionText.Segment> segments)
    {
        String r = getFlattenedSegments(segments);

        return r == null || r.isEmpty() ? Optional.empty() : Optional.of(r);
    }

    /**
     * Flattens the segments concatenating them into a single string. For expression segments, the
     * values are resolved and then converted to string before concatenating.
     * @param segments
     * @return null if no segments provided, empty string if no segments or empty segments
     */
    private String getFlattenedSegments (List<TemplateExpressionText.Segment> segments)
    {
        if(segments == null)
        {
            return null;
        }

        return segments.stream()
        .map(s ->
        {
            if(s.getType() == TemplateExpressionText.Segment.Type.EXPRESSION)
            {
                return scope.evaluate((Expression) s.getObject()).toString();
            }
            else if(s.getType() == TemplateExpressionText.Segment.Type.TEXT)
            {
                return (String)s.getObject();
            }

            return "";
        })
        .collect(Collectors.joining());
    }

}
