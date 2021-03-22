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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.components.GraphEdgesIndex;
import org.omnaest.utils.graph.internal.index.components.GraphIdentityTokenIndex;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphIndex extends AbstractJSONSerializable
{
    @JsonIgnore
    private GraphIdentityTokenIndex graphIdentityTokenIndex = new GraphIdentityTokenIndex();

    @JsonProperty
    private GraphEdgesIndex graphEdgesIndex = new GraphEdgesIndex();

    @JsonProperty
    private Set<NodeIdentity> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @JsonProperty
    private Set<NodeIdentity> unresolvedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public GraphIndex addNode(NodeIdentity nodeIdentity)
    {
        if (nodeIdentity != null)
        {
            this.nodes.add(nodeIdentity);
            this.unresolvedNodes.add(nodeIdentity);
            this.graphIdentityTokenIndex.addNodeToIdentityTokenIndex(nodeIdentity);
        }
        return this;
    }

    public GraphIndex addNodes(Collection<NodeIdentity> nodeIdentities)
    {
        if (nodeIdentities != null)
        {
            nodeIdentities.forEach(this::addNode);
        }
        return this;

    }

    public GraphIndex addEdge(NodeIdentity from, NodeIdentity to)
    {
        return this.addEdge(EdgeIdentity.of(from, to));
    }

    @JsonIgnore
    public Set<NodeIdentity> getNodes()
    {
        return this.nodes;
    }

    public boolean containsNode(NodeIdentity node)
    {
        return this.nodes.contains(node);
    }

    @JsonIgnore
    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphEdgesIndex.getIncomingNodes(nodeIdentity);
    }

    @JsonIgnore
    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.graphEdgesIndex.getOutgoingNodes(nodeIdentity);
    }

    public GraphIndex addEdge(EdgeIdentity edge)
    {
        if (edge != null)
        {
            this.addNode(edge.getFrom())
                .addNode(edge.getTo());
            this.graphEdgesIndex.addEdge(edge);
        }
        return this;
    }

    @JsonIgnore
    public boolean isUnresolvedNode(NodeIdentity node)
    {
        return node != null && this.unresolvedNodes.contains(node);
    }

    public GraphIndex markNodeAsResolved(NodeIdentity node)
    {
        this.unresolvedNodes.remove(node);
        return this;
    }

    public GraphIndex markNodesAsResolved(Collection<NodeIdentity> nodes)
    {
        Optional.ofNullable(nodes)
                .orElse(Collections.emptyList())
                .forEach(this::markNodeAsResolved);
        return this;
    }

    @JsonIgnore
    public Set<NodeIdentity> getUnresolvedNodes()
    {
        return Collections.unmodifiableSet(this.unresolvedNodes);
    }

    @JsonIgnore
    public boolean hasUnresolvedNodes()
    {
        return !this.unresolvedNodes.isEmpty();
    }

}
