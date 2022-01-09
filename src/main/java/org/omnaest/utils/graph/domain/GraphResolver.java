package org.omnaest.utils.graph.domain;

import org.omnaest.utils.graph.domain.node.Node;

/**
 * A {@link GraphResolver} allows to resolve {@link Node}s which are lazy loaded.
 * 
 * @author omnaest
 */
public interface GraphResolver
{
    /**
     * Resolves all unresolved {@link Node}s if lazy loading is enabled
     * 
     * @return
     */
    public GraphResolver resolveAll();

    public GraphResolver withDepthLimit(int depthLimit);
}