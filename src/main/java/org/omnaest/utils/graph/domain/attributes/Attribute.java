package org.omnaest.utils.graph.domain.attributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see #of(String, String)
 * @author omnaest
 */
public class Attribute
{
    @JsonProperty
    private String key;

    @JsonProperty
    private List<String> values;

    protected Attribute(String key, List<String> values)
    {
        super();
        this.key = key;
        this.values = values;
    }

    protected Attribute()
    {
        super();
    }

    public String getKey()
    {
        return this.key;
    }

    @JsonIgnore
    public Optional<String> getValue()
    {
        return Optional.ofNullable(this.values)
                       .orElse(Collections.emptyList())
                       .stream()
                       .findFirst();
    }

    public List<String> getValues()
    {
        return this.values;
    }

    public static Attribute of(String key, String value)
    {
        return new Attribute(key, Arrays.asList(value));
    }

    public static Attribute of(String key, Collection<String> values)
    {
        return new Attribute(key, Optional.ofNullable(values)
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .collect(Collectors.toList()));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Attribute [key=")
               .append(this.key)
               .append(", values=")
               .append(this.values)
               .append("]");
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
        result = prime * result + ((this.values == null) ? 0 : this.values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof Attribute))
        {
            return false;
        }
        Attribute other = (Attribute) obj;
        if (this.key == null)
        {
            if (other.key != null)
            {
                return false;
            }
        }
        else if (!this.key.equals(other.key))
        {
            return false;
        }
        if (this.values == null)
        {
            if (other.values != null)
            {
                return false;
            }
        }
        else if (!this.values.equals(other.values))
        {
            return false;
        }
        return true;
    }

}