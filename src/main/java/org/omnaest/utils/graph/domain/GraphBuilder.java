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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.SetUtils;
import org.omnaest.utils.functional.TriFunction;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

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
     * Similar to {@link #addEdge(EdgeIdentity)} for a batch of {@link EdgeIdentity}s
     * 
     * @param edgeIdentities
     * @return
     */
    public GraphBuilder addEdges(Iterable<EdgeIdentity> edgeIdentities);

    /**
     * Similar to {@link #addEdgeWithAttributes(NodeIdentity, NodeIdentity, Collection)} for a batch of {@link EdgeIdentityWithAttributes}
     * 
     * @param edgeIdentities
     * @return
     */
    public GraphBuilder addEdgesWithAttributes(Iterable<EdgeIdentityWithAttributes> edgeIdentities);

    /**
     * Similar to {@link #addEdge(NodeIdentity, NodeIdentity)} but allows to bind a given {@link Collection} of {@link Attribute}s to an edge.
     * 
     * @param from
     * @param to
     * @param attributes
     * @return
     */
    public GraphBuilder addEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes);

    /**
     * Similar to {@link #addEdgeWithAttributes(NodeIdentity, NodeIdentity, Collection)}
     * 
     * @param from
     * @param to
     * @param attributes
     * @return
     */
    public GraphBuilder addEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Attribute... attributes);

    /**
     * Adds a given list of elements that have children to {@link NodeIdentity}s and {@link EdgeIdentity}s by the given mapping {@link Function}s
     * 
     * @param elements
     * @param elementChildrenExtractor
     * @param elementToNodeIdentityMapper
     * @return
     */
    public <E> GraphBuilder addElementsWithChildren(Collection<E> elements, Function<E, Collection<E>> elementChildrenExtractor,
                                                    Function<E, NodeIdentity> elementToNodeIdentityMapper);

    /**
     * Similar to {@link #addElementsWithChildren(Collection, Function, Function)}
     * 
     * @param elements
     * @param elementChildrenExtractor
     * @param elementToNodeIdentityMapper
     * @return
     */
    public <E> GraphBuilder addElementsWithChildren(Stream<E> elements, Function<E, Stream<E>> elementChildrenExtractor,
                                                    Function<E, NodeIdentity> elementToNodeIdentityMapper);

    /**
     * Adds the forward and backward directed edges
     * 
     * @param from
     * @param to
     * @return
     */
    public GraphBuilder addBidirectionalEdge(NodeIdentity from, NodeIdentity to);

    public GraphBuilder addBidirectionalEdgeWithTags(NodeIdentity from, NodeIdentity to, Collection<Tag> tags);

    public GraphBuilder addBidirectionalEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes);

    public GraphBuilder addBidirectionalEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Attribute... attributes);

    /**
     * Adds a {@link Node} with the given {@link NodeIdentity} to the {@link Graph}
     * 
     * @param nodeIdentity
     * @return
     */
    public GraphBuilder addNode(NodeIdentity nodeIdentity);

    public GraphBuilder addNodeWithData(NodeIdentity nodeIdentity, Consumer<NodeDataBuilder> nodeDataBuilderConsumer);

    public GraphBuilder addNodes(Collection<NodeIdentity> nodeIdentities);

    public GraphBuilder addNodes(NodeIdentity... nodeIdentities);

    public GraphBuilder withSingleNodeResolver(SingleNodeResolver nodeResolver);

    public GraphBuilder withBidirectionalSingleNodeResolver(SingleNodeResolver nodeResolver);

    public GraphBuilder withMultiNodeResolver(MultiNodeResolver nodeResolver);

    /**
     * Adds another {@link Graph} with all {@link Node}s and {@link Edge}s
     * 
     * @param graph
     * @return
     */
    public GraphBuilder addGraph(Graph graph);

    /**
     * Builds a {@link Graph} instance
     * 
     * @return
     */
    public Graph build();

    /**
     * Defines the underlying repositories that are created to host the {@link Graph}. This {@link RepositoryProvider} has to be defined before any
     * {@link NodeIdentity} is added to the {@link GraphBuilder}.
     * 
     * @param repositoryProvider
     * @return
     */
    public GraphBuilder withRepositoryProvider(RepositoryProvider repositoryProvider);

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

        /**
         * Returns a wrapping {@link MultiNodeResolver} which duplicates all the current {@link EdgeIdentity}s into their own and their
         * {@link EdgeIdentity#inverse()} edges
         * 
         * @return
         */
        public default MultiNodeResolver asBidirectionalMultiNodeResolver()
        {
            return ids -> Optional.ofNullable(ids)
                                  .map(this::apply)
                                  .map(forwardEdges -> Stream.concat(forwardEdges.stream(), forwardEdges.stream()
                                                                                                        .map(EdgeIdentity::inverse)))
                                  .map(edges -> edges.collect(Collectors.toSet()))
                                  .orElse(Collections.emptySet());
        }
    }

    public static class EdgeIdentityWithAttributes
    {
        private EdgeIdentity          edgeIdentity;
        private Collection<Attribute> attributes;

        private EdgeIdentityWithAttributes(EdgeIdentity edgeIdentity, Collection<Attribute> attributes)
        {
            super();
            this.edgeIdentity = edgeIdentity;
            this.attributes = attributes;
        }

        public static EdgeIdentityWithAttributes of(NodeIdentity from, NodeIdentity to, Attribute... attributes)
        {
            return of(from, to, Optional.ofNullable(attributes)
                                        .map(Arrays::asList)
                                        .orElse(Collections.emptyList()));
        }

        public static EdgeIdentityWithAttributes of(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes)
        {
            return new EdgeIdentityWithAttributes(EdgeIdentity.of(from, to), attributes);
        }

        public EdgeIdentity getEdgeIdentity()
        {
            return edgeIdentity;
        }

        public Collection<Attribute> getAttributes()
        {
            return attributes;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("EdgeIdentityAndAttributes [edgeIdentity=")
                   .append(edgeIdentity)
                   .append(", attributes=")
                   .append(attributes)
                   .append("]");
            return builder.toString();
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

        /**
         * Returns a new {@link EdgeIdentity} with {@link #getFrom()} and {@link #getTo()} switched.
         * 
         * @return
         */
        public EdgeIdentity inverse()
        {
            return EdgeIdentity.of(to, from);
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

        /**
         * Returns a {@link Stream} containing the {@link #getFrom()} and {@link #getTo()} {@link NodeIdentity}s
         * 
         * @return
         */
        public Stream<NodeIdentity> stream()
        {
            return Stream.of(this.from, this.to);
        }

        /**
         * Returns true, if this {@link EdgeIdentity} contains the given {@link NodeIdentity} as from or to.
         * 
         * @param nodeIdentity
         * @return
         */
        public boolean contains(NodeIdentity nodeIdentity)
        {
            return Objects.equals(this.from, nodeIdentity) || Objects.equals(this.to, nodeIdentity);
        }

    }

    public static interface RepositoryProvider extends TriFunction<String, Class<?>, Class<?>, Map<?, ?>>
    {
        @SuppressWarnings("unchecked")
        public default <K, V> Map<K, V> createMap(String name)
        {
            return (Map<K, V>) this.apply(name, null, null);
        }

        public default <K> Set<K> createSet(String name)
        {
            return Collections.newSetFromMap(this.createMap(name));
        }
    }

    public static interface NodeDataBuilder
    {
        public NodeDataBuilder put(String key, Object value);

        public NodeDataBuilder putAll(Map<String, Object> map);

        public NodeDataBuilder clear();

        /**
         * Puts data based on the properties and property values of an {@link Object} instance
         * 
         * @param object
         * @return
         */
        public NodeDataBuilder putFrom(Object object);
    }

}
