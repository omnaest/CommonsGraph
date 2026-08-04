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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Helper used by {@link EdgeWaypointBuilderPhase} (plan-99 slice S1) to separate edges that would otherwise be routed onto one
 * coincident corridor: two or more edges sharing the same ({@link WorkEdge#getLayoutFrom()}, {@link WorkEdge#getLayoutTo()})
 * pair - taken AFTER {@link CycleRemovalPhase} has run, so an anti-parallel pair (whose back-edge cycle removal already
 * reversed onto the same layoutFrom/layoutTo as a genuine parallel pair) is grouped identically to that parallel pair, with no
 * special case for either.
 * <p>
 * Deliberately not a standalone phase and not a partition of {@link LayoutModel#getEdges()}: unlike self-loops, bundled edges
 * still participate in cycle removal, layering, dummy insertion and crossing minimization, so they must stay in the regular
 * edge list throughout the pipeline and only get separated at waypoint-building time.
 *
 * @author omnaest
 */
final class EdgeBundleCorridorSupport
{
    private EdgeBundleCorridorSupport()
    {
        super();
    }

    /**
     * Assigns each edge a signed lateral lane offset: edges sharing a (layoutFrom, layoutTo) pair form a bundle, sorted by
     * {@link LayoutEdgeId} natural order into lanes symmetric about the corridor centre -
     * {@code offset(k) = (k - (N-1)/2.0) * edgeSeparation} for lane {@code k} of {@code N}. A bundle of size 1 always resolves
     * to exactly {@code 0.0}, which is what keeps an unbundled edge's polyline byte-identical to today's.
     */
    static Map<LayoutEdgeId, Double> computeLaneOffsets(List<WorkEdge> edges, double edgeSeparation)
    {
        Map<List<LayoutNodeId>, List<WorkEdge>> bundles = new LinkedHashMap<>();
        for (WorkEdge edge : edges)
        {
            List<LayoutNodeId> bundleKey = Arrays.asList(edge.getLayoutFrom(), edge.getLayoutTo());
            bundles.computeIfAbsent(bundleKey, unused -> new ArrayList<>())
                   .add(edge);
        }

        Map<LayoutEdgeId, Double> offsets = new LinkedHashMap<>();
        for (List<WorkEdge> bundle : bundles.values())
        {
            List<WorkEdge> lanes = new ArrayList<>(bundle);
            lanes.sort((edgeA, edgeB) -> edgeA.getId()
                                              .compareTo(edgeB.getId()));
            int laneCount = lanes.size();
            for (int lane = 0; lane < laneCount; lane++)
            {
                double offset = (lane - (laneCount - 1) / 2.0) * edgeSeparation;
                offsets.put(lanes.get(lane)
                                 .getId(),
                            offset);
            }
        }
        return offsets;
    }

    /**
     * Rebuilds a baseline polyline (still in layout order, i.e. BEFORE any cycle-removal reversal is re-applied) into an
     * axis-aligned rectangular detour: the border endpoints are left exactly as they are, the polyline first runs straight OUT
     * along v by {@code inset} WHILE still sitting exactly on the unoffset border u (a vertical run shared identically by every
     * lane of the bundle - a short, bounded terminal stub, not the corridor this phase exists to separate), THEN jogs sideways
     * by {@code offset} along u onto the lane's own corridor, follows that corridor at {@code u + offset} (carrying every
     * interior waypoint with the same offset), and mirrors the same out-then-jog sequence in reverse to land on the untouched
     * target border point.
     * <p>
     * Out-before-sideways (rather than sideways-before-out) is deliberate, corrected from an earlier revision of this method
     * (plan-99 corrective round 1 / decision D7): sideways-before-out put the border-adjacent segment of the FINAL approach at
     * the offset u with the SAME v as the border point, i.e. a terminal segment running horizontally along the node's border -
     * {@code dy == 0} for the downstream arrowhead axis, which is derived from exactly the last two waypoints, so every
     * bundled edge's arrowhead ended up rotated 90 degrees flat against the border instead of pointing into it. Out-before-
     * sideways instead keeps the last two waypoints - and symmetrically the first two - sharing u and differing only in v, so
     * the endpoint approach is always perpendicular to the border regardless of offset. The bounded shared stub this
     * reintroduces at each border (all lanes coincide for the first/last {@code inset} of v) is normal, correct converging-edge
     * drawing, not the coincident-corridor defect this phase exists to remove - the corridor itself, from {@code v + inset} to
     * {@code v - inset}, is still fully lane-separated.
     * <p>
     * A zero offset - the only case for a lone edge, or whenever edgeSeparation is 0.0 - returns the baseline unchanged.
     */
    static List<UV> applyLaneDetour(List<UV> baseline, double offset, double inset)
    {
        if (offset == 0.0 || baseline.size() < 2)
        {
            return baseline;
        }

        UV first = baseline.get(0);
        UV last = baseline.get(baseline.size() - 1);

        List<UV> detoured = new ArrayList<>();
        detoured.add(first);
        detoured.add(new UV(first.getU(), first.getV() + inset));
        detoured.add(new UV(first.getU() + offset, first.getV() + inset));
        for (int i = 1; i < baseline.size() - 1; i++)
        {
            UV interior = baseline.get(i);
            detoured.add(new UV(interior.getU() + offset, interior.getV()));
        }
        detoured.add(new UV(last.getU() + offset, last.getV() - inset));
        detoured.add(new UV(last.getU(), last.getV() - inset));
        detoured.add(last);

        return detoured;
    }

}
