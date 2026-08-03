/*******************************************************************************
 * Copyright 2026 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.utils.graph.layout.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.omnaest.utils.graph.layout.domain.LayoutEdge;
import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutGraphBuilder;
import org.omnaest.utils.graph.layout.domain.LayoutNode;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.Size;

/**
 * Sole implementation of {@link LayoutGraphBuilder}.
 *
 * @author omnaest
 */
public class LayoutGraphBuilderImpl implements LayoutGraphBuilder
{
    private final List<LayoutNode> nodes = new ArrayList<>();
    private final List<LayoutEdge> edges = new ArrayList<>();

    @Override
    public LayoutGraphBuilder addNode(LayoutNode node)
    {
        this.nodes.add(Objects.requireNonNull(node, "node must not be null"));
        return this;
    }

    @Override
    public LayoutGraphBuilder addNode(LayoutNodeId id, Size size)
    {
        return this.addNode(LayoutNode.of(id, size));
    }

    @Override
    public LayoutGraphBuilder addEdge(LayoutEdge edge)
    {
        this.edges.add(Objects.requireNonNull(edge, "edge must not be null"));
        return this;
    }

    @Override
    public LayoutGraphBuilder addEdge(LayoutEdgeId id, LayoutNodeId from, LayoutNodeId to)
    {
        return this.addEdge(LayoutEdge.of(id, from, to));
    }

    @Override
    public LayoutGraph build()
    {
        return new LayoutGraphImpl(this.nodes, this.edges);
    }

}
