package pt.neticle.ark.templating.exception;

public class ParsingException extends Exception
{
    private final int offset;
    private final int line;
    private final int column;

    public ParsingException (String message, int offset, int line, int column)
    {
        super(message);

        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    public ParsingException (String message, Throwable cause, int offset, int line, int column)
    {
        super(message, cause);

        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    public int getOffset ()
    {
        return offset;
    }

    public int getLine ()
    {
        return line;
    }

    public int getColumn ()
    {
        return column;
    }
}
