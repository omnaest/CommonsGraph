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
package org.omnaest.utils.graph.domain.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.utils.ListUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identity of a {@link Node} which can be used for {@link #equals(Object)} and {@link #hashCode()} operations.
 * 
 * @author omnaest
 */
public class NodeIdentity implements Supplier<List<String>>, Comparable<NodeIdentity>
{
    @JsonProperty
    private List<String> ids;

    @JsonCreator
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

    public static NodeIdentity of(Collection<String> ids)
    {
        return new NodeIdentity(Optional.ofNullable(ids)
                                        .<List<String>>map(ArrayList::new)
                                        .orElse(Collections.emptyList()));
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
                return this.add(Optional.ofNullable(value)
                                        .map(Enum::name)
                                        .orElse(null));
            }

            @Override
            public NodeIdentityBuilder add(int token)
            {
                return this.add(String.valueOf(token));
            }

            @Override
            public NodeIdentity build()
            {
                return NodeIdentity.of(this.ids.toArray(new String[this.ids.size()]));
            }

            @Override
            public NodeIdentityBuilder add(boolean value)
            {
                return this.add(String.valueOf(value));
            }

        };
    }

    public static interface NodeIdentityBuilder
    {
        public NodeIdentityBuilder add(String token);

        public NodeIdentityBuilder add(int token);

        public NodeIdentityBuilder add(Enum<?> value);

        public NodeIdentityBuilder add(boolean value);

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
    @JsonIgnore
    @JsonValue
    public List<String> get()
    {
        return this.ids;
    }

    /**
     * Returns the first id
     * 
     * @return
     */
    @JsonIgnore
    public String getPrimaryId()
    {
        return ListUtils.first(this.ids);
    }

    @JsonIgnore
    public Optional<String> getSecondaryId()
    {
        return this.getNthId(1);
    }

    /**
     * Returns the nth id.<br>
     * <br>
     * index = 0, 1, 2, ...
     * 
     * @param index
     * @return
     */
    @JsonIgnore
    public Optional<String> getNthId(int index)
    {
        return ListUtils.getOptional(this.ids, index);
    }

    @JsonIgnore
    public IdMapper getPrimaryIdAs()
    {
        return this.getNthIdAs(0);
    }

    @JsonIgnore
    public IdMapper getSecondaryIdAs()
    {
        return this.getNthIdAs(1);
    }

    @JsonIgnore
    public IdMapper getTertiaryIdAs()
    {
        return this.getNthIdAs(2);
    }

    /**
     * Similar to {@link #getNthId(int)} but allows to map to other types like enums.
     * 
     * @param index
     * @return
     */
    @JsonIgnore
    public IdMapper getNthIdAs(int index)
    {
        return new IdMapper(this.getNthId(index));
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

    public int size()
    {
        return this.ids.size();
    }

    @Override
    public int compareTo(NodeIdentity otherNodeIdentity)
    {
        if (otherNodeIdentity == null)
        {
            return 1;
        }
        else
        {
            for (int ii = 0; ii < Math.max(this.ids.size(), otherNodeIdentity.size()); ii++)
            {
                Optional<String> currentToken = this.getNthId(ii);
                Optional<String> otherToken = otherNodeIdentity.getNthId(ii);
                if (currentToken.isPresent() && !otherToken.isPresent())
                {
                    return 1;
                }
                else if (!currentToken.isPresent() && otherToken.isPresent())
                {
                    return -1;
                }
                else
                {
                    int compareResult = currentToken.get()
                                                    .compareTo(otherToken.get());
                    if (compareResult != 0)
                    {
                        return compareResult;
                    }

                    continue;
                }
            }

            return 0;
        }
    }

    public static class IdMapper
    {
        private Optional<String> id;

        protected IdMapper(Optional<String> id)
        {
            super();
            this.id = id;
        }

        public <E extends Enum<E>> Optional<E> enumValue(Class<E> enumType)
        {
            return this.id.map(identifier -> Enum.valueOf(enumType, identifier));
        }

        public Optional<Boolean> booleanValue()
        {
            return this.id.map(Boolean::valueOf);
        }

        public Optional<String> stringValue()
        {
            return this.id.map(String::valueOf);
        }

        public <R> Optional<R> mappedTo(Function<String, R> mapper)
        {
            return this.id.map(mapper);
        }

        public Optional<Integer> intValue()
        {
            return this.id.map(Integer::valueOf);
        }
    }

}
