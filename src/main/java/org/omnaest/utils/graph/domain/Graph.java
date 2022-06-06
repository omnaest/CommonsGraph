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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.utils.graph.GraphUtils;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.edge.Edges;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.node.Nodes;
import org.omnaest.utils.stream.Streamable;

/**
 * A {@link Graph} with {@link Node}s
 * 
 * @see GraphUtils#builder()
 * @author omnaest
 */
public interface Graph extends Streamable<Node>
{
    /**
     * Returns a {@link Node} for a given {@link NodeIdentity} of the {@link Graph}. If the {@link Graph} does not contain the {@link NodeIdentity} then
     * {@link Optional#empty()} is returned, but never null.
     * 
     * @param nodeIdentity
     * @return
     */
    public Optional<Node> findNodeById(NodeIdentity nodeIdentity);

    /**
     * Similar to {@link #findNodeById(NodeIdentity)} but for a batch of {@link NodeIdentity}s
     * 
     * @see Nodes
     * @param nodeIdentities
     * @return
     */
    public Nodes findNodesByIds(Collection<NodeIdentity> nodeIdentities);

    /**
     * Similar to {@link #findNodesByIds(Collection)}
     * 
     * @param nodeIdentities
     * @return
     */
    public Nodes findNodesByIds(NodeIdentity... nodeIdentities);

    public GraphRouter routing();

    public int size();

    /**
     * @see GraphResolver
     * @return
     */
    public GraphResolver resolver();

    /**
     * Clones the current {@link Graph} instance into a new in memory {@link Graph}
     * 
     * @return
     */
    public Graph clone();

    /**
     * Returns a {@link GraphSerializer} for this {@link Graph} instance.
     * 
     * @return
     */
    public GraphSerializer serialize();

    public GraphIndexNodeSelector index();

    public static interface GraphIndexNodeSelector
    {
        public GraphIndexBuilder forAllNodes();

        public GraphIndexBuilder forNodes(NodeInclusionFilter nodeInclusionFilter);
    }

    public static interface NodeInclusionFilter extends Predicate<Node>
    {
    }

    public static interface GraphIndexBuilder extends GraphTokenIndexBuilderBase
    {
        public <K> TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper);

        public <K> TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper, KeyMapper<K> keyMapper);
    }

    public static interface GraphTokenIndexBuilderBase
    {
        public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper);

        public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper, TokenMapper tokenMapper);

        public GraphTokenIndexBuilder withNodeIdentitiesTokenMapper();

        public GraphTokenIndexBuilder withNodeIdentitiesTokenMapperIgnoringCase();
    }

    public static interface GraphTokenIndexBuilder extends GraphTokenIndexBuilderBase, Supplier<GraphTokenIndex>
    {
        @Override
        public GraphTokenIndex get();

    }

    public static interface TypedGraphIndexBuilder<K> extends Supplier<TypedGraphIndex<K>>
    {
        public TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper);

        public TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper, KeyMapper<K> keyMapper);

        @Override
        public TypedGraphIndex<K> get();
    }

    public static interface NodeToKeysMapper<K> extends Function<Node, Stream<K>>
    {
    }

    public static interface KeyMapper<K> extends Function<K, Stream<K>>
    {
    }

    public static interface TokenMapper extends KeyMapper<String>
    {
    }

    public static interface NodeToTokensMapper extends NodeToKeysMapper<String>
    {
    }

    public static interface TypedGraphIndex<K> extends Function<K, Stream<Node>>
    {
        @Override
        public Stream<Node> apply(K key);
    }

    public static interface GraphTokenIndex extends TypedGraphIndex<String>
    {

        @Override
        public Stream<Node> apply(String token);

    }

    /**
     * Finds an {@link Edge} from one {@link NodeIdentity} to another. Be aware that the edge is direction dependent.
     * 
     * @param from
     * @param to
     * @return
     */
    public Optional<Edge> findEdge(NodeIdentity from, NodeIdentity to);

    /**
     * Similar to {@link #findEdge(NodeIdentity, NodeIdentity)}
     * 
     * @param edgeIdentity
     * @return
     */
    public Optional<Edge> findEdge(EdgeIdentity edgeIdentity);

    /**
     * Creates a {@link SubGraphBuilder} that allows to define a view on top of the current {@link Graph}.
     * 
     * @return
     */
    public SubGraphBuilder subGraph();

    public static interface SubGraphBuilder
    {
        public SubGraphBuilder withExcludedNodes(NodeIdentity... nodeIdentities);

        public SubGraphBuilder withExcludedNodes(Collection<NodeIdentity> nodeIdentities);

        public SubGraphBuilder withIncludedNodes(NodeIdentity... nodeIdentities);

        public SubGraphBuilder withIncludedNodes(Collection<NodeIdentity> nodeIdentities);

        public Graph build();
    }

    public Edges edges();

    public Nodes nodes();

    public Nodes startNodes();

    public Nodes endNodes();

    public boolean contains(NodeIdentity nodeIdentity);

    public boolean containsAny(NodeIdentity... nodeIdentities);

    public GraphTransformer transform();

    public static interface GraphTransformer
    {
        public GraphTransformer filter(Predicate<Node> nodeInclusionFilter);

        public GraphTransformer addNodes(Iterable<NodeIdentity> nodes);

        public GraphTransformer addNodes(NodeIdentity... nodes);

        public GraphTransformer addEdges(Iterable<EdgeIdentity> edges);

        public Graph collect();

        public GraphTransformer addEdge(EdgeIdentity edge);

        public GraphTransformer addEdge(NodeIdentity from, NodeIdentity to);

        public GraphTransformer addNode(NodeIdentity node);

        /**
         * Maps a given {@link Node}. The edge connections to the current node are remapped to the {@link NodeIdentity} returned by the mapper function.
         * 
         * @param mapper
         * @return
         */
        public GraphTransformer map(Function<Node, NodeIdentity> mapper);

    }

    public boolean isNotEmpty();

    public boolean isEmpty();

}
