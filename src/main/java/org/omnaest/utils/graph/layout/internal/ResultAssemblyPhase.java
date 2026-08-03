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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.omnaest.utils.graph.layout.domain.LayoutDirection;
import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutResult;
import org.omnaest.utils.graph.layout.domain.Point;
import org.omnaest.utils.graph.layout.domain.Rectangle;

/**
 * Algorithm steps 9 and 11 combined: maps every canonical (u,v) coordinate to real (x,y) - swapping the axes for
 * LEFT_TO_RIGHT rather than re-running the pipeline in a second orientation - then translates the whole result so
 * {@link LayoutResult#getBounds()} starts at exactly (0,0).
 *
 * @author omnaest
 */
final class ResultAssemblyPhase
{
    private ResultAssemblyPhase()
    {
        super();
    }

    static LayoutResult apply(LayoutModel model, Map<LayoutEdgeId, List<UV>> regularWaypoints, Map<LayoutEdgeId, List<UV>> selfLoopWaypoints)
    {
        boolean topToBottom = model.getOptions()
                                   .getDirection() == LayoutDirection.TOP_TO_BOTTOM;

        Map<LayoutNodeId, Rectangle> nodeBounds = new LinkedHashMap<>();
        Map<LayoutNodeId, Integer> layerIndex = new LinkedHashMap<>();
        for (WorkNode node : model.getAllNodes())
        {
            if (node.isDummy())
            {
                continue;
            }
            double x = topToBottom ? node.getU() : node.getV();
            double y = topToBottom ? node.getV() : node.getU();
            nodeBounds.put(node.getId(), Rectangle.of(x, y, node.getRealWidth(), node.getRealHeight()));
            layerIndex.put(node.getId(), node.getLayer());
        }

        Map<LayoutEdgeId, List<Point>> edgeWaypoints = new LinkedHashMap<>();
        for (Map.Entry<LayoutEdgeId, List<UV>> entry : regularWaypoints.entrySet())
        {
            edgeWaypoints.put(entry.getKey(), mapPoints(entry.getValue(), topToBottom));
        }
        for (Map.Entry<LayoutEdgeId, List<UV>> entry : selfLoopWaypoints.entrySet())
        {
            edgeWaypoints.put(entry.getKey(), mapPoints(entry.getValue(), topToBottom));
        }

        if (nodeBounds.isEmpty())
        {
            return new LayoutResultImpl(Rectangle.of(0, 0, 0, 0), nodeBounds, edgeWaypoints, layerIndex);
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Rectangle rectangle : nodeBounds.values())
        {
            minX = Math.min(minX, rectangle.getX());
            minY = Math.min(minY, rectangle.getY());
            maxX = Math.max(maxX, rectangle.getRight());
            maxY = Math.max(maxY, rectangle.getBottom());
        }
        for (List<Point> waypoints : edgeWaypoints.values())
        {
            for (Point point : waypoints)
            {
                minX = Math.min(minX, point.getX());
                minY = Math.min(minY, point.getY());
                maxX = Math.max(maxX, point.getX());
                maxY = Math.max(maxY, point.getY());
            }
        }

        double dx = -minX;
        double dy = -minY;

        Map<LayoutNodeId, Rectangle> finalNodeBounds = new LinkedHashMap<>();
        for (Map.Entry<LayoutNodeId, Rectangle> entry : nodeBounds.entrySet())
        {
            Rectangle rectangle = entry.getValue();
            finalNodeBounds.put(entry.getKey(), Rectangle.of(rectangle.getX() + dx, rectangle.getY() + dy, rectangle.getWidth(), rectangle.getHeight()));
        }

        Map<LayoutEdgeId, List<Point>> finalEdgeWaypoints = new LinkedHashMap<>();
        for (Map.Entry<LayoutEdgeId, List<Point>> entry : edgeWaypoints.entrySet())
        {
            List<Point> translated = new ArrayList<>();
            for (Point point : entry.getValue())
            {
                translated.add(point.translate(dx, dy));
            }
            finalEdgeWaypoints.put(entry.getKey(), translated);
        }

        Rectangle bounds = Rectangle.of(0, 0, maxX - minX, maxY - minY);

        return new LayoutResultImpl(bounds, finalNodeBounds, finalEdgeWaypoints, layerIndex);
    }

    private static List<Point> mapPoints(List<UV> points, boolean topToBottom)
    {
        List<Point> result = new ArrayList<>();
        for (UV point : points)
        {
            result.add(topToBottom ? Point.of(point.getU(), point.getV()) : Point.of(point.getV(), point.getU()));
        }
        return result;
    }

}
