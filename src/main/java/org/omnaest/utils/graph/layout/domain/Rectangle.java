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
 * Immutable axis-aligned rectangle defined by a {@link #getPosition()} (top/left corner) and a {@link #getSize()}.
 *
 * @see #of(double, double, double, double)
 * @author omnaest
 */
public final class Rectangle
{
    private final Point position;
    private final Size  size;

    private Rectangle(Point position, Size size)
    {
        this.position = position;
        this.size = size;
    }

    public static Rectangle of(Point position, Size size)
    {
        return new Rectangle(position, size);
    }

    public static Rectangle of(double x, double y, double width, double height)
    {
        return new Rectangle(Point.of(x, y), Size.of(width, height));
    }

    public Point getPosition()
    {
        return this.position;
    }

    public Size getSize()
    {
        return this.size;
    }

    public double getX()
    {
        return this.position.getX();
    }

    public double getY()
    {
        return this.position.getY();
    }

    public double getWidth()
    {
        return this.size.getWidth();
    }

    public double getHeight()
    {
        return this.size.getHeight();
    }

    public double getRight()
    {
        return this.getX() + this.getWidth();
    }

    public double getBottom()
    {
        return this.getY() + this.getHeight();
    }

    public double getCenterX()
    {
        return this.getX() + this.getWidth() / 2.0;
    }

    public double getCenterY()
    {
        return this.getY() + this.getHeight() / 2.0;
    }

    /**
     * Returns true if this {@link Rectangle} and the given one overlap (share any interior area).
     *
     * @param other
     * @return
     */
    public boolean overlaps(Rectangle other)
    {
        if (other == null)
        {
            return false;
        }
        boolean disjoint = this.getRight() <= other.getX() || other.getRight() <= this.getX() || this.getBottom() <= other.getY()
                           || other.getBottom() <= this.getY();
        return !disjoint;
    }

    @Override
    public String toString()
    {
        return "Rectangle [position=" + this.position + ", size=" + this.size + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.position == null) ? 0 : this.position.hashCode());
        result = prime * result + ((this.size == null) ? 0 : this.size.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Rectangle))
        {
            return false;
        }
        Rectangle other = (Rectangle) obj;
        return java.util.Objects.equals(this.position, other.position) && java.util.Objects.equals(this.size, other.size);
    }

}
