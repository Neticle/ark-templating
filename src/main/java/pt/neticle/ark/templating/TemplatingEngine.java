package pt.neticle.ark.templating;

import pt.neticle.ark.templating.exception.LoaderException;
import pt.neticle.ark.templating.exception.ParsingException;
import pt.neticle.ark.templating.functional.CheckedFunction;
import pt.neticle.ark.templating.parsing.DefaultTemplateParser;
import pt.neticle.ark.templating.parsing.TemplateParser;
import pt.neticle.ark.templating.renderer.InternalScope;
import pt.neticle.ark.templating.renderer.PreprocessedRenderer;
import pt.neticle.ark.templating.renderer.Scope;
import pt.neticle.ark.templating.structure.ReadableElement;
import pt.neticle.ark.templating.structure.TemplateRootElement;
import pt.neticle.ark.templating.structure.expressions.ExpressionMatcher;
import pt.neticle.ark.templating.structure.functions.DefaultFunctionHandler;
import pt.neticle.ark.templating.structure.functions.FunctionCatalog;
import pt.neticle.ark.templating.structure.functions.FunctionHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * The template parser in use.
     */
    private final TemplateParser templateParser;

    /**
     * The expression matcher in use.
     */
    private final ExpressionMatcher expressionMatcher;

    /**
     * Key: Template's qualified name
     * Value: Template's root element
     */
    private final Map<String, TemplateRootElement> rootElementsRegistry;

    /**
     * Key: Template's qualified name
     * Value: A k-v map of meta-data passed from the template's declaration. Everything as a string.
     */
    private final Map<String, Map<String,String>> rootElementsMetaData;

    /**
     * Key: Template's qualified name
     * Value: Timestamp in milliseconds of the template's registering time (not pre-processing time!)
     */
    private final Map<String, Long> registryTimestamps;

    /**
     * Last time the templates were pre-processed, as a milliseconds timestamp.
     */
    private long lastPreprocessingRun = 0;

    public TemplatingEngine ()
    {
        this(new DefaultTemplateParser(), new ExpressionMatcher(new FunctionCatalog()));
    }

    public TemplatingEngine (TemplateParser templateParser, ExpressionMatcher expressionMatcher)
    {
        this.templateParser = templateParser;
        this.expressionMatcher = expressionMatcher;

        this.rootElementsRegistry = new HashMap<>();
        this.rootElementsMetaData = new HashMap<>();
        this.registryTimestamps = new HashMap<>();
    }

    /**
     * Pre-processes the templates that changed since the last pre-processing run.
     *
     * You don't need to call this method, it will be automatically invoked the first time you
     * attempt to get a template from the engine. After that, it will be called everytime you
     * register a template, either replacing or creating.
     *
     * You can however use this method if you want to control when the pre-processing occurs.
     * If you call it just after registering your initial templates, then it wont be lazily invoked
     * on the first getTemplate() call.
     *
     * When called, this method filters out templates that haven't changed since the last pre-processing
     * run, so you don't have to worry about unnecessary processing occurring here.
     */
    public void preprocessChanges ()
    {
        final long previousRun = lastPreprocessingRun;
        lastPreprocessingRun = System.currentTimeMillis();

        registryTimestamps.entrySet().stream()
            .filter(e -> e.getValue() > previousRun)
            .map(e -> rootElementsRegistry.get(e.getKey()))
            .filter(e -> e != null)
            .forEach(e -> e.prepare());
    }

    /**
     * Dumps the instruction set for the given template to System.out
     *
     * This method is mostly intended for debugging and inspection, there are no
     * uses for it in a production system.
     *
     * @param qualifiedName
     */
    public void dumpTemplateInstructionTree (String qualifiedName)
    {
        dumpTemplateInstructionTree(qualifiedName, System.out);
    }

    /**
     * Dumps the instruction set for the given template to the specified output.
     *
     * This method is mostly intended for debugging and inspection, there are no
     * uses for it in a production system.
     *
     * @param qualifiedName
     * @param out
     */
    public void dumpTemplateInstructionTree (String qualifiedName, PrintStream out)
    {
        TemplateRootElement el = (TemplateRootElement) getTemplate(qualifiedName);

        out.println("- " + qualifiedName + " --------------------");

        if(el == null)
        {
            out.println("No template found named '" + qualifiedName + "'");
            out.println("-----------------------");
            return;
        }

        el.getInstructionSet().dump(out);

        out.println("-----------------------");
    }

    /**
     * Attempts to parse and register a new template for a user-defined custom element type.
     *
     * @param is An inputstream containing the template as valid XML and encapsulated in a
     * template node
     *
     * @return The tag-name of the newly defined custom element type.
     *
     * @throws IOException Thrown for any errors while reading from the provided stream
     * @throws ParsingException Thrown for any parser errors while parsing the provided declaration
     */
    public String registerTemplate (InputStream is) throws IOException, ParsingException
    {
        TemplateRootElement rootElement = templateParser.parse(new TemplateRootElement(this), is);

        if(rootElement != null)
        {
            rootElementsRegistry.compute(rootElement.getTemplateName(), (k, v) -> rootElement);
            rootElementsMetaData.compute(rootElement.getTemplateName(), (k, v) -> rootElement.getMetaData());

            registryTimestamps.compute(rootElement.getTemplateName(), (k, v) -> System.currentTimeMillis());

            // We don't pre-process initially because templates may depend on each other, so we'll load everything and
            // once the user calls getTemplate for the first time, the initial pre-processing run will be executed.
            // After that, we always pre-process on new changes.
            //
            // Alternative to this would be to make the user responsible for registering templates in the correct order
            // regarding dependencies, but we don't want to create that extra hassle.
            if(lastPreprocessingRun > 0)
            {
                preprocessChanges();
            }

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
        if(lastPreprocessingRun == 0)
        {
            preprocessChanges();
        }

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
        return Optional.ofNullable(getTemplate(qualifiedName));
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
        new PreprocessedRenderer(this, ((TemplateRootElement) root).getInstructionSet(), new InternalScope(scope), os, Collections.emptyMap());
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
        private final Map<Path, Boolean> searchDirectories;
        private final Map<FileSystem, WatchService> watchServices;
        private final Map<WatchKey, Path> watchKeyPaths;
        private final ExpressionMatcher expressionMatcher;
        private final FunctionCatalog functionCatalog;

        Initializer ()
        {
            searchDirectories = new HashMap<>();
            watchServices = new HashMap<>();
            watchKeyPaths = new HashMap<>();
            expressionMatcher = new ExpressionMatcher(functionCatalog = new FunctionCatalog());
        }

        /**
         * Registers a function handler with the specified name.
         *
         * @param name The name of the function to register
         * @param handler A handler to be called when the function is invoked
         * @return
         */
        public Initializer withFunction (String name, FunctionHandler handler)
        {
            functionCatalog.registerHandler(name, handler);
            return this;
        }

        /**
         * Registers a function handler given a derivative of DefaultFunctionHandler.
         *
         * @param handler
         * @return
         */
        public Initializer withFunction (DefaultFunctionHandler handler)
        {
            functionCatalog.registerHandler(handler);
            return this;
        }

        /**
         * Adds a new search directory for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * @param path
         *
         * @param watchDirectory If true, a filesystem watcher will be created and
         * for any changes detected, the files will be reloaded.
         *
         * @return
         */
        public Initializer withSearchDirectory(Path path, boolean watchDirectory)
        {
            searchDirectories.put(path, watchDirectory);
            return this;
        }

        /**
         * Adds a new search directory for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * The templates will be loaded once. There will be no change-watchers
         * created from this method.
         *
         * @param path
         * @return
         */
        public Initializer withSearchDirectory (Path path)
        {
            return withSearchDirectory(path, false);
        }

        /**
         * Adds multiple search directories for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * @param watchDirectory If true, a filesystem watcher will be created and
         * for any changes detected, the files will be reloaded.
         *
         * @param paths
         *
         * @return
         */
        public Initializer withSearchDirectories(boolean watchDirectory, Path... paths)
        {
            for(Path path : paths) withSearchDirectory(path, watchDirectory);
            return this;
        }

        /**
         * Adds multiple search directories for template discovery.
         *
         * Each directory will be searched recursively for any files with the
         * extension .html
         *
         * The templates will be loaded once. There will be no change-watchers
         * created from this method.
         *
         * @param paths
         * @return
         */
        public Initializer withSearchDirectories (Path... paths)
        {
            return withSearchDirectories(false, paths);
        }

        /**
         * Builds a TemplatingEngine instance and adds any templates found in the specified
         * search directories.
         *
         * @return
         * @throws IOException
         */
        public TemplatingEngine build () throws IOException, LoaderException
        {
            TemplatingEngine engine = new TemplatingEngine(new DefaultTemplateParser(), expressionMatcher);

            for(Map.Entry<Path, Boolean> entry : searchDirectories.entrySet())
            {
                handleFileObject(entry.getKey(), engine, entry.getValue());
            }

            engine.preprocessChanges();

            ExecutorService executor = Executors.newCachedThreadPool();
            for(WatchService s : watchServices.values())
            {
                executor.submit(() -> handleWatchService(s, engine));
            }
            executor.shutdown();

            return engine;
        }

        private WatchService getWatchService (FileSystem fs) throws IOException
        {
            return watchServices.computeIfAbsent(fs, CheckedFunction.rethrow((_fs) -> _fs.newWatchService()));
        }

        private void handleFileObject (Path file, TemplatingEngine engine, boolean watch) throws IOException, LoaderException
        {
            if(!Files.exists(file))
            {
                throw new FileNotFoundException(file.toString());
            }

            if(Files.isDirectory(file))
            {
                if(watch)
                {
                    WatchKey wk = file.register(getWatchService(file.getFileSystem()),
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                    watchKeyPaths.put(wk, file);
                }

                for(Path subdir : Files.newDirectoryStream(file, (f) -> Files.isDirectory(f)))
                {
                    handleFileObject(subdir, engine, watch);
                }

                for(Path tplFile : Files.newDirectoryStream(file, "*.html"))
                {
                    handleFileObject(tplFile, engine, false);
                }

                return;
            }

            try
            {
                engine.registerTemplate(Files.newInputStream(file));
            } catch(ParsingException e)
            {
                throw new LoaderException(file, e);
            }
        }

        private void handleWatchService (WatchService service, TemplatingEngine engine)
        {
            for(;;)
            {
                WatchKey key;

                try
                {
                    key = service.take();
                } catch (InterruptedException e)
                {
                    return;
                }

                Path base = watchKeyPaths.get(key);

                if(base != null)
                {
                    for(WatchEvent<?> ev : key.pollEvents())
                    {
                        if(ev.context() instanceof Path &&
                            ev.kind() == StandardWatchEventKinds.ENTRY_MODIFY &&
                            !Files.isDirectory((Path) ev.context()))
                        {
                            try
                            {
                                handleFileObject(base.resolve(((Path) ev.context())), engine, false);
                            } catch(IOException | LoaderException e)
                            {
                                // TODO: engine.removeTemplate(...)
                                // Or other way to inform the template wasn't parsed.
                            }
                        }
                    }
                }
                key.reset();
            }
        }
    }
}
