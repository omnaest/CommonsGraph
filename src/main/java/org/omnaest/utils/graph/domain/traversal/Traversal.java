package org.omnaest.utils.graph.domain.traversal;

import java.util.Collection;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.RoutingStrategy.EdgeFilter;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.node.Nodes;
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

    /**
     * Traverses internally until the given level, but returns only nodes of the given level in successive method calls like {@link Traversal#stream()}. Level
     * can be 0, 1, 2, ...
     * 
     * @param level
     * @return
     */
    public Traversal level(int level);

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

    /**
     * Same as {@link #nodes()} but returns the {@link NodeIdentity}s.
     * 
     * @return
     */
    public default Stream<NodeIdentity> identities()
    {
        return this.nodes()
                   .map(Node::getIdentity);
    }

    public Hierarchy asHierarchy();

    /**
     * @see #andAfterwardsTraverseOutgoing()
     * @see #andAfterwardsTraverse(Direction...)
     * @return
     */
    public Traversal andAfterwardsTraverseIncoming();

    /**
     * @see #andTraverseOutgoing(NodeIdentity...)
     * @see #andTraverse(Direction, NodeIdentity...)
     * @param startNodes
     * @return
     */
    public Traversal andTraverseIncoming(NodeIdentity... startNodes);

    /**
     * @see #andAfterwardsTraverseIncoming()
     * @see #andAfterwardsTraverse(Direction...)
     * @return
     */
    public Traversal andAfterwardsTraverseOutgoing();

    /**
     * @see #andTraverseIncoming(NodeIdentity...)
     * @see #andTraverse(Direction, NodeIdentity...)
     * @param startNodes
     * @return
     */
    public Traversal andTraverseOutgoing(NodeIdentity... startNodes);

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
     * @see #andAfterwardsTraverse(Direction...)
     * @param filter
     * @param directions
     * @return
     */
    public Traversal andAfterwardsTraverse(Traversal.TraversalStepFilter filter, Direction... directions);

    /**
     * Similar to {@link #andAfterwardsTraverse(BiPredicate, Direction...)} without the ability to define a filter.
     * 
     * @see #andAfterwardsTraverse(BiPredicate, Direction...)
     * @see #andAfterwardsTraverseIncoming()
     * @see #andAfterwardsTraverseOutgoing()
     * @return
     */
    public Traversal andAfterwardsTraverse(Direction... directions);

    /**
     * Allows to traverse a secondary set of start nodes at the same time in the given {@link Direction}
     * 
     * @param direction
     * @param startNodes
     * @return
     */
    public Traversal andTraverse(Direction direction, NodeIdentity... startNodes);

    /**
     * Similar to {@link #andTraverse(Direction, NodeIdentity...)}
     * 
     * @param direction
     * @param startNodes
     * @return
     */
    public Traversal andTraverse(Direction direction, Collection<NodeIdentity> startNodes);

    /**
     * Traverses the {@link Graph} but keeps the subgraph traversal from all startnodes in a distinct context, which means the same nodes and pathes can be
     * traversed twice, if they are on a route that originates from at least two start nodes.
     * 
     * @param direction
     * @param startNodes
     * @return
     */
    public Traversal andTraverseEach(Direction direction, Collection<NodeIdentity> startNodes);

    public Traversal withEdgeFilter(EdgeFilter edgeFilter);

    public Traversal withExcludingEdgeByTagFilter(Tag... tag);

    /**
     * Enables a {@link Graph}
     * 
     * @return
     */
    public Traversal withTracingGraph();

    /**
     * Adds a {@link GraphViewConsumer} as listener which is invoked for each forward step in the traversal.
     * 
     * @param graphConsumer
     * @return
     */
    public Traversal withTracingGraphStepListener(GraphViewConsumer graphConsumer);

    public static interface GraphViewConsumer extends Consumer<GraphView>
    {

    }

    public Traversal andTraverse(Direction direction, TraversalStepFilter filter, NodeIdentity... startNodes);

    public TraversalTerminal and();

    /**
     * Ensure that the last called traversal definition is run in an own context. That means that e.g. nodes that have been visited by another traversal
     * chain/step will not be recognized by this traversal.
     * <br>
     * <br>
     * Example:<br>
     * 
     * <pre>
     * traverseOutgoing().withOwnContext().nodes()....
     * </pre>
     * 
     * @return
     */
    public Traversal withOwnContext();

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

    public static interface TraversalTerminal
    {
        public GraphSteppedView viewAsGraph();

        public VisitedNodesStatistic getVisitedNodesStatistic();
    }

    /**
     * @see #get()
     * @see #layers()
     * @author omnaest
     */
    public static interface GraphView
    {

        /**
         * Returns the final {@link Graph} of the traversed {@link Nodes}
         */
        public Graph get();

        /**
         * Allows to slit the final {@link Graph} of the traversed {@link Nodes} into {@link GraphLayers}
         * 
         * @see #get()
         * @return
         */
        public GraphLayersSelector layers();
    }

    /**
     * @see #stream()
     * @author omnaest
     */
    public static interface GraphSteppedView extends GraphView, Streamable<Graph>
    {

        /**
         * Similar to {@link #steps()}
         */
        @Override
        public Stream<Graph> stream();

        /**
         * Returns a {@link Stream} of a {@link Graph} for each step of the traversal
         */
        public default Stream<Graph> steps()
        {
            return this.stream();
        }

    }

    public static interface GraphLayersSelector
    {
        /**
         * Returns the {@link GraphLayers} where the {@link Nodes} of a {@link GraphLayer} have exactly the visitor count
         * 
         * @see #byAtLeastVisitedCount()
         * @return
         */
        public GraphLayers byVisitedCount();

        /**
         * Returns the {@link GraphLayers} where the {@link Nodes} of a {@link GraphLayer} have at least the visitor count
         * 
         * @see #byVisitedCount()
         * @return
         */
        public GraphLayers byAtLeastVisitedCount();
    }

    public static interface GraphLayers extends Streamable<GraphLayer>
    {

    }

    public static interface GraphLayer extends Supplier<Graph>
    {
        /**
         * Returns the {@link Graph} of this {@link GraphLayers}
         */
        @Override
        public Graph get();

        public int getIndex();

    }

    public static interface VisitedNodesStatistic
    {

        public int getCountFor(NodeIdentity nodeIdentity);

        public Set<NodeIdentity> nodes();

        public Set<NodeIdentity> nodesByCount(int count);

        public int getNumberOfSteps();

        public int getMaxCount();

    }

    /**
     * Limits the number of deepness steps to the given number.
     * 
     * @param steps
     */
    public Traversal limitStepsTo(int steps);

    /**
     * Skips the given number of steps within the result. Internally such steps are taken.
     * 
     * @param steps
     * @return
     */
    public Traversal skipSteps(int steps);

    /**
     * Returns the maximum deepness of all the routes of the traversal. If the graph has cyclic nodes, the deepness determination stops as soon as a cycle is
     * hit.
     * 
     * @return
     */
    public int deepness();
}