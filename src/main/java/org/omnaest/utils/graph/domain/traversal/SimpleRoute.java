package org.omnaest.utils.graph.domain.traversal;

import java.util.List;
import java.util.Optional;

import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.stream.Streamable;

public interface SimpleRoute extends Streamable<Node>
{
    public List<NodeIdentity> toNodeIdentities();

    /**
     * Returns the last {@link Node} of the {@link Route}.
     * 
     * @return
     */
    @Override
    public Optional<Node> last();

    /**
     * Returns the nth last {@link Node}. index = 0,1,2,... where 0=last, 1= second last, ...
     * 
     * @param index
     * @return
     */
    public Optional<Node> lastNth(int index);

    public boolean isCyclic();

    public boolean isNotCyclic();

    /**
     * Adds a new {@link NodeIdentity} to the existing {@link Route} nodes and returns a new {@link Route} instance with that appended {@link NodeIdentity}.
     * 
     * @param nodeIdentity
     * @return
     */
    public SimpleRoute addToNew(NodeIdentity nodeIdentity);

    /**
     * Returns a sub {@link Route} until the nth last {@link Node}
     * 
     * @param index
     * @return
     */
    public SimpleRoute getSubRouteUntilLastNth(int index);
}