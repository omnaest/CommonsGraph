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
package org.omnaest.utils.graph.internal.data.components;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphBuilder.RepositoryProvider;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.internal.data.domain.RawEdge;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author omnaest
 */
public class GraphEdgesIndex extends AbstractJSONSerializable
{
    @JsonProperty
    @JsonSerialize(keyUsing = NodeIdentityKeySerializer.class)
    @JsonDeserialize(keyUsing = NodeIdentityKeyDeserializer.class)
    private Map<NodeIdentity, NodeIdentitiesAndAttributes> incomingToOutgoing = new ConcurrentHashMap<>();

    @JsonProperty
    @JsonSerialize(keyUsing = NodeIdentityKeySerializer.class)
    @JsonDeserialize(keyUsing = NodeIdentityKeyDeserializer.class)
    private Map<NodeIdentity, NodeIdentitiesAndAttributes> outgoingToIncoming = new ConcurrentHashMap<>();

    private static class NodeIdentityKeySerializer extends JsonSerializer<NodeIdentity>
    {
        @Override
        public void serialize(NodeIdentity value, JsonGenerator gen, SerializerProvider serializers) throws IOException
        {
            gen.writeFieldName(JSONHelper.serialize(value));
        }

    }

    private static class NodeIdentityKeyDeserializer extends KeyDeserializer
    {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException
        {
            return JSONHelper.deserializer(NodeIdentity.class)
                             .apply(key);
        }

    }

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

        @JsonIgnore
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

    @JsonIgnore
    public Set<NodeIdentity> getIncomingNodes(NodeIdentity nodeIdentity)
    {
        return this.outgoingToIncoming.getOrDefault(nodeIdentity, NodeIdentitiesAndAttributes.empty())
                                      .getNodeIdentities();
    }

    @JsonIgnore
    public Set<NodeIdentity> getOutgoingNodes(NodeIdentity nodeIdentity)
    {
        return this.incomingToOutgoing.getOrDefault(nodeIdentity, NodeIdentitiesAndAttributes.empty())
                                      .getNodeIdentities();
    }

    @JsonIgnore
    public GraphEdgesIndex addEdge(EdgeIdentity edge)
    {
        return this.addEdge(edge, null);
    }

    @JsonIgnore
    public GraphEdgesIndex addEdge(EdgeIdentity edge, Collection<Attribute> attributes)
    {
        if (edge != null)
        {
            NodeIdentity from = edge.getFrom();
            NodeIdentity to = edge.getTo();
            if (from != null && to != null)
            {
                {
                    NodeIdentitiesAndAttributes nodeIdentities = this.outgoingToIncoming.computeIfAbsent(to, f -> new NodeIdentitiesAndAttributes());
                    nodeIdentities.add(from, attributes);
                    this.outgoingToIncoming.put(to, nodeIdentities);
                }
                {
                    NodeIdentitiesAndAttributes nodeIdentities = this.incomingToOutgoing.computeIfAbsent(from, f -> new NodeIdentitiesAndAttributes());
                    nodeIdentities.add(to, attributes);
                    this.incomingToOutgoing.put(from, nodeIdentities);
                }
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

    @JsonIgnore
    public Optional<Set<Attribute>> getEdge(NodeIdentity from, NodeIdentity to)
    {
        return Optional.ofNullable(this.incomingToOutgoing.get(from))
                       .flatMap(nodeIdentitiesAndAttributes -> nodeIdentitiesAndAttributes.get(to));
    }

    @JsonIgnore
    public Stream<RawEdge> getEdges()
    {
        return this.incomingToOutgoing.entrySet()
                                      .stream()
                                      .flatMap(entry -> entry.getValue()
                                                             .getNodeIdentities()
                                                             .stream()
                                                             .map(to -> new RawEdge(entry.getKey(), to, entry.getValue()
                                                                                                             .get(to)
                                                                                                             .orElse(null))));

    }

}
