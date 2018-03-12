package pt.neticle.ark.templating.exception;

public class RenderingException extends RuntimeException
{
    public RenderingException ()
    {
    }

    public RenderingException (String message)
    {
        super(message);
    }

    public RenderingException (String message, Throwable cause)
    {
        super(message, cause);
    }

    public RenderingException (Throwable cause)
    {
        super(cause);
    }
}
