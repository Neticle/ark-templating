package pt.neticle.ark.templating.structure;

public interface Attribute extends ReadableAttribute
{
    void setName (String qualifiedName);

    void setValue (ReadableText text);
}
