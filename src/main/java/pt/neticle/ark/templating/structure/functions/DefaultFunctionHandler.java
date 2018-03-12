package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

/**
 * The default function handler contains an assortment of utility methods to deal
 * with function arguments.
 *
 * It is merely an utility class, if you do not wish to make use of it you can
 * simply implement the {@link pt.neticle.ark.templating.structure.functions.FunctionHandler}
 * interface.
 *
 * @param <T>
 */
public abstract class DefaultFunctionHandler<T> implements FunctionHandler<T>
{
    public abstract String getName ();

    /**
     * Asserts that the specified argument value is not null and returns it's value.
     *
     * @param args The list of arguments
     * @param index The index of the argument we want to test
     * @param type The desired type of the argument
     * @param <A>
     *
     * @throws RenderingException Throws an exception if argument is null or not of the desired type.
     *
     * @return The argument's value as the expected type if possible.
     */
    protected <A> A nonNullArgument (Object[] args, int index, Class<A> type)
    {
        Object arg = index < args.length ? args[index] : null;

        if(arg == null)
        {
            throw new RenderingException("Argument " + index + " of " + getName() + " function must not be null");
        }

        if(!type.isAssignableFrom(arg.getClass()))
        {
            throw new RenderingException("Argument " + index + " of " + getName() + " function must be of type " + type.getName() + ". " + arg.getClass().getName() + " given.");
        }

        return (A) arg;
    }

    /**
     * Asserts that the specified argument is of the given type, but does not null-check.
     *
     * @param args The list of arguments
     * @param index The index of the argument we want to test
     * @param type The desired type of the argument
     * @param <A>
     *
     * @throws RenderingException Throws an exception if argument is not of the desired type.
     *
     * @return The argument's value as the expected type if possible.
     */
    protected <A> A argument (Object[] args, int index, Class<A> type)
    {
        return argument(args, index, type, true);
    }

    /**
     * Attempts to fetch the specified argument, casted to the specified type.
     *
     * Only set typeCheck as false if you've made sure a conversion is possible before
     * calling this method.
     *
     * @param args The list of arguments
     * @param index The index of the argument we want to test
     * @param type The desired type of the argument
     * @param typeCheck Whether or not to type check the argument value
     * @param <A>
     *
     * @throws RenderingException If typeCheck is true, throws an exception if the argument is
     * not of the desired type.
     *
     * @return The argument's value as the expected type if possible.
     */
    protected <A> A argument (Object[] args, int index, Class<A> type, boolean typeCheck)
    {
        Object arg = index < args.length ? args[index] : null;

        if(typeCheck && arg != null && !type.isAssignableFrom(arg.getClass()))
        {
            throw new RenderingException("Argument " + index + " of " + getName() + " function must be of type " + type.getName() + ". " + arg.getClass().getName() + " given.");
        }

        return (A)arg;
    }

    /**
     * Ensures the provided arguments are all non-null and are the same as in the specified list, both in
     * quantity and in type.
     *
     * @param args The list of provided arguments
     * @param types The list of desired types
     * @throws RenderingException Throws an exception if the number of arguments doesn't match the expected number,
     * if one of the arguments doesn't match the expected type, and if one of the arguments is null.
     */
    protected void ensureSignatureArgs (Object[] args, Class<?>... types)
    {
        ensureSignatureArgs(args, true, types);
    }

    /**
     * Ensures the provided arguments are the same as the specified list, both in quantity and in type.
     *
     * @param args The list of provided arguments
     * @param allowNull Whether null values are allowed or not
     * @param types The list of desired types
     *
     * @throws RenderingException Throws an exception if the number of arguments doesn't match the expected number,
     * if one of the arguments doesn't match the expected type, and - if allowNull is false - if one of the arguments
     * is null.
     */
    protected void ensureSignatureArgs (Object[] args, boolean allowNull, Class<?>... types)
    {
        if(args.length != types.length)
        {
            throw new RenderingException("Wrong number of arguments provided for " + getName() + " function. " + types.length + " arguments required.");
        }

        for(int i = 0; i < types.length; i++)
        {
            if(args[i] == null)
            {
                if(!allowNull)
                {
                    throw new RenderingException("Argument " + i + " of " + getName() + " function must not be null");
                }

                continue;
            }

            if(!types[i].isAssignableFrom(args[i].getClass()))
            {
                throw new RenderingException("Argument " + i + " of " + getName() + " function must be of type " + types[i].getName() + ". " + args[i].getClass().getName() + " given.");
            }
        }
    }

    /**
     * Checks if the provided arguments are all non-null and the same as in the specified list, both in
     * quantity and in type.
     *
     * @param args The list of provided arguments
     * @param types The list of desired types
     * @return False if the number of arguments doesn't match the expected number, if one of the arguments doesn't
     * match the expected type, or if one of the arguments is null.
     */
    protected boolean matchSignatureArgs (Object[] args, Class<?>... types)
    {
        return matchSignatureArgs(args, true, types);
    }

    /**
     * Checks if the provided arguments are the same as the specified list, both in quantity and in type.
     *
     * @param args The list of provided arguments
     * @param allowNull Whether null values are allowed or not
     * @param types The list of desired types
     *
     * @return False if the number of arguments doesn't match the expected number, if one of the arguments doesn't
     * match the expected type, or - if allowNull is false - if one of the arguments is null.
     */
    protected boolean matchSignatureArgs (Object[] args, boolean allowNull, Class<?>... types)
    {
        if(args.length != types.length)
        {
            return false;
        }

        for(int i = 0; i < types.length; i++)
        {
            if(args[i] == null)
            {
                if(!allowNull)
                {
                    return false;
                }

                continue;
            }

            if(!types[i].isAssignableFrom(args[i].getClass()))
            {
                return false;
            }
        }

        return true;
    }
}
