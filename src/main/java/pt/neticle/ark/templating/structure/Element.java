package pt.neticle.ark.templating.structure;

public interface Element extends ReadableElement
{
    void setTagName (String qualifiedName);

    void setAttribute (String qualifiedName, ReadableText text);

    void removeAttribute (String qualifiedName);

    void addChild (ReadableElement child);

    void addText (ReadableText text);
}
