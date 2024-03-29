package org.omnaest.utils.graph.internal.resolver;

import org.omnaest.utils.graph.domain.GraphResolver;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.data.GraphDataIndexAccessor;

public class GraphResolverImpl implements GraphResolver
{
    private final GraphDataIndexAccessor  graphIndexAccessor;
    private final NodeResolverSupport nodeResolverSupport;
    private int                       depthLimit = Integer.MAX_VALUE;

    public GraphResolverImpl(GraphDataIndexAccessor graphIndexAccessor, NodeResolverSupport nodeResolverSupport)
    {
        this.graphIndexAccessor = graphIndexAccessor;
        this.nodeResolverSupport = nodeResolverSupport;
    }

    @Override
    public GraphResolver resolveAll()
    {
        int depth = 0;
        while (this.graphIndexAccessor.hasUnresolvedNodes() && depth < this.depthLimit)
        {
            this.nodeResolverSupport.resolve(this.graphIndexAccessor.getUnresolvedNodes());
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