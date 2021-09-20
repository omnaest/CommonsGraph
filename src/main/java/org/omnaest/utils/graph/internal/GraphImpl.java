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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphResolver;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphSerializer;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.index.GraphIndex;
import org.omnaest.utils.graph.internal.node.NodeImpl;
import org.omnaest.utils.graph.internal.node.NodesImpl;
import org.omnaest.utils.graph.internal.resolver.GraphResolverImpl;
import org.omnaest.utils.graph.internal.router.GraphRouterImpl;

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
    public GraphRouter newRouter()
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
        return this.graphIndex.toString();
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
                return JSONHelper.serialize(graphIndex);
            }
        };
    }
}
