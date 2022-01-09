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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphBuilder.RepositoryProvider;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class GraphEdgesIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private Map<NodeIdentity, NodeIdentitiesAndAttributes> incomingToOutgoing = new ConcurrentHashMap<>();

    @JsonProperty
    private Map<NodeIdentity, NodeIdentitiesAndAttributes> outgoingToIncoming = new ConcurrentHashMap<>();

    public GraphEdgesIndex(RepositoryProvider repositoryProvider)
    {
        super();
        this.incomingToOutgoing = repositoryProvider.createMap("incomingToOutgoing");
        this.outgoingToIncoming = repositoryProvider.createMap("outgoingToIncoming");
    }

    public static class NodeIdentitiesAndAttributes
    {
        @JsonProperty
        private Map<NodeIdentity, Set<Attribute>> nodeIdentityToAttributes = new ConcurrentHashMap<>();

        @JsonCreator
        public NodeIdentitiesAndAttributes(Map<NodeIdentity, Set<Attribute>> nodeIdentityToAttributes)
        {
            super();
            this.nodeIdentityToAttributes = nodeIdentityToAttributes;
        }

        public NodeIdentitiesAndAttributes()
        {
            super();
        }

        public static NodeIdentitiesAndAttributes empty()
        {
            return new NodeIdentitiesAndAttributes();
        }

        @JsonValue
        public Map<NodeIdentity, Set<Attribute>> getNodeIdentityToAttributes()
        {
            return this.nodeIdentityToAttributes;
        }

        @JsonIgnore
        public Set<NodeIdentity> getNodeIdentities()
        {
            return this.nodeIdentityToAttributes.keySet();
        }

        public void add(NodeIdentity nodeIdentity, Collection<Attribute> attributes)
        {
            this.nodeIdentityToAttributes.compute(nodeIdentity, (id, previousAttributes) -> SetUtils.merge(previousAttributes, attributes));
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.nodeIdentityToAttributes == null) ? 0 : this.nodeIdentityToAttributes.hashCode());
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
            if (!(obj instanceof NodeIdentitiesAndAttributes))
            {
                return false;
            }
            NodeIdentitiesAndAttributes other = (NodeIdentitiesAndAttributes) obj;
            if (this.nodeIdentityToAttributes == null)
            {
                if (other.nodeIdentityToAttributes != null)
                {
                    return false;
                }
            }
            else if (!this.nodeIdentityToAttributes.equals(other.nodeIdentityToAttributes))
            {
                return false;
            }
            return true;
        }

        @JsonIgnore
        public Optional<Set<Attribute>> get(NodeIdentity nodeIdentity)
        {
            return Optional.ofNullable(this.nodeIdentityToAttributes.get(nodeIdentity));
        }

    }

    @JsonCreator
    protected GraphEdgesIndex()
    {
        this((name, keyType, valueType) -> new ConcurrentHashMap<>());
    }

    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.outgoingToIncoming.getOrDefault(nodeIdentity, NodeIdentitiesAndAttributes.empty())
                                      .getNodeIdentities();
    }

    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.incomingToOutgoing.getOrDefault(nodeIdentity, NodeIdentitiesAndAttributes.empty())
                                      .getNodeIdentities();
    }

    public GraphEdgesIndex addEdge(EdgeIdentity edge)
    {
        return this.addEdge(edge, null);
    }

    public GraphEdgesIndex addEdge(EdgeIdentity edge, Collection<Attribute> attributes)
    {
        if (edge != null)
        {
            NodeIdentity from = edge.getFrom();
            NodeIdentity to = edge.getTo();
            if (from != null)
            {
                NodeIdentitiesAndAttributes nodeIdentities = this.outgoingToIncoming.computeIfAbsent(to, f -> new NodeIdentitiesAndAttributes());
                nodeIdentities.add(from, attributes);
                this.outgoingToIncoming.put(to, nodeIdentities);
            }
            if (to != null)
            {
                NodeIdentitiesAndAttributes nodeIdentities = this.incomingToOutgoing.computeIfAbsent(from, f -> new NodeIdentitiesAndAttributes());
                nodeIdentities.add(to, attributes);
                this.incomingToOutgoing.put(from, nodeIdentities);
            }
        }
        return this;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.incomingToOutgoing == null) ? 0 : this.incomingToOutgoing.hashCode());
        result = prime * result + ((this.outgoingToIncoming == null) ? 0 : this.outgoingToIncoming.hashCode());
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
        GraphEdgesIndex other = (GraphEdgesIndex) obj;
        if (this.incomingToOutgoing == null)
        {
            if (other.incomingToOutgoing != null)
            {
                return false;
            }
        }
        else if (!this.incomingToOutgoing.equals(other.incomingToOutgoing))
        {
            return false;
        }
        if (this.outgoingToIncoming == null)
        {
            if (other.outgoingToIncoming != null)
            {
                return false;
            }
        }
        else if (!this.outgoingToIncoming.equals(other.outgoingToIncoming))
        {
            return false;
        }
        return true;
    }

    public Optional<Set<Attribute>> getEdge(NodeIdentity from, NodeIdentity to)
    {
        return Optional.ofNullable(this.incomingToOutgoing.get(from))
                       .flatMap(nodeIdentitiesAndAttributes -> nodeIdentitiesAndAttributes.get(to));
    }

}
