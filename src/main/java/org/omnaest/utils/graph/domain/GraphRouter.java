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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.omnaest.utils.stream.Streamable;

public interface GraphRouter
{
    public RoutingStrategy withBreadthFirst();

    public static interface RoutingStrategy
    {
        public Routes findAllOutgoingRoutesBetween(NodeIdentity from, NodeIdentity to);

        public Routes findAllIncomingRoutesBetween(NodeIdentity from, NodeIdentity to);

        /**
         * Disables the lazy loading of {@link Node}s and relies only of already resolved {@link Node}s
         * 
         * @return
         */
        public RoutingStrategy withDisabledNodeResolving();

        public RoutingStrategy withDisabledNodeResolving(boolean disabledNodeResolving);

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
    }

    public static interface TraversalRoutesConsumer extends Consumer<Routes>
    {
    }

    public static enum Direction
    {
        OUTGOING, INCOMING
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

    public static interface Route extends Streamable<Node>
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

        public Optional<Edge> lastEdge();

        public boolean isCyclic();

        /**
         * Adds a new {@link NodeIdentity} to the existing {@link Route} nodes and returns a new {@link Route} instance with that appended {@link NodeIdentity}.
         * 
         * @param nodeIdentity
         * @return
         */
        public Route addToNew(NodeIdentity nodeIdentity);
    }

}
