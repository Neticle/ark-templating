package pt.neticle.ark.templating;

import org.xml.sax.SAXException;
import pt.neticle.ark.templating.parsing.TemplateParser;
import pt.neticle.ark.templating.renderer.Renderer;
import pt.neticle.ark.templating.renderer.Scope;
import pt.neticle.ark.templating.structure.ReadableElement;
import pt.neticle.ark.templating.structure.TemplateRootElement;
import pt.neticle.ark.templating.structure.expressions.ExpressionMatcher;
import pt.neticle.ark.templating.structure.functions.FunctionCatalog;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The main object of the ark-templating library. It contains all registered templates as well as
 * references to the necessary components that make up the engine, such as the parser, renderer,
 * and the expression matcher.
 *
 * {@link pt.neticle.ark.templating.TemplatingEngine.Initializer} can be used to configure and
 * initialize an instance of the engine.
 */
public class TemplatingEngine
{
    private final TemplateParser templateParser;
    private final ExpressionMatcher expressionMatcher;

    private final Map<String, ReadableElement> rootElementsRegistry;
    private final Map<String, Map<String,String>> rootElementsMetaData;

    public TemplatingEngine ()
    {
        this(new TemplateParser(), new ExpressionMatcher(new FunctionCatalog()));
    }

    public TemplatingEngine (TemplateParser templateParser, ExpressionMatcher expressionMatcher)
    {
        this.templateParser = templateParser;
        this.expressionMatcher = expressionMatcher;

        this.rootElementsRegistry = new HashMap<>();
        this.rootElementsMetaData = new HashMap<>();
    }

    /**
     * Attempts to parse and register a new template for a user-defined custom element type.
     *
     * @param is An inputstream containing the template as valid XML and encapsulated in a
     * template node
     *
     * @return The tag-name of the newly defined custom element type.
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public String registerTemplate (InputStream is) throws IOException, SAXException, ParserConfigurationException
    {
        TemplateRootElement rootElement = templateParser.parse(new TemplateRootElement(this), is);

        if(rootElement != null)
        {
            rootElementsRegistry.put(rootElement.getTemplateName(), rootElement);
            rootElementsMetaData.put(rootElement.getTemplateName(), rootElement.getMetaData());

            return rootElement.getTemplateName();
        }

        return null;
    }

    /**
     * Gets the expression matcher instance being used by this engine instance.
     * @return
     */
    public ExpressionMatcher getExpressionMatcher ()
    {
        return expressionMatcher;
    }

    /**
     * Gets a list of all registered custom element types.
     * @return
     */
    public Set<String> getRegisteredTemplateNames ()
    {
        return rootElementsRegistry.keySet();
    }

    /**
     * Gets all defined meta-data for a given template as a map of strings.
     * @param qualifiedName
     * @return
     */
    public Map<String,String> getTemplateMetaData (String qualifiedName)
    {
        return rootElementsMetaData.get(qualifiedName);
    }

    /**
     * Gets the template for the specified element type, if registered.
     * @param qualifiedName
     * @return
     */
    public ReadableElement getTemplate (String qualifiedName)
    {
        return rootElementsRegistry.get(qualifiedName);
    }

    /**
     * Checks if there is a template for the specified element type.
     * @param qualifiedName
     * @return
     */
    public boolean hasTemplate (String qualifiedName)
    {
        return rootElementsRegistry.containsKey(qualifiedName);
    }

    /**
     * Gets the template for the specified element type, if registered, as an optional.
     *
     * @param qualifiedName
     * @return
     */
    public Optional<ReadableElement> template (String qualifiedName)
    {
        return Optional.ofNullable(rootElementsRegistry.get(qualifiedName));
    }

    /**
     * Renders the specified template into the provided outputstream, with the given scope.
     *
     * @param root The template to render
     * @param scope The scope containing any data to be passed to the template
     * @param os The outputstream in which the result will be written to
     *
     * @throws IOException
     */
    public void render (ReadableElement root, Scope scope, OutputStream os) throws IOException
    {
        new Renderer(this, root, scope, os);
    }

    /**
     * Renders the specified template into the provided appendable object, with the given scope.
     *
     * @param root  The template to render
     * @param scope The scope containing any data to be passed to the template
     * @param appendable The appendable object in which the result will be written to
     * @throws IOException
     */
    public void render (ReadableElement root, Scope scope, Appendable appendable) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        render(root, scope, baos);

        appendable.append(new String(baos.toByteArray()));
    }

    /**
     * Creates a new initializer for the TemplatingEngine
     * @return
     */
    public static Initializer initializer ()
    {
        return new Initializer();
    }

    /**
     * An utility class to aid the configuration and initialization of the TemplatingEngine.
     */
    public static class Initializer
    {
        private List<Path> searchDirectories;

        Initializer ()
        {
            this.searchDirectories = new ArrayList<>();
        }

        /**
         * Adds a new search directory for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * @param path
         * @return
         */
        public Initializer withSearchDirectory(Path path)
        {
            searchDirectories.add(path);
            return this;
        }

        /**
         * Adds multiple search directories for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * @param paths
         * @return
         */
        public Initializer withSearchDirectories(Path... paths)
        {
            for(Path path : paths) withSearchDirectory(path);
            return this;
        }

        /**
         * Builds a TemplatingEngine instance and adds any templates found in the specified
         * search directories.
         *
         * @return
         * @throws ParserConfigurationException
         * @throws SAXException
         * @throws IOException
         */
        public TemplatingEngine build () throws ParserConfigurationException, SAXException, IOException
        {
            TemplatingEngine engine = new TemplatingEngine();

            for(Path path : searchDirectories)
            {
                handleFileObject(path, engine);
            }

            return engine;
        }

        private void handleFileObject (Path file, TemplatingEngine engine) throws IOException, ParserConfigurationException, SAXException
        {
            if(!Files.exists(file))
            {
                throw new FileNotFoundException(file.toString());
            }

            if(Files.isDirectory(file))
            {
                for(Path subdir : Files.newDirectoryStream(file, (f) -> Files.isDirectory(f)))
                {
                    handleFileObject(subdir, engine);
                }

                for(Path tplFile : Files.newDirectoryStream(file, "*.html"))
                {
                    handleFileObject(tplFile, engine);
                }

                return;
            }

            engine.registerTemplate(Files.newInputStream(file));
        }
    }
}
