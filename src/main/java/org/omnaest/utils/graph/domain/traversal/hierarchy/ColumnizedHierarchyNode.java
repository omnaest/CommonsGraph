package org.omnaest.utils.graph.domain.traversal.hierarchy;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface ColumnizedHierarchyNode extends Supplier<HierarchicalNode>
{
    /**
     * Returns the column index = 0,1,2,...
     *
     * @return
     */
    public int getColumnIndex();

    public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer);
}
