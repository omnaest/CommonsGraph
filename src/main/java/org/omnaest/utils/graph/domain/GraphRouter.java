/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.utils.graph.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import org.omnaest.utils.stream.Streamable;

public interface GraphRouter
{
    public RoutingStrategy withBreadthFirst();

    public static interface RoutingStrategy
    {
        /**
         * {@link Predicate} for {@link Edge}s
         * 
         * @author omnaest
         */
        public static interface EdgeFilter extends Predicate<Edge>
        {
        }

        /**
         * Adds a static {@link EdgeFilter}
         * 
         * @param edgeFilter
         * @return
         */
        public RoutingStrategy withEdgeFilter(EdgeFilter edgeFilter);

        /**
         * Similar to {@link #withEdgeFilter(EdgeFilter)} with a special filter that excludes {@link Edge}s with any of the given {@link Tag}
         * 
         * @param tag
         * @return
         */
        public RoutingStrategy withExcludingEdgeByTagFilter(Tag... tag);

        public Routes findAllOutgoingRoutesBetween(NodeIdentity from, NodeIdentity to);

        public Routes findAllIncomingRoutesBetween(NodeIdentity from, NodeIdentity to);

        /**
         * Disables the lazy loading of {@link Node}s and relies only of already resolved {@link Node}s
         * 
         * @return
         */
        public RoutingStrategy withDisabledNodeResolving();

        public RoutingStrategy withDisabledNodeResolving(boolean disabledNodeResolving);

        public RoutingStrategy budgetOptimized();

        public Traversal traverseIncoming(Set<NodeIdentity> startNodes);

        public Traversal traverseIncoming();

        public Traversal traverseOutgoing(Set<NodeIdentity> startNodes);

        public Traversal traverseOutgoing(NodeIdentity... startNodes);

        public Traversal traverseOutgoing();

        public Traversal traverse(Direction direction, NodeIdentity... startNodes);

        public Traversal traverse(Direction direction, Set<NodeIdentity> startNodes);

        public Traversal traverseIncoming(NodeIdentity... startNodes);

    }

    public static interface Traversal extends Streamable<TraversalRoutes>
    {
        /**
         * Allows to inspect all {@link TraversalRoutes} that have hit already visited {@link Node}s
         * 
         * @param routesConsumer
         * @return
         */
        public Traversal withAlreadyVisitedNodesHitHandler(TraversalRoutesConsumer routesConsumer);

        public Traversal withWeightedPathTermination(double terminationWeightBarrier, NodeWeightDeterminationFunction nodeWeightDeterminationFunction);

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
                                                               IsolatedNodeWeightDeterminationFunction nodeWeightDeterminationFunction);

        /**
         * @see #withWeightedPathTermination(double, NodeWeightDeterminationFunction)
         * @param terminationWeightBarrier
         * @param nodeWeightByRouteDeterminationFunction
         * @return
         */
        public Traversal withWeightedPathTerminationByBranchesAndRoute(double terminationWeightBarrier,
                                                                       IsolatedNodeWeightByRouteDeterminationFunction nodeWeightByRouteDeterminationFunction);

        public static interface NodeWeightDeterminationFunction
        {
            public double apply(Node node, Route route, OptionalDouble parentWeight);
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
         * {@link Stream} of the routes returned by the traversal
         * 
         * @return
         */
        public Stream<RouteAndTraversalControl> routes();

        public Hierarchy asHierarchy();

    }

    public static interface Hierarchy extends Supplier<Stream<HierarchicalNode>>
    {

        public String asJson();

        public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer);

    }

    public static interface DataBuilder
    {
        public DataBuilder put(String key, Object value);
    }

    public static interface HierarchicalNode extends Supplier<Node>
    {
        public Optional<Edge> getEdge();

        public Stream<HierarchicalNode> getChildren();
    }

    public static interface TraversalRoutesConsumer extends Consumer<Routes>
    {
    }

    public static enum Direction
    {
        OUTGOING, INCOMING;

        /**
         * Returns the inverse {@link Direction}
         * 
         * @return
         */
        public Direction inverse()
        {
            return OUTGOING.equals(this) ? Direction.INCOMING : Direction.OUTGOING;
        }
    }

    public static interface TraversalRoutes extends Streamable<RouteAndTraversalControl>
    {
    }

    public static interface RouteAndTraversalControl extends Supplier<Route>
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
    }

    public static interface Routes extends Streamable<Route>
    {

        public int size();

        /**
         * Returns the first {@link Route}
         * 
         * @return
         */
        @Override
        public Optional<Route> first();

        public boolean hasNoRoutes();

        public boolean hasRoutes();

    }

    public static interface SimpleRoute extends Streamable<Node>
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

    public static interface Route extends SimpleRoute
    {
        public Optional<TraversedEdge> lastEdge();

        public Optional<TraversedEdge> firstEdge();

        public TraversedEdges edges();

    }

    /**
     * A traversed {@link Edge} where #getNextNode() returns the next node of the traversal and #getPreviousNode()
     * returns the previous node in the traversal.<br>
     * <br>
     * #getEdge() returns the orginal {@link Edge} from the {@link Graph}. This edge is in the original direction and not aligned to the traversal direction.
     * <br>
     * <br>
     * Implements the {@link #hashCode()} and {@link #equals(Object)} redirecting to the underlying {@link #getEdge()}
     * 
     * @author omnaest
     */
    public static interface TraversedEdge
    {
        public Node getNextNode();

        public Node getPreviousNode();

        public Edge getEdge();
    }

    public static interface TraversedEdges extends Streamable<TraversedEdge>
    {
    }

}
