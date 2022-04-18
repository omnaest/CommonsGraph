package org.omnaest.utils.graph.internal.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.CollectorUtils;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.Graph.KeyMapper;
import org.omnaest.utils.graph.domain.Graph.NodeInclusionFilter;
import org.omnaest.utils.graph.domain.Graph.NodeToKeysMapper;
import org.omnaest.utils.graph.domain.Graph.TypedGraphIndex;
import org.omnaest.utils.graph.domain.Graph.TypedGraphIndexBuilder;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

public class TypedGraphIndexBuilderImpl<K> implements TypedGraphIndexBuilder<K>
{
    private final Graph                            graph;
    private final NodeInclusionFilter              nodeInclusionFilter;
    private final List<NodeToKeysMapperContext<K>> nodeToKeysMappers;

    public TypedGraphIndexBuilderImpl(Graph graph, NodeInclusionFilter nodeInclusionFilter)
    {
        this.graph = graph;
        this.nodeInclusionFilter = nodeInclusionFilter;
        this.nodeToKeysMappers = new ArrayList<>();
    }

    @Override
    public TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper)
    {
        return this.withNodeToKeyMapper(nodeToKeysMapper, null);
    }

    @Override
    public TypedGraphIndex<K> get()
    {
        Graph graph = this.graph;
        Map<K, Set<NodeIdentity>> keyToNodes = MapUtils.toNewConcurrentHashMap(this.graph.stream()
                                                                                         .filter(this.nodeInclusionFilter)
                                                                                         .flatMap(node -> this.nodeToKeysMappers.stream()
                                                                                                                                .flatMap(mapper -> mapper.getNodeToKeysMapper()
                                                                                                                                                         .apply(node)
                                                                                                                                                         .flatMap(key -> mapper.getKeyMapper()
                                                                                                                                                                               .apply(key)))
                                                                                                                                .map(key -> BiElement.of(key,
                                                                                                                                                         node.getIdentity())))
                                                                                         .collect(CollectorUtils.toGroupedMapByBiElement(Collectors.toSet())));
        List<KeyMapper<K>> keyMappers = this.nodeToKeysMappers.stream()
                                                              .map(TypedGraphIndexBuilderImpl.NodeToKeysMapperContext<K>::getKeyMapper)
                                                              .collect(Collectors.toList());
        return new TypedGraphIndex<K>()
        {
            @Override
            public Stream<Node> apply(K key)
            {
                return Optional.ofNullable(key)
                               .map(inputKey -> keyMappers.stream()
                                                          .flatMap(mapper -> mapper.apply(inputKey))
                                                          .flatMap(singleKey -> Optional.ofNullable(keyToNodes.get(singleKey))
                                                                                        .map(Set::stream)
                                                                                        .orElse(Stream.empty())
                                                                                        .map(graph::findNodeById)
                                                                                        .filter(PredicateUtils.filterNonEmptyOptional())
                                                                                        .map(MapperUtils.mapOptionalToValue())))
                               .orElse(Stream.empty());
            }
        };
    }

    @Override
    public TypedGraphIndexBuilder<K> withNodeToKeyMapper(NodeToKeysMapper<K> nodeToKeysMapper, KeyMapper<K> keyMapper)
    {
        this.nodeToKeysMappers.add(new TypedGraphIndexBuilderImpl.NodeToKeysMapperContext<>(nodeToKeysMapper, keyMapper));
        return this;
    }

    private static class NodeToKeysMapperContext<K>
    {
        private NodeToKeysMapper<K> nodeToKeysMapper;
        private KeyMapper<K>        keyMapper;

        public NodeToKeysMapperContext(NodeToKeysMapper<K> nodeToKeysMapper, KeyMapper<K> keyMapper)
        {
            super();
            this.nodeToKeysMapper = nodeToKeysMapper;
            this.keyMapper = keyMapper;
        }

        public NodeToKeysMapper<K> getNodeToKeysMapper()
        {
            return this.nodeToKeysMapper;
        }

        public KeyMapper<K> getKeyMapper()
        {
            return Optional.ofNullable(this.keyMapper)
                           .orElse(key -> Stream.of(key));
        }

    }
}