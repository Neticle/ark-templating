package pt.neticle.ark.templating.parsing;

import java.util.regex.Pattern;

class TemplateParserContext
{
    private boolean stateTagOpen = false;
    private boolean stateInAttrValue = false;
    private boolean stateEscapeSeq = false;

    private boolean stateTextOnly = false;
    private Pattern textOnlyUntil = null;
    private String textOnlyUntilTag = null;

    private StringBuilder builder = new StringBuilder();

    public void appendToBuffer (char c)
    {
        builder.append(c);
    }

    public String getBufferString ()
    {
        return builder.toString();
    }

    public void resetBuffer ()
    {
        builder = new StringBuilder();
    }

    public boolean isStateTagOpen ()
    {
        return stateTagOpen;
    }

    public boolean isStateInAttrValue ()
    {
        return stateInAttrValue;
    }

    public boolean isStateEscapeSeq ()
    {
        return stateEscapeSeq;
    }

    public boolean isStateTextOnly ()
    {
        return stateTextOnly;
    }

    public Pattern getTextOnlyUntil ()
    {
        return textOnlyUntil;
    }

    public String getTextOnlyUntilTag ()
    {
        return textOnlyUntilTag;
    }

    public void setStateTagOpen (boolean stateTagOpen)
    {
        this.stateTagOpen = stateTagOpen;
    }

    public void setStateInAttrValue (boolean stateInAttrValue)
    {
        this.stateInAttrValue = stateInAttrValue;
    }

    public void setStateEscapeSeq (boolean stateEscapeSeq)
    {
        this.stateEscapeSeq = stateEscapeSeq;
    }

    public void disableTextOnlyState ()
    {
        stateTextOnly = false;
        textOnlyUntil = null;
        textOnlyUntilTag = null;
    }

    public void enableTextOnlyState (Pattern closeTagPattern, String tagName)
    {
        stateTextOnly = true;
        textOnlyUntil = closeTagPattern;
        textOnlyUntilTag = tagName;
    }
}
