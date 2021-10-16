package org.omnaest.utils.graph.domain;

import org.omnaest.utils.stream.Streamable;

/**
 * Batch of {@link Edge}s
 * 
 * @author omnaest
 */
public interface Edges extends Streamable<Edge>
{
    public int size();

}
