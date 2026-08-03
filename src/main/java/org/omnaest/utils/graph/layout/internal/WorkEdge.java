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
import java.util.List;

import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;

/**
 * Mutable working-model edge used only inside the layout engine (never exposed). {@link #getLayoutFrom()}/
 * {@link #getLayoutTo()} may be swapped relative to {@link #getOriginalFrom()}/{@link #getOriginalTo()} once
 * {@link #reverse()} has run during cycle removal; {@link #isReversed()} records that so the final waypoint list can be
 * presented back in original source-to-target order.
 *
 * @author omnaest
 */
final class WorkEdge
{
    private final LayoutEdgeId id;
    private final LayoutNodeId originalFrom;
    private final LayoutNodeId originalTo;

    private LayoutNodeId       layoutFrom;
    private LayoutNodeId       layoutTo;
    private boolean            reversed = false;
    private List<LayoutNodeId> path     = Collections.emptyList();

    WorkEdge(LayoutEdgeId id, LayoutNodeId from, LayoutNodeId to)
    {
        this.id = id;
        this.originalFrom = from;
        this.originalTo = to;
        this.layoutFrom = from;
        this.layoutTo = to;
    }

    LayoutEdgeId getId()
    {
        return this.id;
    }

    LayoutNodeId getOriginalFrom()
    {
        return this.originalFrom;
    }

    LayoutNodeId getOriginalTo()
    {
        return this.originalTo;
    }

    LayoutNodeId getLayoutFrom()
    {
        return this.layoutFrom;
    }

    LayoutNodeId getLayoutTo()
    {
        return this.layoutTo;
    }

    boolean isReversed()
    {
        return this.reversed;
    }

    void reverse()
    {
        LayoutNodeId swap = this.layoutFrom;
        this.layoutFrom = this.layoutTo;
        this.layoutTo = swap;
        this.reversed = true;
    }

    List<LayoutNodeId> getPath()
    {
        return this.path;
    }

    void setPath(List<LayoutNodeId> path)
    {
        this.path = path;
    }

    @Override
    public String toString()
    {
        return "WorkEdge [id=" + this.id + ", layoutFrom=" + this.layoutFrom + ", layoutTo=" + this.layoutTo + ", reversed=" + this.reversed + "]";
    }

}
