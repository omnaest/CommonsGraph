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

import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Mutable working-model node used only inside the layout engine (never exposed). Real nodes and dummy nodes (inserted for
 * long edges) share this type.
 * <p>
 * All sizes are expressed in a canonical coordinate system: "along" is the in-layer packing axis (X for TOP_TO_BOTTOM, Y for
 * LEFT_TO_RIGHT), "across" is the layer-to-layer stacking axis. The final direction-dependent mapping to real (x,y) happens
 * once, at result assembly time.
 *
 * @author omnaest
 */
final class WorkNode
{
    private final LayoutNodeId id;
    private final boolean      dummy;
    private final double       alongSize;
    private final double       acrossSize;
    private final double       realWidth;
    private final double       realHeight;
    private final double       selfLoopReserve;
    private final int          degree;

    private int                layer = -1;
    private double             u;
    private double             v;

    WorkNode(LayoutNodeId id, boolean dummy, double alongSize, double acrossSize, double realWidth, double realHeight, double selfLoopReserve, int degree)
    {
        this.id = id;
        this.dummy = dummy;
        this.alongSize = alongSize;
        this.acrossSize = acrossSize;
        this.realWidth = realWidth;
        this.realHeight = realHeight;
        this.selfLoopReserve = selfLoopReserve;
        this.degree = degree;
    }

    LayoutNodeId getId()
    {
        return this.id;
    }

    boolean isDummy()
    {
        return this.dummy;
    }

    double getAlongSize()
    {
        return this.alongSize;
    }

    double getAcrossSize()
    {
        return this.acrossSize;
    }

    double getRealWidth()
    {
        return this.realWidth;
    }

    double getRealHeight()
    {
        return this.realHeight;
    }

    /**
     * Along-axis size including the reserved self-loop detour space, used only for spacing/packing calculations.
     */
    double getSlotAlongSize()
    {
        return this.alongSize + this.selfLoopReserve;
    }

    int getDegree()
    {
        return this.degree;
    }

    int getLayer()
    {
        return this.layer;
    }

    void setLayer(int layer)
    {
        this.layer = layer;
    }

    double getU()
    {
        return this.u;
    }

    void setU(double u)
    {
        this.u = u;
    }

    double getV()
    {
        return this.v;
    }

    void setV(double v)
    {
        this.v = v;
    }

    double centerU()
    {
        return this.u + this.alongSize / 2.0;
    }

    @Override
    public String toString()
    {
        return "WorkNode [id=" + this.id + ", dummy=" + this.dummy + ", layer=" + this.layer + ", u=" + this.u + ", v=" + this.v + "]";
    }

}
