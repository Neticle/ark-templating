package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * String Implode(String delimiter, String[] segments)
 *
 * Joins the given list of segments into one string, with the specified delimiter.
 */
public class ImplodeFunction extends DefaultFunctionHandler<String>
{
    @Override
    public String getName ()
    {
        return "Implode";
    }

    @Override
    public String apply (Object[] args) throws RenderingException
    {
        String delimiter = nonNullArgument(args, 0, String.class);

        if(args.length == 2 && args[0] instanceof String && args[1] instanceof Iterable)
        {
            return implode(delimiter, ((Iterable<Object>) args[1]).spliterator());
        }
        else if(args.length == 2 && args[0] instanceof String && args[1].getClass().isArray())
        {
            return implode(delimiter, (Object[]) args[1]);
        }

        return implode(delimiter, Arrays.copyOfRange(args, 1, args.length));
    }

    private String implode (String delimiter, Object[] items)
    {
        return Arrays.stream(items)
            .filter((a) -> a != null)
            .map((a) -> a.toString())
            .collect(Collectors.joining(delimiter));
    }

    private String implode (String delimiter, Spliterator<Object> spliterator)
    {
        return StreamSupport.stream(spliterator, false)
            .filter((a) -> a != null)
            .map((a) -> a.toString())
            .collect(Collectors.joining(delimiter));
    }
}
