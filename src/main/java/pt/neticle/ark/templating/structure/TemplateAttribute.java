package pt.neticle.ark.templating.structure;

public class TemplateAttribute extends TemplateNode implements Attribute
{
    private String qualifiedName;
    private ReadableText value;

    public TemplateAttribute (TemplateRootElement templateRootElement, String qualifiedName, ReadableText value)
    {
        super(templateRootElement);
        this.qualifiedName = qualifiedName;
        this.value = value;
    }

    @Override
    public void setName (String qualifiedName)
    {
        this.qualifiedName = qualifiedName;
    }

    @Override
    public void setValue (ReadableText text)
    {
        this.value = text;
    }

    @Override
    public String getName ()
    {
        return qualifiedName;
    }

    @Override
    public ReadableText getValue ()
    {
        return value;
    }

    @Override
    public Types getType ()
    {
        return Types.ATTRIBUTE;
    }
}
