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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;

/**
 * Algorithm step 8: explicit self-loop routing, not a degenerate case of the general edge router. A rectangular detour off
 * the node's trailing along-axis edge (the right edge once mapped to real TOP_TO_BOTTOM coordinates): four waypoints tracing
 * out from the node's border, across by selfLoopReserve, and back in. The node's own along-axis slot was already inflated by
 * selfLoopReserve in {@link BuildWorkGraphPhase} / consumed during {@link XCoordinateAssignmentPhase} packing, so the nearest
 * same-layer neighbour is guaranteed to be at least selfLoopReserve away and the detour never overlaps it.
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
        double reserve = model.getOptions()
                              .getSelfLoopReserve();

        for (WorkEdge edge : model.getSelfLoopEdges())
        {
            WorkNode node = model.getNode(edge.getOriginalFrom());
            double trailingEdge = node.getU() + node.getAlongSize();
            double centerV = node.getV() + node.getAcrossSize() / 2.0;
            double quarterAcross = node.getAcrossSize() / 4.0;

            List<UV> waypoints = Arrays.asList(new UV(trailingEdge, centerV - quarterAcross), new UV(trailingEdge + reserve, centerV - quarterAcross),
                                               new UV(trailingEdge + reserve, centerV + quarterAcross), new UV(trailingEdge, centerV + quarterAcross));

            result.put(edge.getId(), waypoints);
        }

        return result;
    }

}
