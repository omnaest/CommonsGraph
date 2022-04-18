package org.omnaest.utils.graph.domain.edge;

import java.util.Set;

import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentityWithAttributes;
import org.omnaest.utils.stream.Streamable;

/**
 * Batch of {@link Edge}s
 * 
 * @author omnaest
 */
public interface Edges extends Streamable<Edge>
{
    public int size();

    public Set<EdgeIdentity> identities();

    public Set<EdgeIdentityWithAttributes> identitiesWithAttributes();

}
