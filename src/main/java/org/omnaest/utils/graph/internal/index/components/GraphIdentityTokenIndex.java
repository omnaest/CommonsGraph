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
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphIdentityTokenIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private Map<Integer, Map<String, Set<NodeIdentity>>> identityTokenIndexToTokenToNodes = new ConcurrentHashMap<>();

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
                    this.identityTokenIndexToTokenToNodes.computeIfAbsent(index, i -> new ConcurrentHashMap<>())
                                                         .computeIfAbsent(token, t -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                                                         .add(nodeIdentity);
                });
    }
}
