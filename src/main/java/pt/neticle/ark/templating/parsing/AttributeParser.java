package pt.neticle.ark.templating.parsing;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class AttributeParser
{
    private final Map<String, String> attributeMap;

    public AttributeParser (String attributes)
    {
        attributeMap = new HashMap<>();

        String current = attributes.trim();

        // Matches anything resembling an attibute name followed by an equals sign
        while(current.matches("^[\\w\\d-:]+[$#]?=[\\S\\s]*$"))
        {
            String name = current.substring(0, current.indexOf('='));

            // In order to take escaped quotes in account, we iterate over the
            // value string to determine where the quotes start and end
            int quoteStart = -1, quoteEnd = -1;
            for(int i = current.indexOf('=')+1; i < current.length(); i++)
            {
                if(quoteStart == -1 && current.charAt(i) == '"')
                {
                    quoteStart = i;
                    continue;
                }

                if(quoteStart != -1 && current.charAt(i) == '\\' && current.charAt(i+1) == '"')
                {
                    i++;
                    continue;
                }

                if(quoteStart != -1 && current.charAt(i) == '"')
                {
                    quoteEnd = i;
                    break;
                }
            }

            if(quoteEnd == -1 || quoteEnd == -1)
            {
                attributeMap.put(name, null);
                break;
            }

            attributeMap.put(name, current.substring(quoteStart+1, quoteEnd));

            current = current.substring(quoteEnd+1).trim();
        }
    }

    public Stream<Map.Entry<String,String>> attributes ()
    {
        return attributeMap.entrySet().stream();
    }

    public Map<String,String> getMap ()
    {
        return attributeMap;
    }
}
