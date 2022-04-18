package org.omnaest.utils.graph.internal.data.domain;

import java.util.Set;

import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

public class RawEdge
{
    private NodeIdentity   from;
    private NodeIdentity   to;
    private Set<Attribute> attributes;

    public RawEdge(NodeIdentity from, NodeIdentity to, Set<Attribute> attributes)
    {
        super();
        this.from = from;
        this.to = to;
        this.attributes = attributes;
    }

    public NodeIdentity getFrom()
    {
        return this.from;
    }

    public NodeIdentity getTo()
    {
        return this.to;
    }

    public Set<Attribute> getAttributes()
    {
        return this.attributes;
    }

}