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
 * Identity of a {@link LayoutEdge}, wrapping a single String token. Distinct from {@link LayoutNodeId}. Two or more
 * {@link LayoutEdge}s may share the same {@link LayoutEdge#getFrom()}/{@link LayoutEdge#getTo()} pair as long as their ids
 * differ - this is what makes multi-edges representable.
 *
 * @see #of(String)
 * @author omnaest
 */
public final class LayoutEdgeId implements Comparable<LayoutEdgeId>
{
    private final String value;

    private LayoutEdgeId(String value)
    {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static LayoutEdgeId of(String value)
    {
        return new LayoutEdgeId(value);
    }

    public String getValue()
    {
        return this.value;
    }

    @Override
    public int compareTo(LayoutEdgeId other)
    {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString()
    {
        return "LayoutEdgeId [" + this.value + "]";
    }

    @Override
    public int hashCode()
    {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof LayoutEdgeId))
        {
            return false;
        }
        LayoutEdgeId other = (LayoutEdgeId) obj;
        return this.value.equals(other.value);
    }

}
