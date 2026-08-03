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

import java.util.Objects;

/**
 * Immutable tuning parameters for a layered layout run.
 *
 * @see #defaults()
 * @see #builder()
 * @author omnaest
 */
public final class LayoutOptions
{
    private final LayoutDirection direction;
    private final double          layerSeparation;
    private final double          nodeSeparation;
    private final double          selfLoopReserve;

    private LayoutOptions(LayoutDirection direction, double layerSeparation, double nodeSeparation, double selfLoopReserve)
    {
        this.direction = direction;
        this.layerSeparation = layerSeparation;
        this.nodeSeparation = nodeSeparation;
        this.selfLoopReserve = selfLoopReserve;
    }

    public LayoutDirection getDirection()
    {
        return this.direction;
    }

    public double getLayerSeparation()
    {
        return this.layerSeparation;
    }

    public double getNodeSeparation()
    {
        return this.nodeSeparation;
    }

    public double getSelfLoopReserve()
    {
        return this.selfLoopReserve;
    }

    /**
     * Returns the default {@link LayoutOptions}: {@link LayoutDirection#TOP_TO_BOTTOM}, layerSeparation 60, nodeSeparation 45,
     * selfLoopReserve 40.
     *
     * @return
     */
    public static LayoutOptions defaults()
    {
        return builder().build();
    }

    public static LayoutOptionsBuilder builder()
    {
        return new LayoutOptionsBuilder() {
            private LayoutDirection direction       = LayoutDirection.TOP_TO_BOTTOM;
            private double          layerSeparation = 60;
            private double          nodeSeparation  = 45;
            private double          selfLoopReserve = 40;

            @Override
            public LayoutOptionsBuilder direction(LayoutDirection direction)
            {
                this.direction = Objects.requireNonNull(direction, "direction must not be null");
                return this;
            }

            @Override
            public LayoutOptionsBuilder layerSeparation(double layerSeparation)
            {
                this.layerSeparation = layerSeparation;
                return this;
            }

            @Override
            public LayoutOptionsBuilder nodeSeparation(double nodeSeparation)
            {
                this.nodeSeparation = nodeSeparation;
                return this;
            }

            @Override
            public LayoutOptionsBuilder selfLoopReserve(double selfLoopReserve)
            {
                this.selfLoopReserve = selfLoopReserve;
                return this;
            }

            @Override
            public LayoutOptions build()
            {
                return new LayoutOptions(this.direction, this.layerSeparation, this.nodeSeparation, this.selfLoopReserve);
            }
        };
    }

    @Override
    public String toString()
    {
        return "LayoutOptions [direction=" + this.direction + ", layerSeparation=" + this.layerSeparation + ", nodeSeparation=" + this.nodeSeparation
               + ", selfLoopReserve=" + this.selfLoopReserve + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.direction == null) ? 0 : this.direction.hashCode());
        long bitsLayerSeparation = Double.doubleToLongBits(this.layerSeparation);
        long bitsNodeSeparation = Double.doubleToLongBits(this.nodeSeparation);
        long bitsSelfLoopReserve = Double.doubleToLongBits(this.selfLoopReserve);
        result = prime * result + (int) (bitsLayerSeparation ^ (bitsLayerSeparation >>> 32));
        result = prime * result + (int) (bitsNodeSeparation ^ (bitsNodeSeparation >>> 32));
        result = prime * result + (int) (bitsSelfLoopReserve ^ (bitsSelfLoopReserve >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof LayoutOptions))
        {
            return false;
        }
        LayoutOptions other = (LayoutOptions) obj;
        return this.direction == other.direction && Double.doubleToLongBits(this.layerSeparation) == Double.doubleToLongBits(other.layerSeparation)
               && Double.doubleToLongBits(this.nodeSeparation) == Double.doubleToLongBits(other.nodeSeparation)
               && Double.doubleToLongBits(this.selfLoopReserve) == Double.doubleToLongBits(other.selfLoopReserve);
    }

    /**
     * Builder for {@link LayoutOptions}. Each call mutates and returns the same builder instance; {@link #build()} produces an
     * immutable {@link LayoutOptions} snapshot.
     *
     * @author omnaest
     */
    public static interface LayoutOptionsBuilder
    {
        public LayoutOptionsBuilder direction(LayoutDirection direction);

        public LayoutOptionsBuilder layerSeparation(double layerSeparation);

        public LayoutOptionsBuilder nodeSeparation(double nodeSeparation);

        public LayoutOptionsBuilder selfLoopReserve(double selfLoopReserve);

        public LayoutOptions build();
    }

}
