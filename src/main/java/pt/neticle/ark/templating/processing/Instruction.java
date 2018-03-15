package pt.neticle.ark.templating.processing;

/**
 * The base type for all instructions.
 * Each instruction may have a next instruction, but is not required to.
 */
public abstract class Instruction
{
    public enum Type
    {
        RAW_OUTPUT,
        EXPRESSION_RESULT_OUTPUT,
        EXPAND_SLOT,
        EXPAND_INNER_TEMPLATE,
        EXPAND_TEMPLATE
    };

    private final Type type;

    private Instruction next;

    Instruction (Type type)
    {
        this.type = type;
    }

    public Type getType ()
    {
        return type;
    }

    public boolean is (Type t)
    {
        return type == t;
    }

    public Instruction getNext ()
    {
        return next;
    }

    <T extends Instruction> T setNext (T next)
    {
        return (T) (this.next = next);
    }

    @Override
    public String toString ()
    {
        return "[INST:" + type.toString() + ":";
    }
}
