package org.omnaest.utils.graph.internal.edge;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentityWithAttributes;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.edge.Edges;

/**
 * @see Edges
 * @author omnaest
 */
public class EdgesImpl implements Edges
{
    private List<Edge> edges;

    public EdgesImpl(List<Edge> edges)
    {
        super();
        this.edges = edges;
    }

    @Override
    public Stream<Edge> stream()
    {
        return this.edges.stream();
    }

    @Override
    public int size()
    {
        return this.edges.size();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.edges == null) ? 0 : this.edges.hashCode());
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
        if (!(obj instanceof EdgesImpl))
        {
            return false;
        }
        EdgesImpl other = (EdgesImpl) obj;
        if (this.edges == null)
        {
            if (other.edges != null)
            {
                return false;
            }
        }
        else if (!this.edges.equals(other.edges))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EdgesImpl [edges=")
               .append(this.edges)
               .append("]");
        return builder.toString();
    }

    @Override
    public Set<EdgeIdentity> identities()
    {
        return this.stream()
                   .map(Edge::getIdentity)
                   .collect(Collectors.toSet());
    }

    @Override
    public Set<EdgeIdentityWithAttributes> identitiesWithAttributes()
    {
        return this.stream()
                   .map(Edge::getIdentityWithAttributes)
                   .collect(Collectors.toSet());
    }

}
