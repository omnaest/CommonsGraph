package org.omnaest.utils.graph.domain.traversal.hierarchy;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.node.Node;

public interface HierarchicalNode extends Supplier<Node>
{
    public Optional<Edge> getEdge();

    public Stream<HierarchicalNode> getChildren();
}