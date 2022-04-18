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
import java.util.Set;
import java.util.function.Predicate;

import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.traversal.Direction;
import org.omnaest.utils.graph.domain.traversal.Routes;
import org.omnaest.utils.graph.domain.traversal.Traversal;

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

        public Traversal traverseIncoming(NodeIdentity... startNodes);

        public Traversal traverseIncoming(Collection<NodeIdentity> startNodes);

        public Traversal traverseIncoming();

        public Traversal traverseOutgoing(Collection<NodeIdentity> startNodes);

        public Traversal traverseOutgoing(NodeIdentity... startNodes);

        public Traversal traverseOutgoing();

        public Traversal traverse(Direction direction, NodeIdentity... startNodes);

        public Traversal traverse(Direction direction, Set<NodeIdentity> startNodes);

        /**
         * Traverses the {@link Graph} but keeps the subgraph traversal from all startnodes in a distinct context, which means the same nodes and pathes can be
         * traversed twice, if they are on a route that originates from at least two start nodes.
         * 
         * @see Traversal#withOwnContext()
         * @param direction
         * @param startNodes
         * @return
         */
        public Traversal traverseEach(Direction direction, NodeIdentity... startNodes);

        public Traversal traverseEach(Direction direction, Collection<NodeIdentity> startNodes);

    }

}
