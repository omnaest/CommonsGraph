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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.internal.data.GraphDataIndex;
import org.omnaest.utils.graph.internal.data.components.GraphNodeDataIndex.NodeData;
import org.omnaest.utils.graph.internal.data.filter.GraphNodesFilter;

public class GraphBuilderImpl implements GraphBuilder
{
    private GraphIndexContext graphIndexContext = new GraphIndexContext((name, keyType, valueType) -> new ConcurrentHashMap<>());

    @Override
    public GraphBuilder withRepositoryProvider(RepositoryProvider repositoryProvider)
    {
        if (!this.graphIndexContext.getGraphIndex()
                                   .getNodes()
                                   .isEmpty())
        {
            throw new IllegalStateException("Repository provider can only be changed before any nodes have been added.");
        }

        this.graphIndexContext = new GraphIndexContext(repositoryProvider);
        return this;
    }

    @Override
    public GraphBuilder addNode(NodeIdentity nodeIdentity)
    {
        this.graphIndexContext.getGraphIndex()
                              .addNode(nodeIdentity);
        return this;
    }

    @Override
    public GraphBuilder addNodeWithData(NodeIdentity nodeIdentity, Consumer<NodeDataBuilder> nodeDataBuilderConsumer)
    {
        NodeData nodeData = this.graphIndexContext.getGraphIndex()
                                                  .attachNodeDataToNodeAndGet(nodeIdentity);
        nodeDataBuilderConsumer.accept(this.createNodeDataBuilder(nodeData));
        this.graphIndexContext.getGraphIndex()
                              .updateNodeData(nodeIdentity, nodeData);
        return this.addNode(nodeIdentity);
    }

    private NodeDataBuilder createNodeDataBuilder(NodeData nodeData)
    {
        return new NodeDataBuilder()
        {
            @Override
            public NodeDataBuilder put(String key, Object value)
            {
                nodeData.getData()
                        .put(key, value);
                return this;
            }

            @Override
            public NodeDataBuilder putAll(Map<String, Object> map)
            {
                nodeData.getData()
                        .putAll(map);
                return this;
            }

            @Override
            public NodeDataBuilder clear()
            {
                nodeData.getData()
                        .clear();
                return this;
            }

            @Override
            public NodeDataBuilder putFrom(Object object)
            {
                return this.putAll(JSONHelper.toMap(object));
            }
        };
    }

    @Override
    public GraphBuilder addNodes(Collection<NodeIdentity> nodeIdentities)
    {
        this.graphIndexContext.getGraphIndex()
                              .addNodes(nodeIdentities);
        return this;
    }

    @Override
    public GraphBuilder addNodes(NodeIdentity... nodeIdentities)
    {
        return this.addNodes(Optional.ofNullable(nodeIdentities)
                                     .map(Arrays::asList)
                                     .orElse(Collections.emptyList()));
    }

    @Override
    public GraphBuilder addEdge(NodeIdentity from, NodeIdentity to)
    {
        this.graphIndexContext.getGraphIndex()
                              .addEdge(from, to);
        return this;
    }

    public GraphBuilder addEdgeWithAttributes(EdgeIdentityWithAttributes edge)
    {
        return this.addEdgeWithAttributes(edge.getEdgeIdentity()
                                              .getFrom(),
                                          edge.getEdgeIdentity()
                                              .getTo(),
                                          edge.getAttributes());
    }

    @Override
    public GraphBuilder addEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes)
    {
        this.graphIndexContext.getGraphIndex()
                              .addEdge(from, to, attributes);
        return this;
    }

    @Override
    public GraphBuilder addEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Attribute... attributes)
    {
        return this.addEdgeWithAttributes(from, to, Arrays.asList(attributes));
    }

    @Override
    public GraphBuilder addEdge(EdgeIdentity edgeIdentity)
    {
        this.graphIndexContext.getGraphIndex()
                              .addEdge(edgeIdentity);
        return this;
    }

    @Override
    public GraphBuilder addEdges(Iterable<EdgeIdentity> edgeIdentities)
    {
        Optional.ofNullable(edgeIdentities)
                .orElse(Collections.emptyList())
                .forEach(this::addEdge);
        return this;
    }

    @Override
    public GraphBuilder addEdgesWithAttributes(Iterable<EdgeIdentityWithAttributes> edgeIdentities)
    {
        Optional.ofNullable(edgeIdentities)
                .orElse(Collections.emptyList())
                .forEach(this::addEdgeWithAttributes);
        return this;
    }

    @Override
    public GraphBuilder addBidirectionalEdge(NodeIdentity from, NodeIdentity to)
    {
        return this.addEdge(from, to)
                   .addEdge(to, from);
    }

    @Override
    public GraphBuilder addBidirectionalEdgeWithTags(NodeIdentity from, NodeIdentity to, Collection<Tag> tags)
    {
        return this.addBidirectionalEdgeWithAttributes(from, to, Optional.ofNullable(tags)
                                                                         .orElse(Collections.emptyList())
                                                                         .stream()
                                                                         .map(MapperUtils.identity())
                                                                         .collect(Collectors.toList()));
    }

    @Override
    public GraphBuilder addBidirectionalEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Collection<Attribute> attributes)
    {
        return this.addEdgeWithAttributes(from, to, attributes)
                   .addEdgeWithAttributes(to, from, attributes);
    }

    @Override
    public GraphBuilder addBidirectionalEdgeWithAttributes(NodeIdentity from, NodeIdentity to, Attribute... attributes)
    {
        return this.addBidirectionalEdgeWithAttributes(from, to, Arrays.asList(attributes));
    }

    @Override
    public GraphBuilder addGraph(Graph graph)
    {
        if (graph != null)
        {
            this.addNodes(graph.nodes()
                               .identities())
                .addEdgesWithAttributes(graph.edges()
                                             .identitiesWithAttributes());
        }
        return this;
    }

    @Override
    public Graph build()
    {
        return new GraphImpl(this.graphIndexContext.getGraphIndex(), this.graphIndexContext.getNodeResolverSupport(), GraphNodesFilter.empty());
    }

    @Override
    public GraphBuilder withSingleNodeResolver(SingleNodeResolver nodeResolver)
    {
        this.graphIndexContext.getNodeResolverSupport()
                              .add(nodeResolver);
        return this;
    }

    @Override
    public GraphBuilder withBidirectionalSingleNodeResolver(SingleNodeResolver nodeResolver)
    {
        this.graphIndexContext.getNodeResolverSupport()
                              .addBidirectional(nodeResolver);
        return this;
    }

    @Override
    public GraphBuilder withMultiNodeResolver(MultiNodeResolver nodeResolver)
    {
        this.graphIndexContext.getNodeResolverSupport()
                              .add(nodeResolver);
        return this;
    }

    private static class GraphIndexContext
    {
        private final GraphDataIndex      graphIndex;
        private final NodeResolverSupport nodeResolverSupport;

        public GraphIndexContext(RepositoryProvider repositoryProvider)
        {
            super();
            this.graphIndex = new GraphDataIndex(repositoryProvider);
            this.nodeResolverSupport = new NodeResolverSupport(this.graphIndex);
        }

        public GraphDataIndex getGraphIndex()
        {
            return this.graphIndex;
        }

        public NodeResolverSupport getNodeResolverSupport()
        {
            return this.nodeResolverSupport;
        }

    }

    public static class NodeResolverSupport
    {
        private List<MultiNodeResolver> nodeResolvers = new ArrayList<>();
        private GraphDataIndex          graphIndex;

        public NodeResolverSupport(GraphDataIndex graphIndex)
        {
            this.graphIndex = graphIndex;
        }

        public NodeResolverSupport add(SingleNodeResolver nodeResolver)
        {
            if (nodeResolver != null)
            {
                this.add(nodeResolver.asMultiNodeResolver());
            }
            return this;
        }

        public NodeResolverSupport add(MultiNodeResolver nodeResolver)
        {
            if (nodeResolver != null)
            {
                this.nodeResolvers.add(nodeResolver);
            }
            return this;
        }

        public NodeResolverSupport addBidirectional(SingleNodeResolver nodeResolver)
        {
            if (nodeResolver != null)
            {
                this.add(nodeResolver.asMultiNodeResolver()
                                     .asBidirectionalMultiNodeResolver());
            }
            return this;
        }

        public NodeResolverSupport resolve(Set<NodeIdentity> nodeIdentities)
        {
            if (nodeIdentities != null)
            {
                Set<NodeIdentity> unresolvedNodes = nodeIdentities.stream()
                                                                  .filter(this.graphIndex::isUnresolvedNode)
                                                                  .collect(Collectors.toSet());
                this.nodeResolvers.forEach(nodeResolver -> nodeResolver.apply(unresolvedNodes)
                                                                       .forEach(this.graphIndex::addEdge));
                this.graphIndex.markNodesAsResolved(unresolvedNodes);
            }
            return this;
        }

        public boolean isLazyLoadingActive()
        {
            return !this.nodeResolvers.isEmpty();
        }

        public NodeResolverSupport resolve(NodeIdentity nodeIdentity)
        {
            return this.resolve(SetUtils.toSet(nodeIdentity));
        }

    }

}
