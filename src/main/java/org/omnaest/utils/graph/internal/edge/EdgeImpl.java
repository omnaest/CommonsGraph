package org.omnaest.utils.graph.internal.edge;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.element.bi.UnaryBiElement;
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
    public UnaryBiElement<NodeIdentity> getNodeIdentities()
    {
        return UnaryBiElement.of(this.from.getIdentity(), this.to.getIdentity());
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

    @Override
    public boolean hasAnyTag(Tag... tags)
    {
        return Optional.ofNullable(tags)
                       .map(Stream::of)
                       .orElse(Stream.empty())
                       .anyMatch(this::hasTag);
    }

}