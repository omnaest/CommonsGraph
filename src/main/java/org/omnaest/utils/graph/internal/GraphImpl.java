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
package org.omnaest.utils.graph.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.OptionalUtils;
import org.omnaest.utils.exception.handler.ExceptionHandler;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphResolver;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphSerializer;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.edge.EdgeImpl;
import org.omnaest.utils.graph.internal.index.GraphIndex;
import org.omnaest.utils.graph.internal.node.NodeImpl;
import org.omnaest.utils.graph.internal.node.NodesImpl;
import org.omnaest.utils.graph.internal.resolver.GraphResolverImpl;
import org.omnaest.utils.graph.internal.router.GraphRouterImpl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @see Graph
 * @author omnaest
 */
public class GraphImpl implements Graph
{
    private GraphIndex          graphIndex;
    private NodeResolverSupport nodeResolverSupport;

    public GraphImpl(GraphIndex graphIndex, NodeResolverSupport nodeResolverSupport)
    {
        super();
        this.graphIndex = graphIndex;
        this.nodeResolverSupport = nodeResolverSupport;
    }

    @Override
    public Stream<Node> stream()
    {
        return this.graphIndex.getNodes()
                              .stream()
                              .map(this::wrapIntoNode);
    }

    private Node wrapIntoNode(NodeIdentity nodeIdentity)
    {
        return new NodeImpl(nodeIdentity, this.graphIndex, this.nodeResolverSupport);
    }

    @Override
    public Optional<Node> findNodeById(NodeIdentity nodeIdentity)
    {
        return Optional.ofNullable(nodeIdentity)
                       .filter(this.graphIndex::containsNode)
                       .map(this::wrapIntoNode);
    }

    @Override
    public GraphRouter routing()
    {
        return new GraphRouterImpl(this);
    }

    @Override
    public int size()
    {
        return this.graphIndex.getNodes()
                              .size();
    }

    @Override
    public Nodes findNodesByIds(Collection<NodeIdentity> nodeIdentities)
    {
        return new NodesImpl(nodeIdentities.stream()
                                           .filter(this.graphIndex::containsNode)
                                           .collect(Collectors.toSet()),
                             this.graphIndex, this.nodeResolverSupport);
    }

    @Override
    public GraphResolver resolver()
    {
        return new GraphResolverImpl(this.graphIndex, this.nodeResolverSupport);
    }

    @Override
    public String toString()
    {
        return Optional.ofNullable(this.graphIndex)
                       .map(GraphIndex::toString)
                       .orElse(null);
    }

    @Override
    public Graph clone()
    {
        GraphIndex clonedGraphIndex = JSONHelper.<GraphIndex>cloner()
                                                .usingKeyDeserializer(NodeIdentity.class, new NodeIdentityKeyDeserializer())
                                                .apply(this.graphIndex);
        return new GraphImpl(clonedGraphIndex, new NodeResolverSupport(clonedGraphIndex));
    }

    public static Graph fromJson(String json)
    {
        GraphIndex clonedGraphIndex = JSONHelper.deserializer(GraphIndex.class)
                                                .withKeyDeserializer(NodeIdentity.class, new NodeIdentityKeyDeserializer())
                                                .withExceptionHandler(ExceptionHandler.rethrowingExceptionHandler())
                                                .apply(json);
        return new GraphImpl(clonedGraphIndex, new NodeResolverSupport(clonedGraphIndex));
    }

    @Override
    public GraphSerializer serialize()
    {
        GraphIndex graphIndex = this.graphIndex;
        return new GraphSerializer()
        {
            @Override
            public String toJson()
            {
                return JSONHelper.serializer(GraphIndex.class)
                                 .withKeySerializer(NodeIdentity.class, new NodeIdentityJsonSerializer())
                                 .apply(graphIndex);
            }
        };
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.graphIndex == null) ? 0 : this.graphIndex.hashCode());
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
        GraphImpl other = (GraphImpl) obj;
        if (this.graphIndex == null)
        {
            if (other.graphIndex != null)
            {
                return false;
            }
        }
        else if (!this.graphIndex.equals(other.graphIndex))
        {
            return false;
        }
        return true;
    }

    @Override
    public Optional<Edge> findEdge(NodeIdentity from, NodeIdentity to)
    {
        return this.graphIndex.getEdgeAttributes(from, to)
                              .flatMap(attributes -> OptionalUtils.both(this.findNodeById(from), this.findNodeById(to))
                                                                  .map(fromAndTo -> new EdgeImpl(fromAndTo.getFirst(), fromAndTo.getSecond(), attributes)));
    }

    private final class NodeIdentityJsonSerializer extends JsonSerializer<NodeIdentity>
    {
        @Override
        public void serialize(NodeIdentity value, JsonGenerator generator, SerializerProvider serializers) throws IOException
        {
            generator.writeFieldName(JSONHelper.serialize(value));
        }
    }

}
