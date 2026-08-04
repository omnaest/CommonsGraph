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

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Algorithm step 10: builds the canonical waypoint polyline for every regular (non-self-loop) edge from its dummy-expanded
 * path - the first point on the source node's trailing across-axis border, dummy-node centres in between, the last point on
 * the target node's leading across-axis border. If cycle-removal reversed the edge, the list is reversed here so it always
 * reads from the ORIGINAL source to the ORIGINAL target; the reversal is invisible to every caller.
 * <p>
 * plan-99 slice S1: before that reversal, edges that belong to a bundle (two or more sharing the same layoutFrom/layoutTo
 * pair - see {@link EdgeBundleCorridorSupport}) are detoured onto their own lane so their polylines no longer coincide. The
 * border endpoints are never moved by this - only the corridor interior is offset - so
 * {@link org.omnaest.utils.graph.layout.domain.LayoutResult#getEdgeWaypoints(LayoutEdgeId)}'s documented contract (first point
 * on the source border, last point on the target border) holds unchanged.
 *
 * @author omnaest
 */
final class EdgeWaypointBuilderPhase
{
    private EdgeWaypointBuilderPhase()
    {
        super();
    }

    static Map<LayoutEdgeId, List<UV>> apply(LayoutModel model)
    {
        Map<LayoutEdgeId, List<UV>> result = new LinkedHashMap<>();
        double edgeSeparation = model.getOptions()
                                     .getEdgeSeparation();
        double inset = model.getOptions()
                            .getLayerSeparation()
                       / 4.0;
        Map<LayoutEdgeId, Double> laneOffsets = EdgeBundleCorridorSupport.computeLaneOffsets(model.getEdges(), edgeSeparation);

        for (WorkEdge edge : model.getEdges())
        {
            List<LayoutNodeId> path = edge.getPath();
            List<UV> waypoints = new ArrayList<>();
            for (int i = 0; i < path.size(); i++)
            {
                WorkNode node = model.getNode(path.get(i));
                if (i == 0)
                {
                    waypoints.add(new UV(node.centerU(), node.getV() + node.getAcrossSize()));
                }
                else if (i == path.size() - 1)
                {
                    waypoints.add(new UV(node.centerU(), node.getV()));
                }
                else
                {
                    waypoints.add(new UV(node.centerU(), node.getV() + node.getAcrossSize() / 2.0));
                }
            }

            double offset = laneOffsets.getOrDefault(edge.getId(), 0.0);
            waypoints = EdgeBundleCorridorSupport.applyLaneDetour(waypoints, offset, inset);

            if (edge.isReversed())
            {
                Collections.reverse(waypoints);
            }

            result.put(edge.getId(), waypoints);
        }

        return result;
    }

}
