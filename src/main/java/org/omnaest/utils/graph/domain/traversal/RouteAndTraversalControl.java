package org.omnaest.utils.graph.domain.traversal;

import java.util.Collection;
import java.util.function.Supplier;

import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.traversal.Traversal.TraversalStepContext;

public interface RouteAndTraversalControl extends Supplier<Route>
{
    /**
     * Does not traverse this route further.
     * 
     * @return
     */
    public RouteAndTraversalControl skip();

    /**
     * Invokes {@link #skip()} if the condition is true
     * 
     * @param condition
     * @return
     */
    public RouteAndTraversalControl skipIf(boolean condition);

    /**
     * Skips the given {@link NodeIdentity}s for the graph traversal.
     * 
     * @param nodeIdentities
     * @return
     */
    public RouteAndTraversalControl skipNodes(Collection<NodeIdentity> nodeIdentities);

    public RouteAndTraversalControl skipNodes(NodeIdentity... nodeIdentities);

    public RouteAndTraversalControl skipEdgesWithTag(Tag tag);

    public RouteAndTraversalControl skipNextRouteNodes(NodeIdentity... nodeIdentities);

    public RouteAndTraversalControl skipNextRouteNodes(Collection<NodeIdentity> nodeIdentities);

    public TraversalStepContext getTraversalStepContext();
}