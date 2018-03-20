package pt.neticle.ark.templating.structure.functions;

import pt.neticle.ark.templating.exception.RenderingException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GetFunction extends DefaultFunctionHandler<Object>
{
    @Override
    public String getName ()
    {
        return "Get";
    }

    @Override
    public Object apply (Object[] args) throws RenderingException
    {
        if(args[0] == null || args[1] == null)
        {
            return null;
        }

        ensureSignatureArgs(args, Object.class, String.class);

        if(args[0] instanceof Map)
        {
            return ((Map) args[0]).get(args[1]);
        }

        else if(args[0] instanceof Map.Entry)
        {
            // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4207233
            if(args[1].equals("key"))
            {
                return ((Map.Entry) args[0]).getKey();
            } else if(args[1].equals("value"))
            {
                return ((Map.Entry) args[0]).getValue();
            } else
            {
                return null;
            }
        }

        else
        {
            Method getter;

            try
            {
                getter = args[0].getClass().getMethod("get" + ((String)args[1]).substring(0, 1).toUpperCase() + ((String) args[1]).substring(1));
            }
            catch(NoSuchMethodException e)
            {
                return null;
            }

            try
            {
                return getter.invoke(args[0]);
            }
            catch(IllegalAccessException e)
            {
                return null;
            }
            catch(InvocationTargetException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
