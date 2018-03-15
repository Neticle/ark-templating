package pt.neticle.ark.templating.functional;

import java.util.function.Function;

@FunctionalInterface
public interface CheckedFunction<T, R, X extends Throwable>
{
    R apply(T a) throws X;

    static <T, R, X extends Throwable> Function<T, R> rethrow (CheckedFunction<T, R, X> fn) throws X
    {
        return t ->
        {
            try
            {
                return fn.apply(t);
            } catch(Throwable exception)
            {
                throwAsUnchecked(exception);
            }

            return null;
        };
    }

    static <E extends Throwable> void throwAsUnchecked (Throwable exception) throws E
    {
        throw (E) exception;
    }
}
