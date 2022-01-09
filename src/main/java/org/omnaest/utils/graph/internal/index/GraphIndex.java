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
import org.omnaest.utils.graph.domain.GraphBuilder.RepositoryProvider;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.internal.GraphImpl;
import org.omnaest.utils.graph.internal.index.components.GraphEdgesIndex;
import org.omnaest.utils.graph.internal.index.components.GraphIdentityTokenIndex;
import org.omnaest.utils.graph.internal.index.components.GraphNodeDataIndex;
import org.omnaest.utils.graph.internal.index.components.GraphNodeDataIndex.NodeData;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see GraphImpl
 * @author omnaest
 */
public class GraphIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private GraphIdentityTokenIndex graphIdentityTokenIndex;

    @JsonProperty
    private GraphEdgesIndex graphEdgesIndex;

    @JsonProperty
    private Set<NodeIdentity> nodes;

    @JsonProperty
    private Set<NodeIdentity> unresolvedNodes;

    @JsonProperty
    private GraphNodeDataIndex graphNodeDataIndex;

    public GraphIndex(RepositoryProvider repositoryProvider)
    {
        super();
        this.graphIdentityTokenIndex = new GraphIdentityTokenIndex(repositoryProvider);
        this.graphEdgesIndex = new GraphEdgesIndex(repositoryProvider);
        this.graphNodeDataIndex = new GraphNodeDataIndex(repositoryProvider);
        this.nodes = repositoryProvider.createSet("nodes");
        this.unresolvedNodes = repositoryProvider.createSet("unresolvedNodes");
    }

    @JsonCreator
    protected GraphIndex()
    {
        this((name, keyType, valueType) -> new ConcurrentHashMap<>());
    }

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

    public GraphIndex addEdge(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes)
    {
        return this.addEdge(EdgeIdentity.of(from, to), attributes);
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
        return this.addEdge(edge, null);
    }

    public GraphIndex addEdge(EdgeIdentity edge, Collection<Attribute> attributes)
    {
        if (edge != null)
        {
            this.addNode(edge.getFrom())
                .addNode(edge.getTo());
            this.graphEdgesIndex.addEdge(edge, attributes);
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.graphEdgesIndex == null) ? 0 : this.graphEdgesIndex.hashCode());
        result = prime * result + ((this.graphIdentityTokenIndex == null) ? 0 : this.graphIdentityTokenIndex.hashCode());
        result = prime * result + ((this.nodes == null) ? 0 : this.nodes.hashCode());
        result = prime * result + ((this.unresolvedNodes == null) ? 0 : this.unresolvedNodes.hashCode());
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
        GraphIndex other = (GraphIndex) obj;
        if (this.graphEdgesIndex == null)
        {
            if (other.graphEdgesIndex != null)
            {
                return false;
            }
        }
        else if (!this.graphEdgesIndex.equals(other.graphEdgesIndex))
        {
            return false;
        }
        if (this.graphIdentityTokenIndex == null)
        {
            if (other.graphIdentityTokenIndex != null)
            {
                return false;
            }
        }
        else if (!this.graphIdentityTokenIndex.equals(other.graphIdentityTokenIndex))
        {
            return false;
        }
        if (this.nodes == null)
        {
            if (other.nodes != null)
            {
                return false;
            }
        }
        else if (!this.nodes.equals(other.nodes))
        {
            return false;
        }
        if (this.unresolvedNodes == null)
        {
            if (other.unresolvedNodes != null)
            {
                return false;
            }
        }
        else if (!this.unresolvedNodes.equals(other.unresolvedNodes))
        {
            return false;
        }
        return true;
    }

    public Optional<Set<Attribute>> getEdgeAttributes(NodeIdentity from, NodeIdentity to)
    {
        return this.graphEdgesIndex.getEdge(from, to);
    }

    public NodeData attachNodeDataToNodeAndGet(NodeIdentity nodeIdentity)
    {
        return this.graphNodeDataIndex.attachNodeDataToNodeAndGet(nodeIdentity);
    }

    public GraphIndex updateNodeData(NodeIdentity nodeIdentity, NodeData nodeData)
    {
        this.graphNodeDataIndex.updateNodeData(nodeIdentity, nodeData);
        return this;
    }

    public Optional<NodeData> getNodeData(NodeIdentity nodeIdentity)
    {
        return this.graphNodeDataIndex.getNodeData(nodeIdentity);
    }

}
