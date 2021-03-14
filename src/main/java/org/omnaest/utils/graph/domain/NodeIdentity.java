/*******************************************************************************
 * Copyright 2021 Danny Kunz
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
package org.omnaest.utils.graph.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.omnaest.utils.ListUtils;

/**
 * Identity of a {@link Node} which can be used for {@link #equals(Object)} and {@link #hashCode()} operations.
 * 
 * @author omnaest
 */
public class NodeIdentity implements Supplier<List<String>>
{
    private List<String> ids;

    protected NodeIdentity(List<String> ids)
    {
        super();
        this.ids = ids;
    }

    public static NodeIdentity of(String id)
    {
        return new NodeIdentity(Arrays.asList(id));
    }

    public static NodeIdentity of(Enum<?> value)
    {
        return new NodeIdentity(Arrays.asList(value.name()));
    }

    public static NodeIdentity of(String... ids)
    {
        return new NodeIdentity(Arrays.asList(ids));
    }

    public static NodeIdentityBuilder builder()
    {
        return new NodeIdentityBuilder()
        {
            private List<String> ids = new ArrayList<>();

            @Override
            public NodeIdentityBuilder add(String token)
            {
                this.ids.add(token);
                return this;
            }

            @Override
            public NodeIdentityBuilder add(Enum<?> value)
            {
                return this.add(value.name());
            }

            @Override
            public NodeIdentity build()
            {
                return NodeIdentity.of(this.ids.toArray(new String[this.ids.size()]));
            }
        };
    }

    public static interface NodeIdentityBuilder
    {
        public NodeIdentityBuilder add(String token);

        public NodeIdentityBuilder add(Enum<?> value);

        public NodeIdentity build();
    }

    @Override
    public String toString()
    {
        return "[" + this.ids + "]";
    }

    /**
     * Returns all id tokens
     */
    @Override
    public List<String> get()
    {
        return this.ids;
    }

    /**
     * Returns the first id
     * 
     * @return
     */
    public String getPrimaryId()
    {
        return ListUtils.first(this.ids);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.ids == null) ? 0 : this.ids.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        NodeIdentity other = (NodeIdentity) obj;
        if (this.ids == null)
        {
            if (other.ids != null)
            {
                return false;
            }
        }
        else if (!this.ids.equals(other.ids))
        {
            return false;
        }
        return true;
    }

}
