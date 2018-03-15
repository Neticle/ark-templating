package pt.neticle.ark.templating.processing;

/**
 * Instructs the renderer to output raw text content contained in here.
 */
public class RawOutputInstruction extends Instruction
{
    private final StringBuilder sb;

    RawOutputInstruction ()
    {
        super(Type.RAW_OUTPUT);
        this.sb = new StringBuilder();
    }

    public void append (String str)
    {
        sb.append(str);
    }

    public String getContent ()
    {
        return sb.toString();
    }

    @Override
    public String toString ()
    {
        return super.toString() + "'"+getContent().replaceAll("\\n", " ").trim().replaceAll(" +", " ")+"']";
    }
}
