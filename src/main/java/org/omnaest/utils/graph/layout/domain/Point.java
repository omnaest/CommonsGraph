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
 * Immutable 2D point.
 *
 * @see #of(double, double)
 * @author omnaest
 */
public final class Point
{
    private final double x;
    private final double y;

    private Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public static Point of(double x, double y)
    {
        return new Point(x, y);
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    /**
     * Returns a new {@link Point} translated by the given deltas. This instance is left unmodified.
     *
     * @param dx
     * @param dy
     * @return
     */
    public Point translate(double dx, double dy)
    {
        return new Point(this.x + dx, this.y + dy);
    }

    @Override
    public String toString()
    {
        return "Point [x=" + this.x + ", y=" + this.y + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        long bitsX = Double.doubleToLongBits(this.x);
        long bitsY = Double.doubleToLongBits(this.y);
        result = prime * result + (int) (bitsX ^ (bitsX >>> 32));
        result = prime * result + (int) (bitsY ^ (bitsY >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Point))
        {
            return false;
        }
        Point other = (Point) obj;
        return Double.doubleToLongBits(this.x) == Double.doubleToLongBits(other.x)
               && Double.doubleToLongBits(this.y) == Double.doubleToLongBits(other.y);
    }

}
