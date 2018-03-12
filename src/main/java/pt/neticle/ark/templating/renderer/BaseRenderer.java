package pt.neticle.ark.templating.renderer;

import pt.neticle.ark.templating.structure.ReadableElement;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BaseRenderer
{
    protected void openTagBegin (ReadableElement el, OutputStream os) throws IOException
    {
        os.write('<');
        writeAscii(el.getTagName(), os);
    }

    protected void openTagEnd (ReadableElement el, OutputStream os) throws IOException
    {
        os.write('>');
    }

    protected void closeTag (ReadableElement el, OutputStream os) throws IOException
    {
        os.write('<');
        os.write('/');
        writeAscii(el.getTagName(), os);
        os.write('>');
    }

    protected void writeUtf8 (String str, OutputStream os) throws IOException
    {
        os.write(str.getBytes(StandardCharsets.UTF_8));
    }

    protected void writeAscii (String str, OutputStream os) throws IOException
    {
        os.write(str.getBytes(StandardCharsets.US_ASCII));
    }
}
