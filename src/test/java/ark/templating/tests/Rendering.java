package ark.templating.tests;

import org.junit.Assert;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;
import pt.neticle.ark.templating.TemplatingEngine;
import pt.neticle.ark.templating.renderer.MainScope;
import pt.neticle.ark.templating.renderer.Scope;

import javax.xml.transform.Source;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class Rendering
{
    private final TemplatingEngine engine;

    public Rendering () throws Exception
    {
        engine = TemplatingEngine.initializer()
            .withSearchDirectory(Paths.get(getClass().getResource("/templates").toURI()))
            .build();
    }

    @Test
    public void testPageElement () throws IOException
    {
        compare("page", "/results/page.html",
            MainScope.builder()
                .withList("tags", "a", "b", "c")
                .with("message", "Hello world")
                .build());
    }

    private void compare (String template, String expectedResultFile, Scope scope) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        engine.render(engine.getTemplate(template), scope, baos);

        Source rendered = Input.fromByteArray(baos.toByteArray()).build();
        Source expected = Input.fromStream(getClass().getResource(expectedResultFile).openStream()).build();

        Diff diff = DiffBuilder.compare(rendered).withTest(expected)
                .ignoreWhitespace().build();

        if(diff.hasDifferences())
        {
            Assert.fail(diff.toString());
        }
    }
}
