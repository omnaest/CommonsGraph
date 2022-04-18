package org.omnaest.utils.graph.internal.index;

import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.Graph.GraphTokenIndex;
import org.omnaest.utils.graph.domain.Graph.GraphTokenIndexBuilder;
import org.omnaest.utils.graph.domain.Graph.NodeInclusionFilter;
import org.omnaest.utils.graph.domain.Graph.NodeToTokensMapper;
import org.omnaest.utils.graph.domain.Graph.TokenMapper;
import org.omnaest.utils.graph.domain.Graph.TypedGraphIndex;
import org.omnaest.utils.graph.domain.node.Node;

public class GraphTokenIndexBuilderImpl implements GraphTokenIndexBuilder
{
    private TypedGraphIndexBuilderImpl<String> builder;

    public GraphTokenIndexBuilderImpl(Graph graph, NodeInclusionFilter nodeInclusionFilter)
    {
        super();
        this.builder = new TypedGraphIndexBuilderImpl<>(graph, nodeInclusionFilter);
    }

    @Override
    public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper)
    {
        this.builder.withNodeToKeyMapper(nodeToTokensMapper);
        return this;
    }

    @Override
    public GraphTokenIndexBuilder withNodeToTokenMapper(NodeToTokensMapper nodeToTokensMapper, TokenMapper tokenMapper)
    {
        this.builder.withNodeToKeyMapper(nodeToTokensMapper, tokenMapper);
        return this;
    }

    @Override
    public GraphTokenIndexBuilder withNodeIdentitiesTokenMapper()
    {
        return this.withNodeToTokenMapper(nodeToNodeIdentitiesMapper());
    }

    @Override
    public GraphTokenIndex get()
    {
        TypedGraphIndex<String> index = this.builder.get();
        return new GraphTokenIndex()
        {
            @Override
            public Stream<Node> apply(String token)
            {
                return index.apply(token);
            }
        };
    }

    private static NodeToTokensMapper nodeToNodeIdentitiesMapper()
    {
        return node -> node.getIdentity()
                           .get()
                           .stream();
    }

    private static TokenMapper ignoreCaseTokenMapper()
    {
        return token -> Stream.of(StringUtils.lowerCase(token));
    }

    @Override
    public GraphTokenIndexBuilder withNodeIdentitiesTokenMapperIgnoringCase()
    {
        return this.withNodeToTokenMapper(nodeToNodeIdentitiesMapper(), ignoreCaseTokenMapper());
    }

}