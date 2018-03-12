package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.PreparedElement;
import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.exception.RenderingException;
import pt.neticle.ark.templating.functional.CheckedConsumer;
import pt.neticle.ark.templating.structure.*;
import pt.neticle.ark.templating.structure.expressions.Expression;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Renderer extends BaseRenderer
{
    private static int counter = 0;
    private final TemplatingEngine engine;
    private final Scope scope;
    private final OutputStream ostream;
    private final ReadableElement root;
    private final int id;
    private final Map<ReadableElement, Renderer> rendererMap;
    private final Renderer parent;

    public Renderer (TemplatingEngine engine, ReadableElement element, Scope scope, OutputStream outputStream) throws IOException
    {
        this(engine, element, scope, outputStream, null);
        render(root);
    }

    public Renderer (TemplatingEngine engine, ReadableElement element, Scope scope, OutputStream outputStream, Renderer parent) throws IOException
    {
        this.engine = engine;
        this.root = element;
        this.scope = scope;
        this.ostream = outputStream;
        this.id = counter++;
        this.rendererMap = new HashMap<>();
        this.parent = parent;

        rendererMap.put(element, this);
    }

    private void newSubRenderer (ReadableElement element, Scope scope) throws IOException
    {
        Renderer sub = new Renderer(engine, element, scope, ostream, parent == null ? this : parent);

        (parent == null ? rendererMap : parent.rendererMap).put(element, sub);

        sub.render(sub.root);
    }

    private Renderer findRendererFor (ReadableElement element)
    {
        return parent == null ? rendererMap.get(element) : parent.findRendererFor(element);
    }

    private void render (Node node) throws IOException
    {
        switch(node.getType())
        {
            case TEXT:
                renderText((ReadableText)node);
                break;
            case ELEMENT:
                renderElement((ReadableElement)node);
                break;
            case ATTRIBUTE:
                renderAttribute((ReadableAttribute)node);
                break;
        }
    }

    private void renderElement (ReadableElement element) throws IOException
    {
        // we skip template element and just render the contents
        if(element instanceof TemplateRootElement)
        {
            element.childs().forEach(CheckedConsumer.rethrowConsumer(this::render));
            return;
        }

        // TODO: Maybe meta-data should be passed as attributes of the root template node instead
        if(element.getTagName().startsWith("t:"))
        {
            return;
        }

        if(element.getTagName().equals("slot"))
        {
            if(element instanceof TemplateNode && ((TemplateNode) element).getTemplateRootElement() != root)
            {
                Renderer renderer = findRendererFor(((TemplateNode) element).getTemplateRootElement());

                if(renderer == null)
                {
                    throw new RenderingException("Unable to find renderer for " + ((TemplateNode) element).getTemplateRootElement().getTemplateName() + " " + element);
                }

                renderer.renderElement(element);

                return;
            }

            renderSlotContent(element);
            return;
        }

        // templates have special handler code
        if(element.getTagName().equals("template"))
        {
            renderTemplate(element);
            return;
        }

        // for known elements that have defined templates
        if(engine.hasTemplate(element.getTagName()))
        {
            TemplateRootElement templateElement;

            templateElement = (TemplateRootElement) engine.getTemplate(element.getTagName()); // TODO: Fix this cast

            if(element instanceof PreparedElement)
            {
                // Prepared elements already come with a scope.
                newSubRenderer(templateElement, ((PreparedElement) element).getPreparedScope());
                return;
            }

            InternalScope newScope = new InternalScope(scope);

            // All attributes defined on the declaration element are passed to the new scope
            newScope.putAll(element.attributes()
                .collect(Collectors.toMap(
                    (a) -> a.getName(),
                    (a) -> {
                        if(a.getValue() instanceof TemplateExpressionText)
                        {
                            Expression exp = ((TemplateExpressionText) a.getValue()).getExpressionIfOnlySegment();

                            if(exp != null)
                            {
                                return exp.resolve(scope);
                            }
                            else
                            {
                                return ((TemplateExpressionText) a.getValue()).getContent(scope);
                            }
                        }

                        return a.getValue().getContent();
                    }
                ))
            );

            // Check requested slots from the template
            templateElement.getSlots().stream()
            .forEach((slotName) ->
            {
                // for each existing slot we'll find elements assigned to it
                element.childs()
                    .filter((e) -> (e instanceof ReadableElement))
                    .map((e) -> (ReadableElement) e)
                    .filter((e) -> e.hasAttribute("slot") && e.getAttribute("slot").getValue().getContent().equals(slotName))
                    .forEach((e) -> newScope.addSlottedElement(slotName, e));
            });

            if(templateElement.hasUnassignedSlot())
            {
                element.childs()
                    .filter((n) -> !(n instanceof ReadableElement) ||
                        (!((ReadableElement) n).hasAttribute("slot")))
                    .forEach((n) -> newScope.addSlottedNode(n));
            }

            newSubRenderer(templateElement, newScope);
            return;
        }

        // If none of the above aplied, this is a plain-old element without a defined template, so we'll just render it
        // as it is.
        openTagBegin(element, ostream);

        for(ReadableAttribute attr : element.getAttributes())
        {
            renderAttribute(attr);
        }

        openTagEnd(element, ostream);

        element.childs().forEach(CheckedConsumer.rethrowConsumer(this::render));

        closeTag(element, ostream);
    }

    private void renderAttribute (ReadableAttribute attribute) throws IOException
    {
        // slot attribute is used internally, but it doesn't show up in the rendered result.
        if(attribute.getName().equals("slot"))
        {
            return;
        }

        ostream.write(' ');
        writeAscii(attribute.getName().replace("[^a-zA-Z0-9-]", ""), ostream);

        if(attribute.getValue() != null)
        {
            ostream.write('=');
            ostream.write('"');

            if(attribute.getValue() instanceof TemplateExpressionText)
            {
                // If the attribute value contains expressions we need to resolve their values first
                writeUtf8(((TemplateExpressionText) attribute.getValue()).getContent(scope), ostream);
            }
            else
            {
                // No expressions, just render as it is.
                writeUtf8(attribute.getValue().getContent().replace("\"", "\\\""), ostream);
            }

            ostream.write('"');
        }
    }

    private void renderText (ReadableText text) throws IOException
    {
        // Like attributes, text nodes need to be resolved if expressions are present, or otherwise
        // rendered as it is.

        if(text instanceof TemplateExpressionText)
        {
            writeUtf8(((TemplateExpressionText) text).getContent(scope), ostream);
            return;
        }

        writeUtf8(text.getContent().replace("<", "&lt;").replace(">", "&gt;"), ostream);
    }

    private void renderSlotContent (ReadableElement slotElement) throws IOException
    {
        String slotName = slotElement.hasAttribute("name") ?
            slotElement.getAttribute("name").getValue().getContent() :
            null;

        if(slotName != null)
        {
            scope.getElementsForSlot(slotName)
                .forEach(CheckedConsumer.rethrowConsumer(this::renderElement));
        }
        else
        {
            scope.getUnassignedSlotNodes()
                .forEach(CheckedConsumer.rethrowConsumer(this::render));
        }
    }

    private void renderTemplate (ReadableElement tplElement) throws IOException
    {
        // This applies to <template> nodes _nested_ within a template declaration. Not to be confused with the
        // element definitions that also use the <template> tag.

        String is = tplElement.hasAttribute("is") ?
            tplElement.getAttribute("is").getValue().getContent() :
            null;

        // Any template may have an if clause. If it does, the template will only be rendered if the expression evaluates
        // as true.
        if(tplElement.hasAttribute("if"))
        {
            ReadableAttribute ifAttr = tplElement.getAttribute("if");

            if(ifAttr.getValue() instanceof TemplateExpressionText)
            {
                Expression exp = ((TemplateExpressionText)ifAttr.getValue()).getExpressionIfOnlySegment();

                if(exp != null)
                {
                    Boolean result = Boolean.valueOf(exp.resolve(scope).toString());

                    if(!result)
                    {
                        return;
                    }
                }
            }
        }

        if(is != null)
        {
            if(is.equals("foreach"))
            {
                renderTemplateForeach(tplElement);
                return;
            }
        }

        // If we reach the end, either there's no handler for this type of template, or it was a if-condition template
        // that evaluated to true. So we render all children.
        tplElement.childs()
            .forEach(CheckedConsumer.rethrowConsumer(this::render));
    }

    private void renderTemplateForeach (ReadableElement tplElement) throws IOException
    {
        // Attribute "data" must contain an array or Iterable providing the data we're about to iterate.
        ReadableAttribute data = tplElement.attribute("data")
                .orElseThrow(() -> new RenderingException("Template Foreach element must specify a data attribute"));

        // Attribute "as" defines the name of the current item when iterating. This is the "variable"'s name we pass to
        // the inner scope.
        String as = tplElement.attributeContent("as")
                .orElse("item");

        // We grab the template's body, which is the node we'll repeat for each item iterated.
        // Foreach templates only accept a single child element.
        ReadableElement repeatable = tplElement.childs()
                .filter(n -> n instanceof ReadableElement)
                .map(n -> (ReadableElement) n)
                .findFirst()
                .orElseThrow(() -> new RenderingException("Template Foreach doesn't have content"));

        if(!(data.getValue() instanceof TemplateExpressionText))
        {
            throw new RenderingException("Template Foreach's data attribute must be an expression that resolves an iterable");
        }

        Expression dataEx = ((TemplateExpressionText) data.getValue()).getExpressionIfOnlySegment();

        if(dataEx == null)
        {
            // If the data attribute were to contain more than just an expression, e.g. data="{{ = myList }}somethingElse",
            // the result would be a concatenation of the myList object with the "somethingElse" string, which would result
            // in another string.
            // This is unusable for this use-case, so we don't allow it here.
            throw new RenderingException("Template Foreach's data attribute must contain only an expression.");
        }

        Object result = dataEx.resolve(scope);
        Stream<Object> objects;

        if(result instanceof Iterable)
        {
            objects = StreamSupport.stream(((Iterable<Object>) result).spliterator(), false);
        } else if(result instanceof Object[])
        {
            objects = Arrays.stream((Object[]) result);
        } else
        {
            throw new RenderingException("Unable to iterate over data of type " + result.getClass().getName() + " provided in the " + data.getName() + " attribute. " + result);
        }

        objects.forEach(CheckedConsumer.rethrowConsumer((o) ->
        {
            // we're making the inner scope a child of the current scope, so we can access variables defined outside
            // the for-each block
            InternalScope newScope = new InternalScope(scope);

            // we add the current item as a variable into the inner scope
            newScope.put(as, o);

            newSubRenderer(repeatable, newScope);
        }));
    }
}
