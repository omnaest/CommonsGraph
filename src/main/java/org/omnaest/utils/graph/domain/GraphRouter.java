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

import java.util.List;
import java.util.Optional;

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
    }

    public static interface Routes extends Streamable<Route>
    {

        public int size();

        /**
         * Returns the first {@link Route}
         * 
         * @return
         */
        public Optional<Route> first();

        public boolean hasNoRoutes();

        public boolean hasRoutes();

    }

    public static interface Route extends Streamable<Node>
    {

        public List<NodeIdentity> toNodeIdentities();

    }
}
