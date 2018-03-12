package pt.neticle.ark.templating.functional;

import java.io.IOException;
import java.util.function.Consumer;

public interface CheckedConsumer<T, X extends Throwable>
{
    void accept (T t) throws X, IOException;

    static <T,X extends Throwable> CheckedConsumer<T,X> of (CheckedConsumer<T,X> methodReference)
    {
        return methodReference;
    }

    static <T, X extends Exception> Consumer<T> rethrowConsumer (CheckedConsumer<T, X> consumer) throws X
    {
        return t ->
        {
            try
            {
                consumer.accept(t);
            } catch(Exception exception)
            {
                throwAsUnchecked(exception);
            }
        };
    }

    static <E extends Throwable> void throwAsUnchecked (Exception exception) throws E
    {
        throw (E) exception;
    }
}