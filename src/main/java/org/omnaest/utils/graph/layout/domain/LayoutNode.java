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
 * A node passed into a {@link LayoutGraph}: an identity plus the dimensions the layout engine should reserve for it. Carries
 * no text, no styling - dimensions only.
 *
 * @see #of(LayoutNodeId, Size)
 * @author omnaest
 */
public final class LayoutNode
{
    private final LayoutNodeId id;
    private final Size         size;

    private LayoutNode(LayoutNodeId id, Size size)
    {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.size = Objects.requireNonNull(size, "size must not be null");
    }

    public static LayoutNode of(LayoutNodeId id, Size size)
    {
        return new LayoutNode(id, size);
    }

    public LayoutNodeId getId()
    {
        return this.id;
    }

    public Size getSize()
    {
        return this.size;
    }

    @Override
    public String toString()
    {
        return "LayoutNode [id=" + this.id + ", size=" + this.size + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.size.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof LayoutNode))
        {
            return false;
        }
        LayoutNode other = (LayoutNode) obj;
        return this.id.equals(other.id) && this.size.equals(other.size);
    }

}
