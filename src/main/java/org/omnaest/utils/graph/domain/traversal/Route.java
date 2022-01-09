package org.omnaest.utils.graph.domain.traversal;

import java.util.Optional;

public interface Route extends SimpleRoute
{
    public Optional<TraversedEdge> lastEdge();

    public Optional<TraversedEdge> firstEdge();

    public TraversedEdges edges();

}