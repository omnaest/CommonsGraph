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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.omnaest.utils.graph.layout.domain.LayoutDirection;
import org.omnaest.utils.graph.layout.domain.LayoutEdge;
import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutNode;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;

/**
 * Builds the mutable {@link LayoutModel} working copy from the immutable input {@link LayoutGraph} (algorithm step 1: also
 * splits off self-loop edges here, before they can reach cycle-removal or crossing-minimization).
 *
 * @author omnaest
 */
final class BuildWorkGraphPhase
{
    private BuildWorkGraphPhase()
    {
        super();
    }

    static LayoutModel apply(LayoutGraph graph, LayoutOptions options)
    {
        LayoutModel model = new LayoutModel(options);
        boolean topToBottom = options.getDirection() == LayoutDirection.TOP_TO_BOTTOM;

        Set<LayoutNodeId> selfLoopNodeIds = new LinkedHashSet<>();
        Map<LayoutNodeId, Integer> degree = new LinkedHashMap<>();
        for (LayoutNode node : graph.getNodes())
        {
            degree.put(node.getId(), 0);
        }
        for (LayoutEdge edge : graph.getEdges())
        {
            if (edge.getFrom()
                    .equals(edge.getTo()))
            {
                selfLoopNodeIds.add(edge.getFrom());
            }
            else
            {
                degree.merge(edge.getFrom(), 1, Integer::sum);
                degree.merge(edge.getTo(), 1, Integer::sum);
            }
        }

        for (LayoutNode node : graph.getNodes())
        {
            double width = node.getSize()
                               .getWidth();
            double height = node.getSize()
                                .getHeight();
            double alongSize = topToBottom ? width : height;
            double acrossSize = topToBottom ? height : width;
            double selfLoopReserve = selfLoopNodeIds.contains(node.getId()) ? options.getSelfLoopReserve() : 0.0;
            int nodeDegree = degree.getOrDefault(node.getId(), 0);
            model.addNode(new WorkNode(node.getId(), false, alongSize, acrossSize, width, height, selfLoopReserve, nodeDegree));
        }

        for (LayoutEdge edge : graph.getEdges())
        {
            WorkEdge workEdge = new WorkEdge(edge.getId(), edge.getFrom(), edge.getTo());
            if (edge.getFrom()
                    .equals(edge.getTo()))
            {
                model.addSelfLoopEdge(workEdge);
            }
            else
            {
                model.addEdge(workEdge);
            }
        }

        return model;
    }

}
