package pt.neticle.ark.templating.exception;

import java.nio.file.Path;

public class DiscoveryLoaderException extends RuntimeException
{
    public DiscoveryLoaderException (Path file, Throwable cause)
    {
        super("Failed loading template file: " + file.toString(), cause);
    }
}
