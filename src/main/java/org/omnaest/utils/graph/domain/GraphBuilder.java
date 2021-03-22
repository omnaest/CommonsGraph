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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.utils.SetUtils;

public interface GraphBuilder
{

    /**
     * Adds an edge from a {@link Node} with the given {@link NodeIdentity} to another. This added edge is directed. If a bidirectional edge should be added
     * consider using {@link #addBidirectionalEdge(NodeIdentity, NodeIdentity)} instead.
     * 
     * @param from
     * @param to
     * @return
     */
    public GraphBuilder addEdge(NodeIdentity from, NodeIdentity to);

    /**
     * Similar to {@link #addEdge(NodeIdentity, NodeIdentity)} using a predefined {@link EdgeIdentity}
     * 
     * @see EdgeIdentity#of(NodeIdentity, NodeIdentity)
     * @param edgeIdentity
     * @return
     */
    public GraphBuilder addEdge(EdgeIdentity edgeIdentity);

    /**
     * Adds the forward and backward directed edges
     * 
     * @param from
     * @param to
     * @return
     */
    public GraphBuilder addBidirectionalEdge(NodeIdentity from, NodeIdentity to);

    /**
     * Adds a {@link Node} with the given {@link NodeIdentity} to the {@link Graph}
     * 
     * @param nodeIdentity
     * @return
     */
    public GraphBuilder addNode(NodeIdentity nodeIdentity);

    public GraphBuilder addNodes(Collection<NodeIdentity> nodeIdentities);

    public GraphBuilder withSingleNodeResolver(SingleNodeResolver nodeResolver);

    public GraphBuilder withMultiNodeResolver(MultiNodeResolver nodeResolver);

    /**
     * Builds a {@link Graph} instance
     * 
     * @return
     */
    public Graph build();

    /**
     * Marker interface for {@link NodeResolver}s
     * 
     * @see SingleNodeResolver
     * @see MultiNodeResolver
     * @author omnaest
     */
    public static interface NodeResolver
    {

    }

    public static interface SingleNodeResolver extends Function<NodeIdentity, Set<EdgeIdentity>>, NodeResolver
    {
        public default MultiNodeResolver asMultiNodeResolver()
        {
            return ids -> Optional.ofNullable(ids)
                                  .orElse(Collections.emptySet())
                                  .stream()
                                  .map(this)
                                  .flatMap(Set::stream)
                                  .collect(Collectors.toSet());
        }
    }

    public static interface MultiNodeResolver extends Function<Set<NodeIdentity>, Set<EdgeIdentity>>, NodeResolver
    {
        public default SingleNodeResolver asSingleNodeResolver()
        {
            return nodeIdentity -> this.apply(SetUtils.toSet(nodeIdentity));
        }
    }

    public static class EdgeIdentity
    {
        private NodeIdentity from;
        private NodeIdentity to;

        protected EdgeIdentity(NodeIdentity from, NodeIdentity to)
        {
            super();
            this.from = from;
            this.to = to;
        }

        public NodeIdentity getFrom()
        {
            return from;
        }

        public NodeIdentity getTo()
        {
            return to;
        }

        public static EdgeIdentity of(NodeIdentity from, NodeIdentity to)
        {
            return new EdgeIdentity(from, to);
        }

        @Override
        public String toString()
        {
            return "EdgeIdentity [from=" + from + ", to=" + to + "]";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
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
            if (getClass() != obj.getClass())
            {
                return false;
            }
            EdgeIdentity other = (EdgeIdentity) obj;
            if (from == null)
            {
                if (other.from != null)
                {
                    return false;
                }
            }
            else if (!from.equals(other.from))
            {
                return false;
            }
            if (to == null)
            {
                if (other.to != null)
                {
                    return false;
                }
            }
            else if (!to.equals(other.to))
            {
                return false;
            }
            return true;
        }

    }
}
