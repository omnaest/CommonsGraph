package org.omnaest.utils.graph.domain.attributes;

/**
 * Special {@link Attribute} which does not define a value and only defines a key.
 * 
 * @see #of(String)
 * @author omnaest
 */
public class Tag extends Attribute
{
    private Tag(String key)
    {
        super(key, null);
    }

    private Tag()
    {
        this(null);
    }

    public static Tag of(String key)
    {
        return new Tag(key);
    }

}