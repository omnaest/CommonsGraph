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

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Algorithm step 7: each layer's across-axis (canonical "v") band starts at the running total of previous layers' max
 * across-size plus layerSeparation. Nodes are centred within their layer's band.
 *
 * @author omnaest
 */
final class YCoordinateAssignmentPhase
{
    private YCoordinateAssignmentPhase()
    {
        super();
    }

    static void apply(LayoutModel model)
    {
        List<List<LayoutNodeId>> order = model.getLayerOrder();
        double layerSeparation = model.getOptions()
                                      .getLayerSeparation();

        double runningV = 0;
        for (List<LayoutNodeId> layerList : order)
        {
            double bandHeight = 0;
            for (LayoutNodeId id : layerList)
            {
                bandHeight = Math.max(bandHeight, model.getNode(id)
                                                       .getAcrossSize());
            }
            for (LayoutNodeId id : layerList)
            {
                WorkNode node = model.getNode(id);
                double v = runningV + (bandHeight - node.getAcrossSize()) / 2.0;
                node.setV(v);
            }
            runningV += bandHeight + layerSeparation;
        }
    }

}
