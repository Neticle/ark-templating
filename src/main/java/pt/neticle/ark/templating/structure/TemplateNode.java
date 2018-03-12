package pt.neticle.ark.templating.structure;

public class TemplateNode
{
    private final TemplateRootElement templateRootElement;

    public TemplateNode (TemplateRootElement templateRootElement)
    {
        this.templateRootElement = templateRootElement;
    }

    public TemplateRootElement getTemplateRootElement ()
    {
        return templateRootElement;
    }
}
