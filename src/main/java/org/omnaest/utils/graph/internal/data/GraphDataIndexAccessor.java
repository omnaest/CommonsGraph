package org.omnaest.utils.graph.internal.data;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.internal.data.components.GraphNodeDataIndex.NodeData;
import org.omnaest.utils.graph.internal.data.domain.RawEdge;
import org.omnaest.utils.graph.internal.data.filter.GraphNodesFilter;

public class GraphDataIndexAccessor
{
    private GraphDataIndex   graphDataIndex;
    private GraphNodesFilter graphNodesFilter;

    public GraphDataIndexAccessor(GraphDataIndex graphDataIndex, GraphNodesFilter graphNodesFilter)
    {
        super();
        this.graphDataIndex = graphDataIndex;
        this.graphNodesFilter = graphNodesFilter;
    }

    public GraphDataIndex getGraphDataIndex()
    {
        return this.graphDataIndex;
    }

    public GraphNodesFilter getGraphNodesFilter()
    {
        return this.graphNodesFilter;
    }

    public Set<NodeIdentity> getNodes()
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphDataIndex.getNodes());
    }

    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphDataIndex.getOutgoingNodes(nodeIdentity));
    }

    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphDataIndex.getIncomingNodes(nodeIdentity));
    }

    public Optional<Set<Attribute>> getEdgeAttributes(NodeIdentity from, NodeIdentity to)
    {
        return this.graphDataIndex.getEdgeAttributes(from, to);
    }

    public Stream<RawEdge> getEdges()
    {
        return this.graphDataIndex.getEdges();
    }

    public boolean containsNode(NodeIdentity node)
    {
        return this.graphNodesFilter.test(node) && this.graphDataIndex.containsNode(node);
    }

    public boolean hasUnresolvedNodes()
    {
        return !this.graphDataIndex.getUnresolvedNodes()
                                   .isEmpty();
    }

    public Set<NodeIdentity> getUnresolvedNodes()
    {
        return this.graphNodesFilter.getFilteredNodeIdentities(this.graphDataIndex.getUnresolvedNodes());
    }

    @Override
    public String toString()
    {
        return this.graphDataIndex.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.graphDataIndex == null) ? 0 : this.graphDataIndex.hashCode());
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
        if (!(obj instanceof GraphDataIndexAccessor))
        {
            return false;
        }
        GraphDataIndexAccessor other = (GraphDataIndexAccessor) obj;
        if (this.graphDataIndex == null)
        {
            if (other.graphDataIndex != null)
            {
                return false;
            }
        }
        else if (!this.graphDataIndex.equals(other.graphDataIndex))
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

    public Optional<NodeData> getNodeData(NodeIdentity nodeIdentity)
    {
        return this.graphDataIndex.getNodeData(nodeIdentity);
    }

}