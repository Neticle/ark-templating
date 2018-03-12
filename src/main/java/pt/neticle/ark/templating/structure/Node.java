package pt.neticle.ark.templating.structure;

import java.util.Optional;

public interface Node
{
    enum Types
    {
        TEXT,
        ELEMENT,
        ATTRIBUTE
    }

    Types getType ();

    default Optional<ReadableElement> attemptReadableElement ()
    {
        if(this instanceof ReadableElement)
        {
            return Optional.of((ReadableElement) this);
        }

        return Optional.empty();
    }

    default ReadableElement asReadableElement ()
    {
        if(this instanceof ReadableElement)
        {
            return (ReadableElement) this;
        }

        return null;
    }

    default Optional<ReadableAttribute> attemptReadableAttribute ()
    {
        if(this instanceof ReadableAttribute)
        {
            return Optional.of((ReadableAttribute) this);
        }

        return Optional.empty();
    }

    default ReadableAttribute asReadableAttribute ()
    {
        if(this instanceof ReadableAttribute)
        {
            return (ReadableAttribute) this;
        }

        return null;
    }

    default Optional<ReadableText> attemptReadableText ()
    {
        if(this instanceof ReadableText)
        {
            return Optional.of((ReadableText) this);
        }

        return Optional.empty();
    }

    default ReadableText asReadableText ()
    {
        if(this instanceof ReadableText)
        {
            return (ReadableText) this;
        }

        return null;
    }
}
