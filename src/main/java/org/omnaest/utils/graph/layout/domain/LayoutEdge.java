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
 * An edge passed into a {@link LayoutGraph}. Carries its OWN {@link #getId()}, distinct from its {@link #getFrom()}/
 * {@link #getTo()} pair - this is what allows two or more distinct edges between the same ordered pair of nodes to be
 * represented and routed independently (multi-edge support).
 *
 * @see #of(LayoutEdgeId, LayoutNodeId, LayoutNodeId)
 * @author omnaest
 */
public final class LayoutEdge
{
    private final LayoutEdgeId id;
    private final LayoutNodeId from;
    private final LayoutNodeId to;

    private LayoutEdge(LayoutEdgeId id, LayoutNodeId from, LayoutNodeId to)
    {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.from = Objects.requireNonNull(from, "from must not be null");
        this.to = Objects.requireNonNull(to, "to must not be null");
    }

    public static LayoutEdge of(LayoutEdgeId id, LayoutNodeId from, LayoutNodeId to)
    {
        return new LayoutEdge(id, from, to);
    }

    public LayoutEdgeId getId()
    {
        return this.id;
    }

    public LayoutNodeId getFrom()
    {
        return this.from;
    }

    public LayoutNodeId getTo()
    {
        return this.to;
    }

    @Override
    public String toString()
    {
        return "LayoutEdge [id=" + this.id + ", from=" + this.from + ", to=" + this.to + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.from.hashCode();
        result = prime * result + this.to.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof LayoutEdge))
        {
            return false;
        }
        LayoutEdge other = (LayoutEdge) obj;
        return this.id.equals(other.id) && this.from.equals(other.from) && this.to.equals(other.to);
    }

}
