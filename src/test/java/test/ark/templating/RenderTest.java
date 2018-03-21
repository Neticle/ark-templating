package test.ark.templating;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.exception.ParsingException;
import pt.neticle.ark.templating.renderer.MainScope;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class RenderTest
{
    @Test
    public void testRenderFiles () throws URISyntaxException, IOException
    {
        Path base = Paths.get(getClass().getResource("/render-tests").toURI());

        if(!Files.isDirectory(base))
        {
            Assert.fail("Can't find render-tests directory");
        }

        for(Path file : Files.newDirectoryStream(base, "*.test.html"))
        {
            testRenderFile(file);
        }
    }

    private void testRenderFile (Path file) throws IOException
    {
        String[] lines = Files.lines(file, StandardCharsets.UTF_8).toArray(String[]::new);

        Map<String, String> segments = new HashMap<>();
        String currentSegmentName = null;
        int nTemplate = 0;

        for(String line : lines)
        {
            if(line.startsWith("## "))
            {
                currentSegmentName = line.substring(3).trim();

                if(currentSegmentName.equals("TEMPLATE"))
                {
                    currentSegmentName += ++nTemplate;
                }

                continue;
            }

            if(currentSegmentName != null)
            {
                segments.compute(currentSegmentName, (k, v) -> v == null ? line : v + line);
            }
        }

        /*segments.entrySet().stream()
            .forEach(e -> System.out.println(e.getKey() + ":\n" + e.getValue()));*/

        String scopeStr = segments.getOrDefault("SCOPE", "{}");
        Map<String, Object> scope =
            new ObjectMapper().readValue(scopeStr, new TypeReference<Map<String,Object>>(){});

        TemplatingEngine engine = new TemplatingEngine();

        segments.entrySet().stream()
            .filter(e -> e.getKey().startsWith("TEMPLATE"))
            .forEach(e ->
            {
                try
                {
                    engine.registerTemplate(new ByteArrayInputStream(e.getValue().getBytes(StandardCharsets.UTF_8)));
                } catch(IOException | ParsingException ex)
                {
                    Assert.fail(e.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            });

        Assert.assertTrue("Test file doesn't define 'test' template", engine.hasTemplate("test"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        engine.render(engine.getTemplate("test"), new MainScope(scope), baos);
        byte[] renderedBytes = baos.toByteArray();

        Source rendered = Input.fromByteArray(renderedBytes).build();
        Source expected = Input.fromString(segments.getOrDefault("EXPECTED-RESULT", "")).build();

        Diff diff = DiffBuilder.compare(rendered).withTest(expected)
            .ignoreWhitespace().build();

        if(diff.hasDifferences())
        {
            System.out.println(new String(renderedBytes, StandardCharsets.UTF_8));
            Assert.fail(diff.toString());
        }
    }
}
