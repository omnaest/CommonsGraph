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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Algorithm step 3: longest-path layering over the now-acyclic edge set. layer(v) = 0 if v has no incoming edge, else
 * max(layer(u)) + 1 over incoming edges - computed via a Kahn topological relaxation so it terminates in one pass and never
 * depends on map/set iteration order (a {@link TreeSet} keyed on {@link LayoutNodeId}'s natural ordering drives processing
 * order).
 *
 * @author omnaest
 */
final class LongestPathLayeringPhase
{
    private LongestPathLayeringPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        List<WorkNode> realNodes = model.getAllNodesSortedById();

        Map<LayoutNodeId, Integer> inDegree = new LinkedHashMap<>();
        Map<LayoutNodeId, List<WorkEdge>> outAdjacency = new LinkedHashMap<>();
        Map<LayoutNodeId, Integer> layer = new LinkedHashMap<>();
        for (WorkNode node : realNodes)
        {
            inDegree.put(node.getId(), 0);
            outAdjacency.put(node.getId(), new ArrayList<>());
            layer.put(node.getId(), 0);
        }
        for (WorkEdge edge : model.getEdges())
        {
            outAdjacency.get(edge.getLayoutFrom())
                        .add(edge);
            inDegree.merge(edge.getLayoutTo(), 1, Integer::sum);
        }
        Comparator<WorkEdge> edgeOrder = Comparator.<WorkEdge, String>comparing(edge -> edge.getLayoutTo()
                                                                                            .getValue())
                                                   .thenComparing(edge -> edge.getId()
                                                                              .getValue());
        for (List<WorkEdge> outgoing : outAdjacency.values())
        {
            outgoing.sort(edgeOrder);
        }

        Map<LayoutNodeId, Integer> remainingInDegree = new LinkedHashMap<>(inDegree);
        TreeSet<LayoutNodeId> ready = new TreeSet<>();
        for (WorkNode node : realNodes)
        {
            if (inDegree.get(node.getId()) == 0)
            {
                ready.add(node.getId());
            }
        }

        while (!ready.isEmpty())
        {
            LayoutNodeId u = ready.pollFirst();
            int uLayer = layer.get(u);
            for (WorkEdge edge : outAdjacency.get(u))
            {
                LayoutNodeId w = edge.getLayoutTo();
                int candidate = uLayer + 1;
                if (candidate > layer.get(w))
                {
                    layer.put(w, candidate);
                }
                int remaining = remainingInDegree.get(w) - 1;
                remainingInDegree.put(w, remaining);
                if (remaining == 0)
                {
                    ready.add(w);
                }
            }
        }

        int maxLayer = 0;
        for (WorkNode node : realNodes)
        {
            int nodeLayer = layer.get(node.getId());
            node.setLayer(nodeLayer);
            maxLayer = Math.max(maxLayer, nodeLayer);
        }
        model.setMaxLayer(maxLayer);
    }

}
