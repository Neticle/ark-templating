package pt.neticle.ark.templating.renderer;

public final class ForeachIterationInfo
{
    private final int index;
    private final boolean isFirst;
    private final boolean isLast;

    ForeachIterationInfo (int index, boolean isFirst, boolean isLast)
    {
        this.index = index;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }

    public int getIndex ()
    {
        return index;
    }

    public boolean getIsFirst ()
    {
        return isFirst;
    }

    public boolean getIsLast ()
    {
        return isLast;
    }

    public boolean getIndexIsOdd ()
    {
        return index % 2 != 0;
    }

    public boolean getIndexIsEven ()
    {
        return index % 2 == 0;
    }
}
