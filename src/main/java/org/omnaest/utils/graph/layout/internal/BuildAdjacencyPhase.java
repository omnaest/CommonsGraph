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

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Derives per-node adjacent-layer adjacency (upper = neighbours one layer above, lower = neighbours one layer below) from
 * every edge's dummy-expanded path. Shared by crossing minimization (step 5) and x-coordinate assignment (step 6).
 *
 * @author omnaest
 */
final class BuildAdjacencyPhase
{
    private BuildAdjacencyPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        Map<LayoutNodeId, List<LayoutNodeId>> upper = new LinkedHashMap<>();
        Map<LayoutNodeId, List<LayoutNodeId>> lower = new LinkedHashMap<>();
        for (WorkNode node : model.getAllNodes())
        {
            upper.put(node.getId(), new ArrayList<>());
            lower.put(node.getId(), new ArrayList<>());
        }

        List<WorkEdge> sortedEdges = new ArrayList<>(model.getEdges());
        sortedEdges.sort(Comparator.comparing(edge -> edge.getId()
                                                          .getValue()));

        for (WorkEdge edge : sortedEdges)
        {
            List<LayoutNodeId> path = edge.getPath();
            for (int i = 0; i < path.size() - 1; i++)
            {
                LayoutNodeId a = path.get(i);
                LayoutNodeId b = path.get(i + 1);
                lower.get(a)
                     .add(b);
                upper.get(b)
                     .add(a);
            }
        }

        model.setUpperAdjacency(upper);
        model.setLowerAdjacency(lower);
    }

}
