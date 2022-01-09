package org.omnaest.utils.graph.domain.traversal.hierarchy;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.omnaest.utils.graph.internal.router.strategy.BreadthFirstRoutingStrategy.ColumnizedHierarchyNode;
import org.omnaest.utils.stream.Streamable;

/**
 * @see #stream()
 * @author omnaest
 */
public interface Hierarchy extends Streamable<HierarchicalNode>
{
    /**
     * Returns the root {@link HierarchicalNode}s.
     */
    @Override
    public Stream<HierarchicalNode> stream();

    public String asJson();

    public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer);

    public Stream<ColumnizedHierarchyNode> asColumnizedNodes();

}