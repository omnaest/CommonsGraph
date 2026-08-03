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

/**
 * Immutable width/height pair.
 *
 * @see #of(double, double)
 * @author omnaest
 */
public final class Size
{
    private final double width;
    private final double height;

    private Size(double width, double height)
    {
        this.width = width;
        this.height = height;
    }

    public static Size of(double width, double height)
    {
        return new Size(width, height);
    }

    public double getWidth()
    {
        return this.width;
    }

    public double getHeight()
    {
        return this.height;
    }

    @Override
    public String toString()
    {
        return "Size [width=" + this.width + ", height=" + this.height + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long bitsWidth = Double.doubleToLongBits(this.width);
        long bitsHeight = Double.doubleToLongBits(this.height);
        result = prime * result + (int) (bitsWidth ^ (bitsWidth >>> 32));
        result = prime * result + (int) (bitsHeight ^ (bitsHeight >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Size))
        {
            return false;
        }
        Size other = (Size) obj;
        return Double.doubleToLongBits(this.width) == Double.doubleToLongBits(other.width)
               && Double.doubleToLongBits(this.height) == Double.doubleToLongBits(other.height);
    }

}
