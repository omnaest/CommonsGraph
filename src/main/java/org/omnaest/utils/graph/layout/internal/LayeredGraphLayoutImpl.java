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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.omnaest.utils.graph.layout.LayeredGraphLayout;
import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;
import org.omnaest.utils.graph.layout.domain.LayoutResult;

/**
 * Sole implementation of {@link LayeredGraphLayout}. Orchestrates the flat layered layout pipeline (algorithm steps 1-11) as
 * a sequence of stateless phase classes over a fresh, per-call {@link LayoutModel} - never shared state, never a mutation of
 * the input {@link LayoutGraph}.
 *
 * @author omnaest
 */
public class LayeredGraphLayoutImpl implements LayeredGraphLayout
{
    private final LayoutOptions options;

    public LayeredGraphLayoutImpl(LayoutOptions options)
    {
        this.options = options;
    }

    @Override
    public LayoutResult apply(LayoutGraph graph)
    {
        Objects.requireNonNull(graph, "graph must not be null");

        LayoutModel model = BuildWorkGraphPhase.apply(graph, this.options);
        CycleRemovalPhase.apply(model);
        LongestPathLayeringPhase.apply(model);
        DummyNodeInsertionPhase.apply(model);
        BuildAdjacencyPhase.apply(model);
        CrossingMinimizationPhase.apply(model);
        XCoordinateAssignmentPhase.apply(model);
        YCoordinateAssignmentPhase.apply(model);

        Map<LayoutEdgeId, List<UV>> selfLoopWaypoints = SelfLoopRoutingPhase.apply(model);
        Map<LayoutEdgeId, List<UV>> regularWaypoints = EdgeWaypointBuilderPhase.apply(model);

        return ResultAssemblyPhase.apply(model, regularWaypoints, selfLoopWaypoints);
    }

}
