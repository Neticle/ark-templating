package pt.neticle.ark.templating.processing;

/**
 * Instructs the renderer to expand slotted content.
 */
public class ExpandSlotInstruction extends Instruction
{
    private final String slotName;

    ExpandSlotInstruction (String slotName)
    {
        super(Type.EXPAND_SLOT);

        this.slotName = slotName;
    }

    public String getSlotName ()
    {
        return slotName;
    }

    public boolean isUnassignedSlot ()
    {
        return slotName == null;
    }

    @Override
    public String toString ()
    {
        return super.toString() + slotName + "]";
    }

}
