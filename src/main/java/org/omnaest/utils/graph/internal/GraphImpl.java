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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.OptionalUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.exception.handler.ExceptionHandler;
import org.omnaest.utils.graph.GraphUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphResolver;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphSerializer;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.edge.Edges;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.node.Nodes;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.data.GraphDataIndex;
import org.omnaest.utils.graph.internal.data.GraphDataIndexAccessor;
import org.omnaest.utils.graph.internal.data.filter.GraphNodesFilter;
import org.omnaest.utils.graph.internal.edge.EdgeImpl;
import org.omnaest.utils.graph.internal.edge.EdgesImpl;
import org.omnaest.utils.graph.internal.index.GraphIndexNodeSelectorImpl;
import org.omnaest.utils.graph.internal.node.NodeImpl;
import org.omnaest.utils.graph.internal.node.NodesImpl;
import org.omnaest.utils.graph.internal.resolver.GraphResolverImpl;
import org.omnaest.utils.graph.internal.router.GraphRouterImpl;
import org.omnaest.utils.graph.internal.serialization.NodeIdentityKeyDeserializer;
import org.omnaest.utils.graph.internal.serialization.SIFUtils;
import org.omnaest.utils.graph.internal.serialization.SIFUtils.SIFResourceBuilder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @see Graph
 * @author omnaest
 */
public class GraphImpl implements Graph
{
    private NodeResolverSupport    nodeResolverSupport;
    private GraphDataIndexAccessor graphIndexAccessor;

    public GraphImpl(GraphDataIndex graphIndex, NodeResolverSupport nodeResolverSupport, GraphNodesFilter graphNodesFilter)
    {
        super();
        this.nodeResolverSupport = nodeResolverSupport;
        this.graphIndexAccessor = new GraphDataIndexAccessor(graphIndex, graphNodesFilter);
    }

    @Override
    public Stream<Node> stream()
    {
        return this.graphIndexAccessor.getNodes()
                                      .stream()
                                      .map(this::wrapIntoNode);
    }

    private Node wrapIntoNode(NodeIdentity nodeIdentity)
    {
        return new NodeImpl(nodeIdentity, this.graphIndexAccessor, this.nodeResolverSupport);
    }

    @Override
    public Optional<Node> findNodeById(NodeIdentity nodeIdentity)
    {
        return Optional.ofNullable(nodeIdentity)
                       .filter(this.graphIndexAccessor::containsNode)
                       .map(this::wrapIntoNode);
    }

    @Override
    public boolean contains(NodeIdentity nodeIdentity)
    {
        return this.findNodeById(nodeIdentity)
                   .isPresent();
    }

    @Override
    public boolean containsAny(NodeIdentity... nodeIdentities)
    {
        return Optional.ofNullable(nodeIdentities)
                       .map(Arrays::asList)
                       .orElse(Collections.emptyList())
                       .stream()
                       .anyMatch(this::contains);
    }

    @Override
    public GraphRouter routing()
    {
        return new GraphRouterImpl(this);
    }

    @Override
    public int size()
    {
        return this.graphIndexAccessor.getNodes()
                                      .size();
    }

    @Override
    public Nodes findNodesByIds(Collection<NodeIdentity> nodeIdentities)
    {
        return new NodesImpl(nodeIdentities.stream()
                                           .filter(this.graphIndexAccessor::containsNode)
                                           .collect(Collectors.toSet()),
                             this.graphIndexAccessor, this.nodeResolverSupport);
    }

    @Override
    public Nodes findNodesByIds(NodeIdentity... nodeIdentities)
    {
        return this.findNodesByIds(Optional.ofNullable(nodeIdentities)
                                           .map(Arrays::asList)
                                           .orElse(Collections.emptyList()));
    }

    @Override
    public GraphResolver resolver()
    {
        return new GraphResolverImpl(this.graphIndexAccessor, this.nodeResolverSupport);
    }

    @Override
    public String toString()
    {
        return Optional.ofNullable(this.graphIndexAccessor)
                       .map(GraphDataIndexAccessor::toString)
                       .orElse(null);
    }

    @Override
    public Graph clone()
    {
        GraphDataIndex clonedGraphIndex = JSONHelper.<GraphDataIndex>cloner()
                                                    .usingKeyDeserializer(NodeIdentity.class, new NodeIdentityKeyDeserializer())
                                                    .apply(this.graphIndexAccessor.getGraphDataIndex());
        return new GraphImpl(clonedGraphIndex, new NodeResolverSupport(clonedGraphIndex), this.graphIndexAccessor.getGraphNodesFilter());
    }

    public static Graph fromJson(String json)
    {
        GraphDataIndex clonedGraphIndex = JSONHelper.deserializer(GraphDataIndex.class)
                                                    .withKeyDeserializer(NodeIdentity.class, new NodeIdentityKeyDeserializer())
                                                    .withExceptionHandler(ExceptionHandler.rethrowingExceptionHandler())
                                                    .apply(json);
        return new GraphImpl(clonedGraphIndex, new NodeResolverSupport(clonedGraphIndex), GraphNodesFilter.empty());
    }

    @Override
    public GraphSerializer serialize()
    {
        GraphDataIndex graphIndex = this.graphIndexAccessor.getGraphDataIndex();
        Graph graph = this;
        return new GraphSerializer()
        {
            @Override
            public String toJson()
            {
                return JSONHelper.serializer(GraphDataIndex.class)
                                 .withKeySerializer(NodeIdentity.class, new NodeIdentityJsonSerializer())
                                 .apply(graphIndex);
            }

            @Override
            public SIFSerializer toSif()
            {

                return new SIFSerializer()
                {
                    private Function<Node, String> labelProvider = node -> JSONHelper.serialize(node.getIdentity());

                    private SIFUtils.SIFResource buildRawSifResource()
                    {
                        SIFResourceBuilder builder = SIFUtils.builder();
                        graph.edges()
                             .forEach(edge -> builder.addEdge(this.labelProvider.apply(edge.getFrom()), edge.getTags()
                                                                                                            .stream()
                                                                                                            .map(Tag::getKey)
                                                                                                            .collect(Collectors.joining(",")),
                                                              this.labelProvider.apply(edge.getTo())));
                        graph.nodes()
                             .stream()
                             .filter(Node::isDetached)
                             .forEach(node -> builder.addEdge(this.labelProvider.apply(node), "", ""));
                        return builder.build();
                    }

                    @Override
                    public String get()
                    {
                        return this.buildRawSifResource()
                                   .get();
                    }

                    @Override
                    public SIFSerializer writeInto(File file)
                    {
                        this.buildRawSifResource()
                            .writeInto(file);
                        return this;
                    }

                    @Override
                    public SIFSerializer withLabelProvider(Function<Node, String> labelProvider)
                    {
                        this.labelProvider = labelProvider;
                        return this;
                    }
                };
            }
        };
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.graphIndexAccessor == null) ? 0 : this.graphIndexAccessor.hashCode());
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
        if (!(obj instanceof GraphImpl))
        {
            return false;
        }
        GraphImpl other = (GraphImpl) obj;
        if (this.graphIndexAccessor == null)
        {
            if (other.graphIndexAccessor != null)
            {
                return false;
            }
        }
        else if (!this.graphIndexAccessor.equals(other.graphIndexAccessor))
        {
            return false;
        }
        return true;
    }

    @Override
    public Optional<Edge> findEdge(NodeIdentity from, NodeIdentity to)
    {
        return this.graphIndexAccessor.getEdgeAttributes(from, to)
                                      .flatMap(attributes -> OptionalUtils.both(this.findNodeById(from), this.findNodeById(to))
                                                                          .map(fromAndTo -> new EdgeImpl(fromAndTo.getFirst(), fromAndTo.getSecond(),
                                                                                                         attributes)));
    }

    @Override
    public Optional<Edge> findEdge(EdgeIdentity edgeIdentity)
    {
        return this.findEdge(edgeIdentity.getFrom(), edgeIdentity.getTo());
    }

    private static class NodeIdentityJsonSerializer extends JsonSerializer<NodeIdentity>
    {
        @Override
        public void serialize(NodeIdentity value, JsonGenerator generator, SerializerProvider serializers) throws IOException
        {
            generator.writeFieldName(JSONHelper.serialize(value));
        }
    }

    @Override
    public SubGraphBuilder subGraph()
    {
        return new SubGraphBuilder()
        {
            private Optional<Set<NodeIdentity>> excludedNodes = Optional.empty();
            private Optional<Set<NodeIdentity>> includedNodes = Optional.empty();

            @Override
            public SubGraphBuilder withExcludedNodes(Collection<NodeIdentity> nodeIdentities)
            {
                this.initializeExcludedNodesAndGet()
                    .addAll(Optional.ofNullable(nodeIdentities)
                                    .orElse(Collections.emptyList()));
                return this;
            }

            private Set<NodeIdentity> initializeExcludedNodesAndGet()
            {
                if (!this.excludedNodes.isPresent())
                {
                    this.excludedNodes = Optional.of(new HashSet<>());
                }
                return this.excludedNodes.get();
            }

            private Set<NodeIdentity> initializeIncludedNodesAndGet()
            {
                if (!this.includedNodes.isPresent())
                {
                    this.includedNodes = Optional.of(new HashSet<>());
                }
                return this.includedNodes.get();
            }

            @Override
            public SubGraphBuilder withExcludedNodes(NodeIdentity... nodeIdentities)
            {
                return this.withExcludedNodes(Arrays.asList(nodeIdentities));
            }

            @Override
            public SubGraphBuilder withIncludedNodes(Collection<NodeIdentity> nodeIdentities)
            {
                this.initializeIncludedNodesAndGet()
                    .addAll(Optional.ofNullable(nodeIdentities)
                                    .orElse(Collections.emptyList()));
                return this;
            }

            @Override
            public SubGraphBuilder withIncludedNodes(NodeIdentity... nodeIdentities)
            {
                return this.withIncludedNodes(Arrays.asList(nodeIdentities));
            }

            @Override
            public Graph build()
            {
                return new GraphImpl(GraphImpl.this.graphIndexAccessor.getGraphDataIndex(), GraphImpl.this.nodeResolverSupport,
                                     new GraphNodesFilter(this.excludedNodes, this.includedNodes));
            }
        };
    }

    @Override
    public GraphIndexNodeSelector index()
    {
        return new GraphIndexNodeSelectorImpl(this);
    }

    @Override
    public Edges edges()
    {
        return new EdgesImpl(this.graphIndexAccessor.getEdges()
                                                    .map(edge -> OptionalUtils.both(this.findNodeById(edge.getFrom()), this.findNodeById(edge.getTo()))
                                                                              .map(fromAndTo -> new EdgeImpl(fromAndTo.getFirst(), fromAndTo.getSecond(),
                                                                                                             edge.getAttributes())))
                                                    .filter(PredicateUtils.filterNonEmptyOptional())
                                                    .map(MapperUtils.mapOptionalToValue())
                                                    .collect(Collectors.toList()));
    }

    @Override
    public Nodes nodes()
    {
        return this.createNodesByIdentities(this.graphIndexAccessor.getNodes());
    }

    private NodesImpl createNodesByIdentities(Set<NodeIdentity> nodeIdentities)
    {
        return new NodesImpl(nodeIdentities, this.graphIndexAccessor, this.nodeResolverSupport);
    }

    @Override
    public Nodes startNodes()
    {
        return this.determineNonForwardNodes(Node::getIncomingNodes);
    }

    @Override
    public Nodes endNodes()
    {
        return this.determineNonForwardNodes(Node::getOutgoingNodes);
    }

    private Nodes determineNonForwardNodes(Function<Node, Nodes> forwardFunction)
    {
        return this.createNodesByIdentities(this.stream()
                                                .filter(node -> forwardFunction.apply(node)
                                                                               .hasNone())
                                                .map(Node::getIdentity)
                                                .collect(Collectors.toSet()));
    }

    @Override
    public GraphTransformer transform()
    {
        Stream<Node> initialNodes = this.stream();
        return new GraphTransformer()
        {
            private List<Predicate<Node>>              nodeInclusionFilters     = new ArrayList<>();
            private List<Function<Node, NodeIdentity>> nodeMappers              = new ArrayList<>();
            private Set<NodeIdentity>                  additionalNodeIdentities = new HashSet<>();
            private Set<EdgeIdentity>                  additionalEdgeIdentities = new HashSet<>();

            @Override
            public GraphTransformer filter(Predicate<Node> nodeInclusionFilter)
            {
                if (nodeInclusionFilter != null)
                {
                    this.nodeInclusionFilters.add(nodeInclusionFilter);
                }
                return this;
            }

            @Override
            public Graph collect()
            {
                List<Node> nodes = initialNodes.filter(PredicateUtils.all(this.nodeInclusionFilters))
                                               .collect(Collectors.toList());
                Function<Node, Stream<NodeIdentity>> nodeToIdentityFlattener = this.determineNodeToIdentityFlattener();
                return GraphUtils.builder()
                                 .addNodes(nodes.stream()
                                                .flatMap(nodeToIdentityFlattener)
                                                .collect(Collectors.toList()))
                                 .addNodes(this.additionalNodeIdentities)
                                 .addEdges(nodes.stream()
                                                .map(Node::getAllEdges)
                                                .flatMap(Edges::stream)
                                                .filter(edge -> edge.stream()
                                                                    .allMatch(PredicateUtils.all(this.nodeInclusionFilters)))
                                                .flatMap(edge -> StreamUtils.cartesianProductOf(nodeToIdentityFlattener.apply(edge.getFrom()),
                                                                                                nodeToIdentityFlattener.apply(edge.getTo()))
                                                                            .map(fromAndTo -> EdgeIdentity.of(fromAndTo.getFirst(), fromAndTo.getSecond())))
                                                .collect(Collectors.toList()))
                                 .addEdges(this.additionalEdgeIdentities)
                                 .build();
            }

            private Function<Node, Stream<NodeIdentity>> determineNodeToIdentityFlattener()
            {
                if (this.nodeMappers.isEmpty())
                {
                    return node -> Stream.of(node.getIdentity());
                }
                else
                {
                    Map<NodeIdentity, Set<NodeIdentity>> nodeToReplacedNodes = new HashMap<>();
                    return node ->
                    {
                        return Optional.ofNullable(nodeToReplacedNodes.get(node.getIdentity()))
                                       .orElseGet(() ->
                                       {
                                           Set<NodeIdentity> replacedNodes = this.nodeMappers.stream()
                                                                                             .map(mapper -> mapper.apply(node))
                                                                                             .collect(Collectors.toSet());
                                           nodeToReplacedNodes.put(node.getIdentity(), replacedNodes);
                                           return replacedNodes;
                                       })
                                       .stream();
                    };
                }
            }

            @Override
            public GraphTransformer addNodes(Iterable<NodeIdentity> nodes)
            {
                Optional.ofNullable(nodes)
                        .orElse(Collections.emptyList())
                        .forEach(this::addNode);
                return this;
            }

            @Override
            public GraphTransformer addNode(NodeIdentity node)
            {
                if (node != null)
                {
                    this.additionalNodeIdentities.add(node);
                }
                return this;
            }

            @Override
            public GraphTransformer addEdges(Iterable<EdgeIdentity> edges)
            {
                Optional.ofNullable(edges)
                        .orElse(Collections.emptyList())
                        .forEach(this::addEdge);
                return this;
            }

            @Override
            public GraphTransformer addEdge(EdgeIdentity edge)
            {
                if (edge != null)
                {
                    this.additionalEdgeIdentities.add(edge);
                }
                return this;
            }

            @Override
            public GraphTransformer addEdge(NodeIdentity from, NodeIdentity to)
            {
                return this.addEdge(EdgeIdentity.of(from, to));
            }

            @Override
            public GraphTransformer addNodes(NodeIdentity... nodes)
            {
                return this.addNodes(Optional.ofNullable(nodes)
                                             .map(Arrays::asList)
                                             .orElse(Collections.emptyList()));
            }

            @Override
            public GraphTransformer map(Function<Node, NodeIdentity> mapper)
            {
                if (mapper != null)
                {
                    this.nodeMappers.add(mapper);
                }
                return this;
            }

        };
    }

    @Override
    public boolean isNotEmpty()
    {
        return !this.isEmpty();
    }

    @Override
    public boolean isEmpty()
    {
        return this.size() == 0;
    }

}
