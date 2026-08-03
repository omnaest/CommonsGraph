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
package org.omnaest.utils.graph.layout.domain;

import java.util.List;

/**
 * The geometry produced by a {@link org.omnaest.utils.graph.layout.LayeredGraphLayout}. The overall {@link #getBounds()} is
 * normalized so its origin is exactly (0,0).
 *
 * @author omnaest
 */
public interface LayoutResult
{
    public Rectangle getBounds();

    /**
     * Returns the placed {@link Rectangle} of the {@link LayoutNode} with the given id.
     *
     * @param id
     * @return
     */
    public Rectangle getNodeBounds(LayoutNodeId id);

    /**
     * Returns the polyline for the given edge: the first {@link Point} lies on the source node's border, the last
     * {@link Point} lies on the target node's border, with any intermediate routing points (dummy-node centres, self-loop
     * detour corners) in between. Always reads from-source-to-target, regardless of any internal cycle-removal reversal.
     *
     * @param id
     * @return
     */
    public List<Point> getEdgeWaypoints(LayoutEdgeId id);

    /**
     * Returns the 0-based layer index of the {@link LayoutNode} with the given id.
     *
     * @param id
     * @return
     */
    public int getLayerIndex(LayoutNodeId id);
}
