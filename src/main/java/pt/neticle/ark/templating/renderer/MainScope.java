package pt.neticle.ark.templating.renderer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainScope extends InternalScope
{
    public MainScope ()
    {
        super(null);
    }

    public MainScope (Map<String, Object> data)
    {
        super(null, data);
    }

    public static class Builder
    {
        private final Map<String, Object> data;

        Builder ()
        {
            data = new HashMap<>();
        }

        public Builder with (String key, Supplier<Object> value)
        {
            data.put(key, value);
            return this;
        }

        public Builder with (String key, Object value)
        {
            data.put(key, value);
            return this;
        }

        public Builder withMap (String key, Consumer<Map<String,Object>> initializer)
        {
            initializer.accept
            (
                (Map<String,Object>)data.compute(key,
                    (k, v) -> (v == null || !(v instanceof Map)) ?
                        new HashMap<String, Object>() : v
                )
            );

            return this;
        }

        public <T> Builder withList (String key, T... values)
        {
            data.put(key, Arrays.asList(values));
            return this;
        }

        public MainScope build ()
        {
            return new MainScope(data);
        }
    }

    public static Builder builder ()
    {
        return new Builder();
    }
}
