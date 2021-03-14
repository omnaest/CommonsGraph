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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphEdgesIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private Map<NodeIdentity, Set<NodeIdentity>> incomingToOutgoing = new ConcurrentHashMap<>();

    @JsonProperty
    private Map<NodeIdentity, Set<NodeIdentity>> outgoingToIncoming = new ConcurrentHashMap<>();

    public void addEdge(NodeIdentity from, NodeIdentity to)
    {
        this.incomingToOutgoing.computeIfAbsent(from, f -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                               .add(to);
        this.outgoingToIncoming.computeIfAbsent(to, f -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                               .add(from);

    }

    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.outgoingToIncoming.getOrDefault(nodeIdentity, Collections.emptySet());
    }

    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.incomingToOutgoing.getOrDefault(nodeIdentity, Collections.emptySet());
    }

}
