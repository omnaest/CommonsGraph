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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.omnaest.utils.graph.layout.domain.LayoutEdge;
import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutNode;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Sole implementation of {@link LayoutGraph}. Immutable snapshot of the nodes/edges handed to the builder, insertion order
 * preserved throughout (see the layout engine's determinism requirement).
 *
 * @author omnaest
 */
public class LayoutGraphImpl implements LayoutGraph
{
    private final List<LayoutNode>              nodes;
    private final List<LayoutEdge>              edges;
    private final Map<LayoutNodeId, LayoutNode> nodesById;

    public LayoutGraphImpl(List<LayoutNode> nodes, List<LayoutEdge> edges)
    {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));

        Map<LayoutNodeId, LayoutNode> byId = new LinkedHashMap<>();
        for (LayoutNode node : this.nodes)
        {
            byId.put(node.getId(), node);
        }
        this.nodesById = Collections.unmodifiableMap(byId);
    }

    @Override
    public List<LayoutNode> getNodes()
    {
        return this.nodes;
    }

    @Override
    public List<LayoutEdge> getEdges()
    {
        return this.edges;
    }

    @Override
    public Optional<LayoutNode> findNodeById(LayoutNodeId id)
    {
        return Optional.ofNullable(this.nodesById.get(id));
    }

}
