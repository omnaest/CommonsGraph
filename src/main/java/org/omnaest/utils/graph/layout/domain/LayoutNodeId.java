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
 * Identity of a {@link LayoutNode}, wrapping a single String token. Comparable so every internal sort in the layout engine has a
 * deterministic, explicit final tie-breaker.
 *
 * @see #of(String)
 * @author omnaest
 */
public final class LayoutNodeId implements Comparable<LayoutNodeId>
{
    private final String value;

    private LayoutNodeId(String value)
    {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static LayoutNodeId of(String value)
    {
        return new LayoutNodeId(value);
    }

    public String getValue()
    {
        return this.value;
    }

    @Override
    public int compareTo(LayoutNodeId other)
    {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString()
    {
        return "LayoutNodeId [" + this.value + "]";
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
        if (!(obj instanceof LayoutNodeId))
        {
            return false;
        }
        LayoutNodeId other = (LayoutNodeId) obj;
        return this.value.equals(other.value);
    }

}
