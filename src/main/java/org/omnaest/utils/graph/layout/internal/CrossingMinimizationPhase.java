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
 * Algorithm step 5: barycentric/median ordering sweeps (down, then up, alternating, 8 total - a fixed count, never a
 * time-bounded "until no improvement" loop) followed by a transpose pass that swaps adjacent pairs while that reduces total
 * crossings. The best ordering seen by total crossing count is kept.
 *
 * @author omnaest
 */
final class CrossingMinimizationPhase
{
    private static final int SWEEP_COUNT      = 8;
    private static final int TRANSPOSE_PASSES = 4;

    private CrossingMinimizationPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        int maxLayer = model.getMaxLayer();
        Map<LayoutNodeId, List<LayoutNodeId>> upperAdjacency = model.getUpperAdjacency();
        Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency = model.getLowerAdjacency();

        List<List<LayoutNodeId>> order = new ArrayList<>();
        for (int layer = 0; layer <= maxLayer; layer++)
        {
            order.add(new ArrayList<>());
        }
        for (WorkNode node : model.getAllNodes())
        {
            order.get(node.getLayer())
                 .add(node.getId());
        }
        for (List<LayoutNodeId> layerList : order)
        {
            layerList.sort(Comparator.comparing(LayoutNodeId::getValue));
        }

        List<List<LayoutNodeId>> best = deepCopy(order);
        int bestCrossings = countTotalCrossings(order, lowerAdjacency, maxLayer);

        for (int sweep = 0; sweep < SWEEP_COUNT; sweep++)
        {
            boolean down = sweep % 2 == 0;
            if (down)
            {
                for (int layer = 1; layer <= maxLayer; layer++)
                {
                    reorderLayer(order.get(layer), order.get(layer - 1), upperAdjacency);
                }
            }
            else
            {
                for (int layer = maxLayer - 1; layer >= 0; layer--)
                {
                    reorderLayer(order.get(layer), order.get(layer + 1), lowerAdjacency);
                }
            }

            transpose(order, lowerAdjacency, maxLayer);

            int crossings = countTotalCrossings(order, lowerAdjacency, maxLayer);
            if (crossings < bestCrossings)
            {
                bestCrossings = crossings;
                best = deepCopy(order);
            }
        }

        model.setLayerOrder(best);
    }

    private static void reorderLayer(List<LayoutNodeId> layerList, List<LayoutNodeId> referenceLayerList, Map<LayoutNodeId, List<LayoutNodeId>> adjacency)
    {
        Map<LayoutNodeId, Integer> referencePosition = positionIndex(referenceLayerList);
        Map<LayoutNodeId, Integer> ownPosition = positionIndex(layerList);

        Map<LayoutNodeId, Double> barycenter = new LinkedHashMap<>();
        for (LayoutNodeId id : layerList)
        {
            List<LayoutNodeId> neighbours = adjacency.getOrDefault(id, Collections.emptyList());
            double sum = 0;
            int count = 0;
            for (LayoutNodeId neighbour : neighbours)
            {
                Integer position = referencePosition.get(neighbour);
                if (position != null)
                {
                    sum += position;
                    count++;
                }
            }
            barycenter.put(id, count == 0 ? (double) ownPosition.get(id) : sum / count);
        }

        layerList.sort(Comparator.<LayoutNodeId>comparingDouble(barycenter::get)
                                 .thenComparing(LayoutNodeId::getValue));
    }

    private static void transpose(List<List<LayoutNodeId>> order, Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency, int maxLayer)
    {
        for (int pass = 0; pass < TRANSPOSE_PASSES; pass++)
        {
            boolean improvedAny = false;
            for (int layer = 0; layer <= maxLayer; layer++)
            {
                List<LayoutNodeId> layerList = order.get(layer);
                for (int i = 0; i < layerList.size() - 1; i++)
                {
                    int before = countTotalCrossings(order, lowerAdjacency, maxLayer);
                    Collections.swap(layerList, i, i + 1);
                    int after = countTotalCrossings(order, lowerAdjacency, maxLayer);
                    if (after < before)
                    {
                        improvedAny = true;
                    }
                    else
                    {
                        Collections.swap(layerList, i, i + 1);
                    }
                }
            }
            if (!improvedAny)
            {
                break;
            }
        }
    }

    private static int countTotalCrossings(List<List<LayoutNodeId>> order, Map<LayoutNodeId, List<LayoutNodeId>> lowerAdjacency, int maxLayer)
    {
        int total = 0;
        for (int layer = 0; layer < maxLayer; layer++)
        {
            Map<LayoutNodeId, Integer> positionInLayer = positionIndex(order.get(layer));
            Map<LayoutNodeId, Integer> positionInNextLayer = positionIndex(order.get(layer + 1));

            List<int[]> pairs = new ArrayList<>();
            for (LayoutNodeId a : order.get(layer))
            {
                for (LayoutNodeId b : lowerAdjacency.getOrDefault(a, Collections.emptyList()))
                {
                    Integer positionB = positionInNextLayer.get(b);
                    if (positionB != null)
                    {
                        pairs.add(new int[] {positionInLayer.get(a), positionB});
                    }
                }
            }

            for (int i = 0; i < pairs.size(); i++)
            {
                for (int j = i + 1; j < pairs.size(); j++)
                {
                    int[] p1 = pairs.get(i);
                    int[] p2 = pairs.get(j);
                    boolean crosses = (p1[0] < p2[0] && p1[1] > p2[1]) || (p1[0] > p2[0] && p1[1] < p2[1]);
                    if (crosses)
                    {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    private static Map<LayoutNodeId, Integer> positionIndex(List<LayoutNodeId> list)
    {
        Map<LayoutNodeId, Integer> position = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++)
        {
            position.put(list.get(i), i);
        }
        return position;
    }

    private static List<List<LayoutNodeId>> deepCopy(List<List<LayoutNodeId>> order)
    {
        List<List<LayoutNodeId>> copy = new ArrayList<>();
        for (List<LayoutNodeId> layerList : order)
        {
            copy.add(new ArrayList<>(layerList));
        }
        return copy;
    }

}
