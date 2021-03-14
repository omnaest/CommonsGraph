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
package org.omnaest.utils.graph.internal.index;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.components.GraphEdgesIndex;
import org.omnaest.utils.graph.internal.index.components.GraphIdentityTokenIndex;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private GraphIdentityTokenIndex graphIdentityTokenIndex = new GraphIdentityTokenIndex();

    @JsonProperty
    private GraphEdgesIndex graphEdgesIndex = new GraphEdgesIndex();

    @JsonProperty
    private Set<NodeIdentity> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public GraphIndex addNode(NodeIdentity nodeIdentity)
    {
        if (nodeIdentity != null)
        {
            this.nodes.add(nodeIdentity);
            this.graphIdentityTokenIndex.addNodeToIdentityTokenIndex(nodeIdentity);
        }
        return this;
    }

    public GraphIndex addEdge(NodeIdentity from, NodeIdentity to)
    {
        if (from != null && to != null)
        {
            this.addNode(from)
                .addNode(to);
            this.graphEdgesIndex.addEdge(from, to);
        }
        return this;
    }

    public Set<NodeIdentity> getNodes()
    {
        return this.nodes;
    }

    public boolean containsNode(NodeIdentity node)
    {
        return this.nodes.contains(node);
    }

    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphEdgesIndex.getIncomingNodes(nodeIdentity);
    }

    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphEdgesIndex.getOutgoingNodes(nodeIdentity);
    }

}
