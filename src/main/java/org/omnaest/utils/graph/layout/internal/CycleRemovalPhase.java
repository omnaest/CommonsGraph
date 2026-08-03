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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Algorithm step 2: DFS-based cycle removal. Back-edges (an edge whose target is currently on the DFS stack) are reversed in
 * place on the {@link WorkEdge} so the remainder of the pipeline sees an acyclic graph; {@link WorkEdge#isReversed()} records
 * which edges were flipped so their final waypoint list can be presented back in original order (step 10).
 *
 * @author omnaest
 */
final class CycleRemovalPhase
{
    private static final int WHITE = 0;
    private static final int GRAY  = 1;
    private static final int BLACK = 2;

    private CycleRemovalPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        List<WorkNode> nodesSortedById = model.getAllNodesSortedById();

        Map<LayoutNodeId, List<WorkEdge>> outAdjacency = new LinkedHashMap<>();
        for (WorkNode node : nodesSortedById)
        {
            outAdjacency.put(node.getId(), new ArrayList<>());
        }
        for (WorkEdge edge : model.getEdges())
        {
            outAdjacency.get(edge.getLayoutFrom())
                        .add(edge);
        }
        Comparator<WorkEdge> edgeOrder = Comparator.<WorkEdge, String>comparing(edge -> edge.getLayoutTo()
                                                                                            .getValue())
                                                   .thenComparing(edge -> edge.getId()
                                                                              .getValue());
        for (List<WorkEdge> outgoing : outAdjacency.values())
        {
            outgoing.sort(edgeOrder);
        }

        Map<LayoutNodeId, Integer> color = new LinkedHashMap<>();
        for (WorkNode node : nodesSortedById)
        {
            color.put(node.getId(), WHITE);
        }

        for (WorkNode node : nodesSortedById)
        {
            if (color.get(node.getId()) == WHITE)
            {
                dfs(node.getId(), outAdjacency, color);
            }
        }
    }

    private static void dfs(LayoutNodeId u, Map<LayoutNodeId, List<WorkEdge>> outAdjacency, Map<LayoutNodeId, Integer> color)
    {
        color.put(u, GRAY);
        for (WorkEdge edge : outAdjacency.getOrDefault(u, Collections.emptyList()))
        {
            LayoutNodeId v = edge.getLayoutTo();
            int state = color.get(v);
            if (state == GRAY)
            {
                edge.reverse();
            }
            else if (state == WHITE)
            {
                dfs(v, outAdjacency, color);
            }
        }
        color.put(u, BLACK);
    }

}
