package pt.neticle.ark.templating.structure.expressions;

import pt.neticle.ark.templating.renderer.Scope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Matches a reference expression such as [var] or [var1].[var2]
 * Variable names may contain any alphanumeric characters and underscores. Dots shall be used
 * as separators for referencing members of the previous reference.
 *
 * E.g.
 * "foo.bar" references the "bar" member of the "foo" object present in the current scope.
 * If "foo" is a Map, the reference will be resolved by calling foo.get("bar").
 * Otherwise, it's assumed "foo" is an Object and the reference will be resolved by attempting
 * to call foo.getBar().
 */
public class ObjectReferenceExpression implements Expression
{
    private static final Pattern matcherPt = Pattern.compile("(^\\w([\\w.]*)?\\w$)|(^(\\w+)$)");
    private final String[] segments;
    private final int hashCode;

    private static Map<String, Method> mappedMethods = new ConcurrentHashMap<>();

    ObjectReferenceExpression (ExpressionMatcher matcher, String text)
    {
        segments = Arrays.stream(text.split("\\."))
            .filter((s) -> s.length() > 0)
            .toArray(String[]::new);

        hashCode = Arrays.hashCode(segments);
    }

    public Object resolve (Scope scope)
    {
        if(segments.length == 0)
        {
            return null;
        }

        Object current = scope.get(segments[0]);

        for(int i = 1; i < segments.length && current != null; i++)
        {
            if(current instanceof Map)
            {
                current = ((Map) current).get(segments[i]);
            }

            else if(current instanceof Map.Entry)
            {
                // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4207233
                if(segments[i].equals("key"))
                {
                    current =  ((Map.Entry)current).getKey();
                }
                else if(segments[i].equals("value"))
                {
                    current = ((Map.Entry) current).getValue();
                }
                else
                {
                    current = null;
                }
            }
            else
            {
                final Object finalCurrent = current;
                final int finalI = i;
                Method getter = mappedMethods.computeIfAbsent(current.getClass().getName()+segments[i], (k) ->
                {
                    try
                    {
                        return finalCurrent.getClass().getMethod("get" + segments[finalI].substring(0, 1).toUpperCase() + segments[finalI].substring(1));
                    } catch(NoSuchMethodException e)
                    {
                        return null;
                    }
                });

                if(getter != null)
                {
                    try
                    {
                        current = getter.invoke(current);
                    } catch(IllegalAccessException e)
                    {
                        current = null;
                    } catch(InvocationTargetException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    current = null;
                }
            }
        }

        return current;
    }

    @Override
    public Function<Scope, Object> getResolver ()
    {
        return this::resolve;
    }

    public String[] getSegments ()
    {
        return segments;
    }

    static boolean matches (String text)
    {
        return matcherPt.matcher(text).find();
    }

    @Override
    public boolean equals (Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        ObjectReferenceExpression that = (ObjectReferenceExpression) o;

        return that.hashCode == hashCode && Arrays.equals(segments, that.segments);
    }

    @Override
    public int hashCode ()
    {
        return hashCode;
    }
}
