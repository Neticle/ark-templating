package pt.neticle.ark.templating.structure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface ReadableElement extends Node
{
    String getTagName ();

    ReadableAttribute getAttribute (String qualifiedName);

    Collection<? extends ReadableAttribute> getAttributes ();

    default Optional<ReadableAttribute> attribute (String qualifiedName)
    {
        if(!hasAttribute(qualifiedName))
        {
            return Optional.empty();
        }

        return Optional.of(getAttribute(qualifiedName));
    }

    default Optional<String> attributeContent (String qualifiedName)
    {
        if(!hasAttribute(qualifiedName))
        {
            return Optional.empty();
        }

        return Optional.ofNullable(getAttribute(qualifiedName).getValue().getContent());
    }

    default Stream<? extends ReadableAttribute> attributes ()
    {
        return getAttributes().stream();
    }

    boolean hasAttribute (String qualifiedName);

    List<Node> getChilds ();

    default Stream<Node> childs ()
    {
        return getChilds().stream();
    }

    default <T> Stream<T> childs(Class<T> childType)
    {
        return childs()
            .filter((n) -> childType.isAssignableFrom(n.getClass()))
            .map((n) -> (T) n);
    }

    //Element getWritableCopy ();
}
