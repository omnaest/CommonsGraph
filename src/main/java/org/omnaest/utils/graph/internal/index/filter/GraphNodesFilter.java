package org.omnaest.utils.graph.internal.index.filter;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

public class GraphNodesFilter implements Predicate<NodeIdentity>
{
    private Set<NodeIdentity> excludedNodes;
    private Set<NodeIdentity> includedNodes;

    public GraphNodesFilter(Set<NodeIdentity> excludedNodes, Set<NodeIdentity> includedNodes)
    {
        super();
        this.excludedNodes = excludedNodes;
        this.includedNodes = includedNodes;
    }

    public static GraphNodesFilter empty()
    {
        return new GraphNodesFilter(Collections.emptySet(), Collections.emptySet());
    }

    @Override
    public boolean test(NodeIdentity nodeIdentity)
    {
        boolean excludeNone = this.excludedNodes.isEmpty();
        boolean isExcluded = !excludeNone && this.excludedNodes.contains(nodeIdentity);
        boolean includeAll = this.includedNodes.isEmpty();
        boolean isIncluded = includeAll || this.includedNodes.contains(nodeIdentity);
        return !isExcluded && isIncluded;
    }

    private boolean hasFilter()
    {
        return !this.includedNodes.isEmpty() || !this.excludedNodes.isEmpty();
    }

    public Set<NodeIdentity> getFilteredNodeIdentities(Set<NodeIdentity> nodeIdentities)
    {
        return this.hasFilter() ? SetUtils.newFilteredSet(nodeIdentities, this) : nodeIdentities;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.excludedNodes == null) ? 0 : this.excludedNodes.hashCode());
        result = prime * result + ((this.includedNodes == null) ? 0 : this.includedNodes.hashCode());
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
        if (!(obj instanceof GraphNodesFilter))
        {
            return false;
        }
        GraphNodesFilter other = (GraphNodesFilter) obj;
        if (this.excludedNodes == null)
        {
            if (other.excludedNodes != null)
            {
                return false;
            }
        }
        else if (!this.excludedNodes.equals(other.excludedNodes))
        {
            return false;
        }
        if (this.includedNodes == null)
        {
            if (other.includedNodes != null)
            {
                return false;
            }
        }
        else if (!this.includedNodes.equals(other.includedNodes))
        {
            return false;
        }
        return true;
    }

}