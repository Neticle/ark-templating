package pt.neticle.ark.templating.exception;

public class SanityException extends Exception
{
    public SanityException (String message)
    {
        super(message);
    }

    public SanityException (String message, Throwable cause)
    {
        super(message, cause);
    }
}
