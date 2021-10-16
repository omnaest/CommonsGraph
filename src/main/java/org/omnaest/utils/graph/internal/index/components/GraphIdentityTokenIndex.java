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
package org.omnaest.utils.graph.internal.index.components;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.graph.domain.GraphBuilder.RepositoryProvider;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphIdentityTokenIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private Map<Integer, Map<String, Set<NodeIdentity>>> identityTokenIndexToTokenToNodes;

    @JsonIgnore
    private RepositoryProvider repositoryProvider;

    public GraphIdentityTokenIndex(RepositoryProvider repositoryProvider)
    {
        super();
        this.repositoryProvider = repositoryProvider;
        this.identityTokenIndexToTokenToNodes = repositoryProvider.createMap("identityTokenIndexToTokenToNodes");
    }

    @JsonCreator
    protected GraphIdentityTokenIndex()
    {
        this((name, keyType, valueType) -> new ConcurrentHashMap<>());
    }

    public void addNodeToIdentityTokenIndex(NodeIdentity nodeIdentity)
    {
        Optional.ofNullable(nodeIdentity.get())
                .orElse(Collections.emptyList())
                .stream()
                .map(MapperUtils.withIntCounter())
                .forEach(tokenAndIndex ->
                {
                    int index = tokenAndIndex.getSecond();
                    String token = tokenAndIndex.getFirst();
                    Map<String, Set<NodeIdentity>> tokenIndexRepository = this.identityTokenIndexToTokenToNodes.computeIfAbsent(index,
                                                                                                                                i -> this.repositoryProvider.createMap("identityTokenIndexToTokenToNodes"
                                                                                                                                        + i));
                    Set<NodeIdentity> associatedNodes = tokenIndexRepository.computeIfAbsent(token, t -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    associatedNodes.add(nodeIdentity);
                    tokenIndexRepository.put(token, associatedNodes);
                });
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.identityTokenIndexToTokenToNodes == null) ? 0 : this.identityTokenIndexToTokenToNodes.hashCode());
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
        GraphIdentityTokenIndex other = (GraphIdentityTokenIndex) obj;
        if (this.identityTokenIndexToTokenToNodes == null)
        {
            if (other.identityTokenIndexToTokenToNodes != null)
            {
                return false;
            }
        }
        else if (!this.identityTokenIndexToTokenToNodes.equals(other.identityTokenIndexToTokenToNodes))
        {
            return false;
        }
        return true;
    }

}
