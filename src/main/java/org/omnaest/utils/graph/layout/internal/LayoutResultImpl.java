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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutResult;
import org.omnaest.utils.graph.layout.domain.Point;
import org.omnaest.utils.graph.layout.domain.Rectangle;

/**
 * Sole implementation of {@link LayoutResult}. Immutable snapshot produced by {@link ResultAssemblyPhase}.
 *
 * @author omnaest
 */
public class LayoutResultImpl implements LayoutResult
{
    private final Rectangle                      bounds;
    private final Map<LayoutNodeId, Rectangle>   nodeBounds;
    private final Map<LayoutEdgeId, List<Point>> edgeWaypoints;
    private final Map<LayoutNodeId, Integer>     layerIndex;

    LayoutResultImpl(Rectangle bounds, Map<LayoutNodeId, Rectangle> nodeBounds, Map<LayoutEdgeId, List<Point>> edgeWaypoints, Map<LayoutNodeId, Integer> layerIndex)
    {
        this.bounds = bounds;
        this.nodeBounds = Collections.unmodifiableMap(new LinkedHashMap<>(nodeBounds));
        this.edgeWaypoints = Collections.unmodifiableMap(new LinkedHashMap<>(edgeWaypoints));
        this.layerIndex = Collections.unmodifiableMap(new LinkedHashMap<>(layerIndex));
    }

    @Override
    public Rectangle getBounds()
    {
        return this.bounds;
    }

    @Override
    public Rectangle getNodeBounds(LayoutNodeId id)
    {
        Rectangle rectangle = this.nodeBounds.get(id);
        if (rectangle == null)
        {
            throw new IllegalArgumentException("Unknown node id: " + id);
        }
        return rectangle;
    }

    @Override
    public List<Point> getEdgeWaypoints(LayoutEdgeId id)
    {
        List<Point> waypoints = this.edgeWaypoints.get(id);
        if (waypoints == null)
        {
            throw new IllegalArgumentException("Unknown edge id: " + id);
        }
        return waypoints;
    }

    @Override
    public int getLayerIndex(LayoutNodeId id)
    {
        Integer layer = this.layerIndex.get(id);
        if (layer == null)
        {
            throw new IllegalArgumentException("Unknown node id: " + id);
        }
        return layer;
    }

    @Override
    public String toString()
    {
        return "LayoutResultImpl [bounds=" + this.bounds + ", nodeBounds=" + this.nodeBounds + ", edgeWaypoints=" + this.edgeWaypoints + ", layerIndex="
               + this.layerIndex + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.bounds.hashCode();
        result = prime * result + this.nodeBounds.hashCode();
        result = prime * result + this.edgeWaypoints.hashCode();
        result = prime * result + this.layerIndex.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof LayoutResultImpl))
        {
            return false;
        }
        LayoutResultImpl other = (LayoutResultImpl) obj;
        return this.bounds.equals(other.bounds) && this.nodeBounds.equals(other.nodeBounds) && this.edgeWaypoints.equals(other.edgeWaypoints)
               && this.layerIndex.equals(other.layerIndex);
    }

}
