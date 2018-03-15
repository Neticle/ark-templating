package pt.neticle.ark.templating.renderer;

public interface Scope
{
    Scope getParent ();
    boolean available (String key);
    Object get (String key);
}
