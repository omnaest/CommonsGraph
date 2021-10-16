package org.omnaest.utils.graph.internal.edge;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.Attribute;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Tag;

public class EdgeImpl implements Edge
{
    private final Node           from;
    private final Node           to;
    private final Set<Attribute> attributes;

    public EdgeImpl(Node from, Node to, Set<Attribute> attributes)
    {
        this.from = from;
        this.to = to;
        this.attributes = attributes;
    }

    @Override
    public Node getFrom()
    {
        return this.from;
    }

    @Override
    public Node getTo()
    {
        return this.to;
    }

    @Override
    public Set<Attribute> getAttributes()
    {
        return SetUtils.toNew(this.attributes);
    }

    @Override
    public List<NodeIdentity> getNodeIdentities()
    {
        return Arrays.asList(this.from.getIdentity(), this.to.getIdentity());
    }

    @Override
    public Stream<Attribute> getAttributesByKey(String key)
    {
        return this.attributes.stream()
                              .filter(attribute -> StringUtils.equals(attribute.getKey(), key));
    }

    @Override
    public boolean hasTag(Tag tag)
    {
        return tag != null && this.attributes.contains(tag);
    }
}