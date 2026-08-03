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
import java.util.List;

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Algorithm step 4: any edge spanning more than one layer is split into a chain through dummy nodes, one per intermediate
 * layer, so long edges route between nodes rather than through them. Dummy ids are namespaced with the owning edge's id, so
 * two multi-edges between the same pair (cliff C1) each get their own independent dummy chain and never collide.
 *
 * @author omnaest
 */
final class DummyNodeInsertionPhase
{
    private static final double DUMMY_SIZE = 1.0;

    private DummyNodeInsertionPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        List<WorkEdge> edgesSnapshot = new ArrayList<>(model.getEdges());
        for (WorkEdge edge : edgesSnapshot)
        {
            WorkNode fromNode = model.getNode(edge.getLayoutFrom());
            WorkNode toNode = model.getNode(edge.getLayoutTo());
            int fromLayer = fromNode.getLayer();
            int toLayer = toNode.getLayer();

            List<LayoutNodeId> path = new ArrayList<>();
            path.add(fromNode.getId());
            for (int layer = fromLayer + 1; layer < toLayer; layer++)
            {
                LayoutNodeId dummyId = LayoutNodeId.of("__dummy__" + edge.getId()
                                                                         .getValue()
                                                       + "__" + layer);
                WorkNode dummy = new WorkNode(dummyId, true, DUMMY_SIZE, DUMMY_SIZE, DUMMY_SIZE, DUMMY_SIZE, 0.0, 0);
                dummy.setLayer(layer);
                model.addNode(dummy);
                path.add(dummyId);
            }
            path.add(toNode.getId());

            edge.setPath(path);
        }
    }

}
