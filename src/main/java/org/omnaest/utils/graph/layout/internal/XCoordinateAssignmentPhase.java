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
 * Algorithm step 6: priority-method along-axis (canonical "u") coordinate assignment. Each layer is first packed left-to-
 * right at nodeSeparation using each node's slot size (its own size plus any reserved self-loop detour space). A fixed 8
 * down/up passes then move each node toward the median of its neighbours' centre, processed in priority order (dummy nodes
 * first, then real nodes by descending degree) - a node is only ever clamped between its CURRENT immediate left/right
 * neighbour in the layer, so no move can ever displace another node or produce an overlap, regardless of processing order.
 *
 * @author omnaest
 */
final class XCoordinateAssignmentPhase
{
    private static final int PASS_COUNT = 8;

    private XCoordinateAssignmentPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        List<List<LayoutNodeId>> order = model.getLayerOrder();
        double nodeSeparation = model.getOptions()
                                     .getNodeSeparation();
        int maxLayer = model.getMaxLayer();

        for (List<LayoutNodeId> layerList : order)
        {
            double u = 0;
            for (LayoutNodeId id : layerList)
            {
                WorkNode node = model.getNode(id);
                node.setU(u);
                u += node.getSlotAlongSize() + nodeSeparation;
            }
        }

        Map<LayoutNodeId, List<LayoutNodeId>> upperAdjacency = model.getUpperAdjacency();
        Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency = model.getLowerAdjacency();

        for (int pass = 0; pass < PASS_COUNT; pass++)
        {
            boolean down = pass % 2 == 0;
            if (down)
            {
                for (int layer = 1; layer <= maxLayer; layer++)
                {
                    refineLayer(model, order.get(layer), upperAdjacency, nodeSeparation);
                }
            }
            else
            {
                for (int layer = maxLayer - 1; layer >= 0; layer--)
                {
                    refineLayer(model, order.get(layer), lowerAdjacency, nodeSeparation);
                }
            }
        }
    }

    private static void refineLayer(LayoutModel model, List<LayoutNodeId> layerList, Map<LayoutNodeId, List<LayoutNodeId>> adjacency, double nodeSeparation)
    {
        Map<LayoutNodeId, Integer> indexInLayer = new LinkedHashMap<>();
        for (int i = 0; i < layerList.size(); i++)
        {
            indexInLayer.put(layerList.get(i), i);
        }

        List<LayoutNodeId> priorityOrder = new ArrayList<>(layerList);
        priorityOrder.sort(Comparator.<LayoutNodeId>comparingLong(id -> -priority(model.getNode(id)))
                                     .thenComparing(LayoutNodeId::getValue));

        for (LayoutNodeId id : priorityOrder)
        {
            WorkNode node = model.getNode(id);
            int idx = indexInLayer.get(id);
            List<LayoutNodeId> neighbours = adjacency.getOrDefault(id, Collections.emptyList());
            if (neighbours.isEmpty())
            {
                continue;
            }

            List<Double> centers = new ArrayList<>();
            for (LayoutNodeId neighbourId : neighbours)
            {
                centers.add(model.getNode(neighbourId)
                                 .centerU());
            }
            Collections.sort(centers);
            double median = medianOf(centers);
            double desiredLeftU = median - node.getAlongSize() / 2.0;

            double minU = idx > 0
                    ? model.getNode(layerList.get(idx - 1))
                           .getU()
                      + model.getNode(layerList.get(idx - 1))
                             .getSlotAlongSize()
                      + nodeSeparation
                    : Double.NEGATIVE_INFINITY;
            double maxU = idx < layerList.size() - 1
                    ? model.getNode(layerList.get(idx + 1))
                           .getU()
                      - nodeSeparation - node.getSlotAlongSize()
                    : Double.POSITIVE_INFINITY;

            if (minU <= maxU)
            {
                double clamped = Math.max(minU, Math.min(maxU, desiredLeftU));
                node.setU(clamped);
            }
        }
    }

    private static long priority(WorkNode node)
    {
        return node.isDummy() ? Long.MAX_VALUE : node.getDegree();
    }

    private static double medianOf(List<Double> sortedValues)
    {
        int size = sortedValues.size();
        if (size % 2 == 1)
        {
            return sortedValues.get(size / 2);
        }
        return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
    }

}
