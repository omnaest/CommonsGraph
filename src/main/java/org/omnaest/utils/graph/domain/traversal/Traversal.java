package org.omnaest.utils.graph.domain.traversal;

import java.util.OptionalDouble;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.traversal.hierarchy.Hierarchy;
import org.omnaest.utils.stream.Streamable;

public interface Traversal extends Streamable<TraversalRoutes>
{
    /**
     * Allows to inspect all {@link TraversalRoutes} that have hit already visited {@link Node}s
     * 
     * @param routesConsumer
     * @return
     */
    public Traversal withAlreadyVisitedNodesHitHandler(TraversalRoutesConsumer routesConsumer);

    /**
     * Includes the first {@link Route} of a {@link Node} that is hit a second time, but doesn't follow any further pathes for that {@link Node}.
     * 
     * @return
     */
    public Traversal includingFirstRouteOfAlreadyVisitedNodes();

    public Traversal withWeightedPathTermination(double terminationWeightBarrier, Traversal.NodeWeightDeterminationFunction nodeWeightDeterminationFunction);

    /**
     * Similar to {@link #withWeightedPathTermination(double, NodeWeightDeterminationFunction)} which uses a weight scoring function that return a score
     * inverse to the number of sibling branches. <br>
     * <br>
     * In a binary tree as example, all nodes would score to 0.5 * parent.
     * 
     * @param terminationWeightBarrier
     * @return
     */
    public Traversal withWeightedPathTerminationByBranches(double terminationWeightBarrier);

    /**
     * Similar to {@link #withWeightedPathTerminationByBranches(double)} but allows to specify an {@link IsolatedNodeWeightDeterminationFunction}.
     * 
     * @param terminationWeightBarrier
     * @param nodeWeightDeterminationFunction
     * @return
     */
    public Traversal withWeightedPathTerminationByBranches(double terminationWeightBarrier,
                                                           Traversal.IsolatedNodeWeightDeterminationFunction nodeWeightDeterminationFunction);

    /**
     * @see #withWeightedPathTermination(double, NodeWeightDeterminationFunction)
     * @param terminationWeightBarrier
     * @param nodeWeightByRouteDeterminationFunction
     * @return
     */
    public Traversal withWeightedPathTerminationByBranchesAndRoute(double terminationWeightBarrier,
                                                                   Traversal.IsolatedNodeWeightByRouteDeterminationFunction nodeWeightByRouteDeterminationFunction);

    public static interface NodeWeightDeterminationFunction
    {
        public double apply(Node node, Route route, OptionalDouble parentWeight, ForwardNodeFunction forwardNodeFunction);
    }

    /**
     * @see #applyAsDouble(Node)
     * @author omnaest
     */
    public static interface IsolatedNodeWeightDeterminationFunction extends ToDoubleFunction<Node>
    {
        /**
         * Determines the isolated weight of a single {@link Node} without any regard to its parent or siblings.
         * 
         * @param node
         * @return
         */
        @Override
        public double applyAsDouble(Node node);
    }

    /**
     * @see #applyAsDouble(Route)
     * @author omnaest
     */
    public static interface IsolatedNodeWeightByRouteDeterminationFunction extends ToDoubleFunction<SimpleRoute>
    {
        /**
         * Determines the isolated weight of a single {@link Route}.
         * 
         * @param route
         * @return
         */
        @Override
        public double applyAsDouble(SimpleRoute route);
    }

    /**
     * {@link Stream} of the {@link TraversalRoutes}. Each {@link TraversalRoutes} encapsulates a single batch of {@link Node}s
     * 
     * @see #routes()
     * @see #nodes()
     */
    @Override
    public Stream<TraversalRoutes> stream();

    /**
     * {@link Stream} of the routes returned by the traversal
     * 
     * @see #nodes()
     * @see #routesAndTraversalControls()
     * @see #stream()
     * @return
     */
    public Stream<Route> routes();

    /**
     * {@link Stream} of the {@link RouteAndTraversalControl}s
     * 
     * @see #routes()
     * @return
     */
    public Stream<RouteAndTraversalControl> routesAndTraversalControls();

    /**
     * {@link Stream} of {@link Node}s returned by the traversal cursor.
     * 
     * @see #stream()
     * @see #routes()
     * @return
     */
    public Stream<Node> nodes();

    public Hierarchy asHierarchy();

    /**
     * @see #andTraverseOutgoing()
     * @see #andTraverse(Direction...)
     * @return
     */
    public Traversal andTraverseIncoming();

    /**
     * @see #andTraverseIncoming()
     * @see #andTraverse(Direction...)
     * @return
     */
    public Traversal andTraverseOutgoing();

    /**
     * Defines secondary, tertiary, ... directions where nodes are traversed. E.g. if the primary direction is incoming and the secondary direction is
     * outgoing, the start node is traversed in incoming direction and the start node and all discovered nodes by this process are traversed in the
     * secondary/outgoing
     * direction as well.<br>
     * <br>
     * Multiple given directions define the different node bags and their traversal direction, where every processed node from an earlier bag will be pushed
     * into the next bag.
     * <br>
     * <br>
     * The inclusion filter allows to limit the nodes passed to the next bag of nodes.
     * 
     * @see #andTraverse(Direction...)
     * @param filter
     * @param directions
     * @return
     */
    public Traversal andTraverse(Traversal.TraversalStepFilter filter, Direction... directions);

    /**
     * Similar to {@link #andTraverse(BiPredicate, Direction...)} without the ability to define a filter.
     * 
     * @see #andTraverse(BiPredicate, Direction...)
     * @see #andTraverseIncoming()
     * @see #andTraverseOutgoing()
     * @return
     */
    public Traversal andTraverse(Direction... directions);

    /**
     * Inclusion filter for a node to be passed downwards to another processing step/bag
     * 
     * @author omnaest
     */
    public static interface TraversalStepFilter extends BiPredicate<Node, Traversal.TraversalStepContext>
    {

    }

    public static interface TraversalStepContext
    {
        /**
         * Returns the current step which is 0,1,2,... and increasing with the node passing each and every bag
         * 
         * @return
         */
        public int getStep();

        public Direction getDirection();

        public ForwardNodeFunction getForwardNodeFunction();

        /**
         * Returns true, if this is the primary/first traversal step
         * 
         * @return
         */
        public default boolean isPrimaryStep()
        {
            return this.getStep() == 0;
        }
    }

}