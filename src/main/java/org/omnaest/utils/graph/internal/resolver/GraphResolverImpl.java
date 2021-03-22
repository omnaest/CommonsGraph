package org.omnaest.utils.graph.internal.resolver;

import org.omnaest.utils.graph.domain.GraphResolver;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class GraphResolverImpl implements GraphResolver
{
    private final GraphIndex          graphIndex;
    private final NodeResolverSupport nodeResolverSupport;
    private int                       depthLimit = Integer.MAX_VALUE;

    public GraphResolverImpl(GraphIndex graphIndex, NodeResolverSupport nodeResolverSupport)
    {
        this.graphIndex = graphIndex;
        this.nodeResolverSupport = nodeResolverSupport;
    }

    @Override
    public GraphResolver resolveAll()
    {
        int depth = 0;
        while (this.graphIndex.hasUnresolvedNodes() && depth < this.depthLimit)
        {
            this.nodeResolverSupport.resolve(this.graphIndex.getUnresolvedNodes());
            depth++;
        }
        return this;

    }

    @Override
    public GraphResolver withDepthLimit(int depthLimit)
    {
        this.depthLimit = depthLimit;
        return this;
    }
}