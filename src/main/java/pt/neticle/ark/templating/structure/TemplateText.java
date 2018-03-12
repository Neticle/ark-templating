package pt.neticle.ark.templating.structure;

public class TemplateText extends TemplateNode implements Text
{
    private String content;

    public TemplateText (TemplateRootElement rootElement, String content)
    {
        super(rootElement);
        setContent(content);
    }

    @Override
    public void setContent (String content)
    {
       this.content = content;
    }

    @Override
    public String getContent ()
    {
        return content;
    }

    @Override
    public Types getType ()
    {
        return Types.TEXT;
    }
}
