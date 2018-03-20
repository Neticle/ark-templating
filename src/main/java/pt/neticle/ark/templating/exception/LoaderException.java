package pt.neticle.ark.templating.exception;

import java.nio.file.Path;

public class LoaderException extends Exception
{
    public LoaderException (Path file, Throwable cause)
    {
        super("Failed loading template file: " + file.toString(), cause);
    }

    public LoaderException (Path file, ParsingException cause)
    {
        super("Failed parsing template file: " + file.toString() + ":" +
            cause.getLine() + "," + cause.getColumn() + ", " +
            cause.getMessage(), cause);
    }
}
