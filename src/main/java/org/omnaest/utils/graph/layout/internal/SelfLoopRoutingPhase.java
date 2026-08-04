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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;

/**
 * Algorithm step 8: explicit self-loop routing, not a degenerate case of the general edge router. A rectangular detour off
 * the node's trailing along-axis edge (the right edge once mapped to real TOP_TO_BOTTOM coordinates): four waypoints tracing
 * out from the node's border, across by a per-loop depth, and back in.
 * <p>
 * A node carrying N self-loops divides its own across-axis size into N equal bands, one per loop - band i (0-indexed, loops
 * ordered by {@link LayoutEdgeId} natural order for determinism, never by hash iteration) is centred at
 * {@code node.v + i*bandHeight + bandHeight/2} with a quarter-band half-span either side, exactly reproducing the original
 * single-loop centre/half-span for N==1. Because every loop's band lies in a disjoint across-axis slice, the N detour
 * rectangles never overlap regardless of how their along-axis depths compare. Depth still increases with the loop's index
 * ({@code selfLoopReserve + i*edgeSeparation}, see {@link LayoutOptions#getEdgeSeparation()}) so the loops nest visually
 * (loop 0 innermost) and so the outermost loop's own reserved along-axis slot - inflated to match in
 * {@link BuildWorkGraphPhase} / consumed during {@link XCoordinateAssignmentPhase} packing - still clears the nearest
 * same-layer neighbour by at least selfLoopReserve. N==1 reproduces {@code selfLoopReserve} exactly, so single-self-loop
 * diagrams are byte-identical to before this class supported multiple loops per node.
 *
 * @author omnaest
 */
final class SelfLoopRoutingPhase
{
    private SelfLoopRoutingPhase()
    {
        super();
    }

    static Map<LayoutEdgeId, List<UV>> apply(LayoutModel model)
    {
        Map<LayoutEdgeId, List<UV>> result = new LinkedHashMap<>();
        double baseReserve = model.getOptions()
                                  .getSelfLoopReserve();
        double edgeSeparation = model.getOptions()
                                     .getEdgeSeparation();

        Map<LayoutNodeId, List<WorkEdge>> loopsByNode = new LinkedHashMap<>();
        for (WorkEdge edge : model.getSelfLoopEdges())
        {
            loopsByNode.computeIfAbsent(edge.getOriginalFrom(), key -> new ArrayList<>())
                       .add(edge);
        }

        for (Map.Entry<LayoutNodeId, List<WorkEdge>> entry : loopsByNode.entrySet())
        {
            WorkNode node = model.getNode(entry.getKey());
            List<WorkEdge> loops = new ArrayList<>(entry.getValue());
            loops.sort(Comparator.comparing(WorkEdge::getId));

            double trailingEdge = node.getU() + node.getAlongSize();
            int loopCount = loops.size();
            double bandHeight = node.getAcrossSize() / loopCount;

            for (int i = 0; i < loopCount; i++)
            {
                WorkEdge edge = loops.get(i);
                double centerV = node.getV() + i * bandHeight + bandHeight / 2.0;
                double halfSpan = bandHeight / 4.0;
                double depth = baseReserve + i * edgeSeparation;

                List<UV> waypoints = Arrays.asList(new UV(trailingEdge, centerV - halfSpan), new UV(trailingEdge + depth, centerV - halfSpan),
                                                   new UV(trailingEdge + depth, centerV + halfSpan), new UV(trailingEdge, centerV + halfSpan));

                result.put(edge.getId(), waypoints);
            }
        }

        return result;
    }

}
