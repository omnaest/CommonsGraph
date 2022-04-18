package org.omnaest.utils.graph.internal.index;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.Graph.GraphIndexBuilder;
import org.omnaest.utils.graph.domain.Graph.GraphIndexNodeSelector;
import org.omnaest.utils.graph.domain.Graph.GraphTokenIndexBuilder;
import org.omnaest.utils.graph.domain.Graph.KeyMapper;
import org.omnaest.utils.graph.domain.Graph.NodeInclusionFilter;
import org.omnaest.utils.graph.domain.Graph.NodeToKeysMapper;
import org.omnaest.utils.graph.domain.Graph.NodeToTokensMapper;
import org.omnaest.utils.graph.domain.Graph.TokenMapper;
import org.omnaest.utils.graph.domain.Graph.TypedGraphIndexBuilder;

public class GraphIndexNodeSelectorImpl implements GraphIndexNodeSelector
{
    private final Graph graph;

    public GraphIndexNodeSelectorImpl(Graph graph)
    {
        this.graph = graph;
    }

    @Override
    public GraphIndexBuilder forNodes(NodeInclusionFilter nodeInclusionFilter)
    {
        return new GraphIndexBuilder()
        {
            @Override
            public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper)
            {
                return new GraphTokenIndexBuilderImpl(GraphIndexNodeSelectorImpl.this.graph, nodeInclusionFilter).withNodeToTokenMapper(nodeToTokensMapper);
            }

            @Override
            public <K> TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper)
            {
                return new TypedGraphIndexBuilderImpl<K>(GraphIndexNodeSelectorImpl.this.graph, nodeInclusionFilter).withNodeToKeyMapper(nodeToKeysMapper);
            }

            @Override
            public <K> TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper, KeyMapper<K> keyMapper)
            {
                return new TypedGraphIndexBuilderImpl<K>(GraphIndexNodeSelectorImpl.this.graph, nodeInclusionFilter).withNodeToKeyMapper(nodeToKeysMapper,
                                                                                                                                         keyMapper);
            }

            @Override
            public GraphTokenIndexBuilder withNodeIdentitiesTokenMapper()
            {
                return new GraphTokenIndexBuilderImpl(GraphIndexNodeSelectorImpl.this.graph, nodeInclusionFilter).withNodeIdentitiesTokenMapper();
            }

            @Override
            public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper, TokenMapper tokenMapper)
            {
                return new GraphTokenIndexBuilderImpl(GraphIndexNodeSelectorImpl.this.graph, nodeInclusionFilter).withNodeToTokenMapper(nodeToTokensMapper,
                                                                                                                                        tokenMapper);
            }

            @Override
            public GraphTokenIndexBuilder withNodeIdentitiesTokenMapperIgnoringCase()
            {
                return new GraphTokenIndexBuilderImpl(GraphIndexNodeSelectorImpl.this.graph,
                                                      nodeInclusionFilter).withNodeIdentitiesTokenMapperIgnoringCase();
            }

        };
    }

    @Override
    public GraphIndexBuilder forAllNodes()
    {
        return this.forNodes(node -> true);
    }
}