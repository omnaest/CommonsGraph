package org.omnaest.utils.graph.internal.index;

import java.util.Optional;
import java.util.Set;

import org.omnaest.utils.graph.domain.Attribute;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.filter.GraphNodesFilter;

public class GraphIndexAccessor
{
    private GraphIndex       graphIndex;
    private GraphNodesFilter graphNodesFilter;

    public GraphIndexAccessor(GraphIndex graphIndex, GraphNodesFilter graphNodesFilter)
    {
        super();
        this.graphIndex = graphIndex;
        this.graphNodesFilter = graphNodesFilter;
    }

    public GraphIndex getGraphIndex()
    {
        return this.graphIndex;
    }

    public GraphNodesFilter getGraphNodesFilter()
    {
        return this.graphNodesFilter;
    }

    public Set<NodeIdentity> getNodes()
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphIndex.getNodes());
    }

    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphIndex.getOutgoingNodes(nodeIdentity));
    }

    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphIndex.getIncomingNodes(nodeIdentity));
    }

    public Optional<Set<Attribute>> getEdgeAttributes(NodeIdentity from, NodeIdentity to)
    {
        return this.graphIndex.getEdgeAttributes(from, to);
    }

    public boolean containsNode(NodeIdentity node)
    {
        return this.graphNodesFilter.test(node) && this.graphIndex.containsNode(node);
    }

    public boolean hasUnresolvedNodes()
    {
        return !this.graphIndex.getUnresolvedNodes()
                               .isEmpty();
    }

    public Set<NodeIdentity> getUnresolvedNodes()
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphIndex.getUnresolvedNodes());
    }

    @Override
    public String toString()
    {
        return this.graphIndex.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.graphIndex == null) ? 0 : this.graphIndex.hashCode());
        result = prime * result + ((this.graphNodesFilter == null) ? 0 : this.graphNodesFilter.hashCode());
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
        if (!(obj instanceof GraphIndexAccessor))
        {
            return false;
        }
        GraphIndexAccessor other = (GraphIndexAccessor) obj;
        if (this.graphIndex == null)
        {
            if (other.graphIndex != null)
            {
                return false;
            }
        }
        else if (!this.graphIndex.equals(other.graphIndex))
        {
            return false;
        }
        if (this.graphNodesFilter == null)
        {
            if (other.graphNodesFilter != null)
            {
                return false;
            }
        }
        else if (!this.graphNodesFilter.equals(other.graphNodesFilter))
        {
            return false;
        }
        return true;
    }

}