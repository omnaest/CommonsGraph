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

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;

/**
 * Mutable working-model threaded through the layout engine phases for a single {@link org.omnaest.utils.graph.layout.LayeredGraphLayout#apply}
 * call. Never shared across calls and never derived from anything but copies, so the source {@link org.omnaest.utils.graph.layout.domain.LayoutGraph}
 * is never mutated.
 *
 * @author omnaest
 */
final class LayoutModel
{
    private final LayoutOptions                   options;
    private final List<WorkNode>                  nodes          = new ArrayList<>();
    private final Map<LayoutNodeId, WorkNode>     nodesById      = new LinkedHashMap<>();
    private final List<WorkEdge>                  edges          = new ArrayList<>();
    private final List<WorkEdge>                  selfLoopEdges  = new ArrayList<>();

    private int                                   maxLayer       = 0;
    private List<List<LayoutNodeId>>              layerOrder     = Collections.emptyList();
    private Map<LayoutNodeId, List<LayoutNodeId>> upperAdjacency = Collections.emptyMap();
    private Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency = Collections.emptyMap();

    LayoutModel(LayoutOptions options)
    {
        this.options = options;
    }

    LayoutOptions getOptions()
    {
        return this.options;
    }

    void addNode(WorkNode node)
    {
        this.nodes.add(node);
        this.nodesById.put(node.getId(), node);
    }

    WorkNode getNode(LayoutNodeId id)
    {
        return this.nodesById.get(id);
    }

    List<WorkNode> getAllNodes()
    {
        return this.nodes;
    }

    /**
     * Returns all currently known nodes sorted by id - used where the algorithm needs a deterministic starting order that
     * does not depend on insertion order (e.g. picking DFS start nodes).
     */
    List<WorkNode> getAllNodesSortedById()
    {
        List<WorkNode> sorted = new ArrayList<>(this.nodes);
        sorted.sort((a, b) -> a.getId()
                               .compareTo(b.getId()));
        return sorted;
    }

    void addEdge(WorkEdge edge)
    {
        this.edges.add(edge);
    }

    List<WorkEdge> getEdges()
    {
        return this.edges;
    }

    void addSelfLoopEdge(WorkEdge edge)
    {
        this.selfLoopEdges.add(edge);
    }

    List<WorkEdge> getSelfLoopEdges()
    {
        return this.selfLoopEdges;
    }

    int getMaxLayer()
    {
        return this.maxLayer;
    }

    void setMaxLayer(int maxLayer)
    {
        this.maxLayer = maxLayer;
    }

    List<List<LayoutNodeId>> getLayerOrder()
    {
        return this.layerOrder;
    }

    void setLayerOrder(List<List<LayoutNodeId>> layerOrder)
    {
        this.layerOrder = layerOrder;
    }

    Map<LayoutNodeId, List<LayoutNodeId>> getUpperAdjacency()
    {
        return this.upperAdjacency;
    }

    void setUpperAdjacency(Map<LayoutNodeId, List<LayoutNodeId>> upperAdjacency)
    {
        this.upperAdjacency = upperAdjacency;
    }

    Map<LayoutNodeId, List<LayoutNodeId>> getLowerAdjacency()
    {
        return this.lowerAdjacency;
    }

    void setLowerAdjacency(Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency)
    {
        this.lowerAdjacency = lowerAdjacency;
    }

}
