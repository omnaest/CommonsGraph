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
package org.omnaest.utils.graph.internal.router.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.AssertionUtils;
import org.omnaest.utils.CollectionUtils;
import org.omnaest.utils.ComparatorUtils;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.IteratorUtils;
import org.omnaest.utils.IteratorUtils.RoundRobinListIterator;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.OptionalUtils;
import org.omnaest.utils.PeekUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.StreamUtils.SplittedStream;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.element.bi.UnaryBiElement;
import org.omnaest.utils.element.cached.CachedElement;
import org.omnaest.utils.functional.PredicateConsumer;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Edges;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.DataBuilder;
import org.omnaest.utils.graph.domain.GraphRouter.Direction;
import org.omnaest.utils.graph.domain.GraphRouter.ForwardNodeFunction;
import org.omnaest.utils.graph.domain.GraphRouter.HierarchicalNode;
import org.omnaest.utils.graph.domain.GraphRouter.Hierarchy;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.GraphRouter.RoutingStrategy;
import org.omnaest.utils.graph.domain.GraphRouter.SimpleRoute;
import org.omnaest.utils.graph.domain.GraphRouter.Traversal;
import org.omnaest.utils.graph.domain.GraphRouter.Traversal.NodeWeightDeterminationFunction;
import org.omnaest.utils.graph.domain.GraphRouter.Traversal.TraversalStepContext;
import org.omnaest.utils.graph.domain.GraphRouter.Traversal.TraversalStepFilter;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutes;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutesConsumer;
import org.omnaest.utils.graph.domain.GraphRouter.TraversedEdge;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.domain.Tag;
import org.omnaest.utils.graph.internal.router.route.RouteImpl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BreadthFirstRoutingStrategy implements RoutingStrategy
{
    private Graph            graph;
    private boolean          enableNodeResolving = true;
    private List<EdgeFilter> edgeFilters         = new ArrayList<>();
    private BudgetManager    budgetManager       = new NoBudgetManager();

    @Override
    public BreadthFirstRoutingStrategy withEdgeFilter(EdgeFilter edgeFilter)
    {
        if (edgeFilter != null)
        {
            this.edgeFilters.add(edgeFilter);
        }
        return this;
    }

    @Override
    public RoutingStrategy withExcludingEdgeByTagFilter(Tag... tag)
    {
        return this.withEdgeFilter(edge -> !edge.hasAnyTag(tag));
    }

    public BreadthFirstRoutingStrategy(Graph graph)
    {
        this.graph = graph;
    }

    @Override
    public RoutingStrategy withDisabledNodeResolving()
    {
        return this.withDisabledNodeResolving(true);
    }

    @Override
    public RoutingStrategy withDisabledNodeResolving(boolean disabledNodeResolving)
    {
        this.enableNodeResolving = !disabledNodeResolving;
        return this;
    }

    @Override
    public RoutingStrategy budgetOptimized()
    {
        this.budgetManager = new OptimziedBudgetManager();
        return this;
    }

    private static interface BudgetManager extends UnaryOperator<List<NodeAndContext>>
    {

        public double calculateBudgetScore(NodeAndContext parentNodeAndContext, TraversedEdges siblingEdges);

    }

    private static class NoBudgetManager implements BudgetManager
    {

        @Override
        public List<NodeAndContext> apply(List<NodeAndContext> nodeAndPaths)
        {
            return nodeAndPaths;
        }

        @Override
        public double calculateBudgetScore(NodeAndContext parentNodeAndContext, TraversedEdges siblingEdges)
        {
            return 0.0;
        }

    }

    private static class OptimziedBudgetManager implements BudgetManager
    {
        private List<NodeAndContext> stack = new ArrayList<>();

        @Override
        public List<NodeAndContext> apply(List<NodeAndContext> nodeAndContext)
        {
            List<NodeAndContext> sortedNodeAndContext = Stream.concat(this.stack.stream(), nodeAndContext.stream())
                                                              .sorted(ComparatorUtils.builder()
                                                                                     .of(NodeAndContext.class)
                                                                                     .with(NodeAndContext::getBudgetScore)
                                                                                     .build())
                                                              .collect(Collectors.toList());

            double average = sortedNodeAndContext.stream()
                                                 .mapToDouble(NodeAndContext::getBudgetScore)
                                                 .average()
                                                 .orElse(0.0);

            SplittedStream<NodeAndContext> splittedStream = StreamUtils.splitByFilter(sortedNodeAndContext.stream(),
                                                                                      iNodeAndContext -> iNodeAndContext.getBudgetScore() >= average);

            this.stack = splittedStream.excluded()
                                       .collect(Collectors.toList());
            List<NodeAndContext> result = splittedStream.included()
                                                        .collect(Collectors.toList());
            return result;
        }

        @Override
        public double calculateBudgetScore(NodeAndContext parentNodeAndContext, TraversedEdges siblingEdges)
        {
            return parentNodeAndContext.getBudgetScore() / Math.max(1.0, siblingEdges.size());
        }
    }

    @Override
    public Routes findAllIncomingRoutesBetween(NodeIdentity from, NodeIdentity to)
    {
        return this.findAllRoutesBetween(from, to, ForwardFunctions.INCOMING);
    }

    @Override
    public Routes findAllOutgoingRoutesBetween(NodeIdentity from, NodeIdentity to)
    {
        return this.findAllRoutesBetween(from, to, ForwardFunctions.OUTGOING);
    }

    private Routes findAllRoutesBetween(NodeIdentity from, NodeIdentity to, ForwardFunctionsProvider forwardFunctions)
    {
        Optional<Node> startNode = this.graph.findNodeById(from);
        List<Route> routes = new ArrayList<>();
        if (startNode.isPresent())
        {
            List<NodeAndContext> currentNodeAndPaths = new ArrayList<>();
            currentNodeAndPaths.add(NodeAndContext.of(startNode.get()));

            if (this.graph.findNodeById(to)
                          .map(node -> node.equals(startNode.get()))
                          .orElse(false))
            {
                routes.add(new RouteImpl(Arrays.asList(startNode.get()
                                                                .getIdentity()),
                                         Collections.emptyList(), this.graph));
            }
            VisitedNodesHandler visitedNodesHandler = new VisitedNodesFilterStrategy().newVisitedNodesContext()
                                                                                      .newVisitedNodesHandler(new TraversalBag(new ForwardFunctionsContext(forwardFunctions,
                                                                                                                                                           null),
                                                                                                                               0).getExpandedNodesFilter());
            while (!currentNodeAndPaths.isEmpty())
            {
                Set<NodeIdentity> currentNodes = this.determineNodesFrom(currentNodeAndPaths);
                this.resolveUnresolvedNodesIfEnabled(currentNodes);
                currentNodeAndPaths = this.determineNextNodes(forwardFunctions, currentNodeAndPaths, visitedNodesHandler, SkipNodes.empty(),
                                                              ConsumerUtils.noOperation());
                routes.addAll(this.determineRoutesByMatchingNodes(this.graph.findNodeById(to), currentNodeAndPaths));
            }
        }

        return new RoutesImpl(routes);
    }

    @Override
    public Traversal traverseOutgoing()
    {
        return this.traverseOutgoing(this.determineStartNodes(node -> node.getIncomingNodes()));
    }

    @Override
    public Traversal traverseOutgoing(Set<NodeIdentity> startNodes)
    {
        return this.traverse(startNodes, ForwardFunctions.OUTGOING);

    }

    @Override
    public Traversal traverseOutgoing(NodeIdentity... startNodes)
    {
        return this.traverseOutgoing(SetUtils.toSet(startNodes));
    }

    @Override
    public Traversal traverseIncoming()
    {
        return this.traverseIncoming(this.determineStartNodes(node -> node.getOutgoingNodes()));
    }

    @Override
    public Traversal traverseIncoming(NodeIdentity... startNodes)
    {
        return this.traverseIncoming(SetUtils.toSet(startNodes));
    }

    @Override
    public Traversal traverseIncoming(Set<NodeIdentity> startNodes)
    {
        return this.traverse(startNodes, ForwardFunctions.INCOMING);
    }

    @Override
    public Traversal traverse(Direction direction, NodeIdentity... startNodes)
    {
        return this.traverse(direction, SetUtils.toSet(startNodes));
    }

    @Override
    public Traversal traverse(Direction direction, Set<NodeIdentity> startNodes)
    {
        AssertionUtils.assertIsNotNull("Direction must not be null", direction);
        if (Direction.OUTGOING.equals(direction))
        {
            return this.traverseOutgoing(startNodes);
        }
        else
        {
            return this.traverseIncoming(startNodes);
        }
    }

    private Traversal traverse(Set<NodeIdentity> startNodes, ForwardFunctionsProvider forwardFunction)
    {
        Graph graph = this.graph;
        return new TraversalImpl(forwardFunction, graph, startNodes);

    }

    private static interface ForwardFunctionsProvider
    {
        public ForwardNodeFunction forwardNodeFunction();

        public ForwardEdgeFinderFunction forwardEdgeFinderFunction();

        public ForwardEdgeFunction forwardEdgeFunction();

        public Direction direction();
    }

    private static interface ForwardEdgeFinderFunction extends BiFunction<Node, Node, Optional<Edge>>
    {
    }

    private static interface ForwardEdgeFunction extends Function<Node, TraversedEdges>
    {
    }

    private static class TraversedEdges
    {
        private Edges                      edges;
        private EdgeToNextNodeFunction     edgeToNextNodeFunction;
        private EdgeToPreviousNodeFunction edgeToPreviousNodeFunction;

        private TraversedEdges(Edges edges, EdgeToNextNodeFunction edgeToNextNodeFunction, EdgeToPreviousNodeFunction edgeToPreviousNodeFunction)
        {
            super();
            this.edges = edges;
            this.edgeToNextNodeFunction = edgeToNextNodeFunction;
            this.edgeToPreviousNodeFunction = edgeToPreviousNodeFunction;
        }

        public static TraversedEdges of(Edges edges, EdgeToNextNodeFunction edgeToNextNodeFunction, EdgeToPreviousNodeFunction edgeToPreviousNodeFunction)
        {
            return new TraversedEdges(edges, edgeToNextNodeFunction, edgeToPreviousNodeFunction);
        }

        public Stream<TraversedEdge> stream()
        {
            return this.edges.stream()
                             .map(edge -> new TraversedEdgeImpl(edge, this.edgeToNextNodeFunction.apply(edge), this.edgeToPreviousNodeFunction.apply(edge)));
        }

        public int size()
        {
            return this.edges.size();
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("TraversedEdges [edges=")
                   .append(this.edges)
                   .append(", edgeToNextNodeFunction=")
                   .append(this.edgeToNextNodeFunction)
                   .append(", edgeToPreviousNodeFunction=")
                   .append(this.edgeToPreviousNodeFunction)
                   .append("]");
            return builder.toString();
        }

    }

    private static class TraversedEdgeImpl implements TraversedEdge
    {
        private Edge edge;
        private Node nextNode;
        private Node previousNode;

        public TraversedEdgeImpl(Edge edge, Node nextNode, Node previousNode)
        {
            super();
            this.edge = edge;
            this.nextNode = nextNode;
            this.previousNode = previousNode;
        }

        @Override
        public Edge getEdge()
        {
            return this.edge;
        }

        @Override
        public Node getPreviousNode()
        {
            return this.previousNode;
        }

        @Override
        public Node getNextNode()
        {
            return this.nextNode;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("TraversedEdge [edge=")
                   .append(this.edge)
                   .append(", nextNode=")
                   .append(this.nextNode)
                   .append("]");
            return builder.toString();
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.edge == null) ? 0 : this.edge.hashCode());
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
            if (!(obj instanceof TraversedEdgeImpl))
            {
                return false;
            }
            TraversedEdgeImpl other = (TraversedEdgeImpl) obj;
            if (this.edge == null)
            {
                if (other.edge != null)
                {
                    return false;
                }
            }
            else if (!this.edge.equals(other.edge))
            {
                return false;
            }
            return true;
        }

    }

    private static interface EdgeToNextNodeFunction extends EdgeToOneNodeFunction
    {

    }

    private static interface EdgeToPreviousNodeFunction extends EdgeToOneNodeFunction
    {

    }

    private static interface EdgeToOneNodeFunction extends Function<Edge, Node>
    {

    }

    private static enum ForwardFunctions implements ForwardFunctionsProvider
    {
        OUTGOING(node -> node.getOutgoingNodes(), node -> TraversedEdges.of(node.getOutgoingEdges(), Edge::getTo, Edge::getFrom),
                (from, to) -> from.findOutgoingEdgeTo(to.getIdentity()), Direction.OUTGOING),
        INCOMING(node -> node.getIncomingNodes(), node -> TraversedEdges.of(node.getIncomingEdges(), Edge::getFrom, Edge::getTo),
                (from, to) -> from.findIncomingEdgeFrom(to.getIdentity()), Direction.INCOMING);

        private ForwardNodeFunction       forwardNodeFunction;
        private ForwardEdgeFunction       forwardEdgeFunction;
        private ForwardEdgeFinderFunction forwardEdgeFinderFunction;
        private Direction                 direction;

        private ForwardFunctions(ForwardNodeFunction forwardNodeFunction, ForwardEdgeFunction forwardEdgeFunction,
                                 ForwardEdgeFinderFunction forwardEdgeFinderFunction, Direction direction)
        {
            this.forwardNodeFunction = forwardNodeFunction;
            this.forwardEdgeFunction = forwardEdgeFunction;
            this.forwardEdgeFinderFunction = forwardEdgeFinderFunction;
            this.direction = direction;
        }

        @Override
        public ForwardNodeFunction forwardNodeFunction()
        {
            return this.forwardNodeFunction;
        }

        @Override
        public ForwardEdgeFinderFunction forwardEdgeFinderFunction()
        {
            return this.forwardEdgeFinderFunction;
        }

        @Override
        public ForwardEdgeFunction forwardEdgeFunction()
        {
            return this.forwardEdgeFunction;
        }

        @Override
        public Direction direction()
        {
            return this.direction;
        }

        public static ForwardFunctions byDirection(Direction direction)
        {
            if (Direction.INCOMING.equals(direction))
            {
                return ForwardFunctions.INCOMING;
            }
            else
            {
                return ForwardFunctions.OUTGOING;
            }
        }

    }

    private static class ForwardFunctionsContext
    {
        private final ForwardFunctionsProvider forwardFunctionsProvider;
        private final TraversalStepFilter      traversalStepFilter;

        public ForwardFunctionsContext(ForwardFunctionsProvider forwardFunctionsProvider, TraversalStepFilter traversalStepFilter)
        {
            super();
            this.forwardFunctionsProvider = forwardFunctionsProvider;
            this.traversalStepFilter = traversalStepFilter;
        }

        public ForwardFunctionsProvider getForwardFunctionsProvider()
        {
            return this.forwardFunctionsProvider;
        }

        public Optional<TraversalStepFilter> getTraversalStepFilter()
        {
            return Optional.ofNullable(this.traversalStepFilter);
        }

    }

    public static interface ColumnizedHierarchyNode extends Supplier<HierarchicalNode>
    {
        /**
         * Returns the column index = 0,1,2,...
         * 
         * @return
         */
        public int getColumnIndex();

        public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer);
    }

    private class TraversalImpl implements Traversal
    {
        private final List<ForwardFunctionsContext> forwardFunctionContexts = new ArrayList<>();
        private final Graph                         graph;
        private final Set<NodeIdentity>             startNodes;

        private final List<TraversalRoutesConsumer>    alreadyVisitedNodesHitHandlers = new ArrayList<>();
        private final VisitedNodesFilterStrategy       visitedNodesFilterStrategy     = new VisitedNodesFilterStrategy();
        private final List<WeightedTerminationHandler> weightedTerminationHandlers    = new ArrayList<>();

        private TraversalImpl(ForwardFunctionsProvider forwardFunction, Graph graph, Set<NodeIdentity> startNodes)
        {
            this.graph = graph;
            this.startNodes = startNodes;
            this.forwardFunctionContexts.add(new ForwardFunctionsContext(forwardFunction, null));
        }

        @Override
        public Stream<TraversalRoutes> stream()
        {
            return StreamUtils.fromIterator(new BreadthFirstIterator(this.startNodes, this.forwardFunctionContexts, this.graph,
                                                                     this.alreadyVisitedNodesHitHandlers, this.visitedNodesFilterStrategy))
                              .map(TraversalRoutesImpl::new)
                              .map(MapperUtils.identityCast(TraversalRoutes.class))
                              .peek(PeekUtils.all(this.weightedTerminationHandlers));
        }

        @Override
        public Traversal withAlreadyVisitedNodesHitHandler(TraversalRoutesConsumer routesConsumer)
        {
            this.alreadyVisitedNodesHitHandlers.add(routesConsumer);
            return this;
        }

        @Override
        public Traversal includingFirstRouteOfAlreadyVisitedNodes()
        {
            this.visitedNodesFilterStrategy.setIncludeFirstVisitedNodeHitRoutes(true);
            return this;
        }

        @Override
        public Traversal withWeightedPathTermination(double terminationWeightBarrier, NodeWeightDeterminationFunction nodeWeightDeterminationFunction)
        {
            this.weightedTerminationHandlers.add(new WeightedTerminationHandler(terminationWeightBarrier, nodeWeightDeterminationFunction));
            return this;
        }

        @Override
        public Traversal withWeightedPathTerminationByBranches(double terminationWeightBarrier,
                                                               IsolatedNodeWeightDeterminationFunction nodeWeightDeterminationFunction)
        {
            return this.withWeightedPathTerminationByBranchesAndRoute(terminationWeightBarrier, route -> route.last()
                                                                                                              .map(nodeWeightDeterminationFunction::applyAsDouble)
                                                                                                              .orElse(1.0));
        }

        @Override
        public Traversal withWeightedPathTerminationByBranchesAndRoute(double terminationWeightBarrier,
                                                                       IsolatedNodeWeightByRouteDeterminationFunction nodeWeightByRouteDeterminationFunction)
        {
            return this.withWeightedPathTermination(terminationWeightBarrier, (node, route, parentWeight, forwardNodeFunction) -> parentWeight.orElse(1.0)
                    * nodeWeightByRouteDeterminationFunction.applyAsDouble(route) / Math.max(1.0, route.lastNth(1)
                                                                                                       .map(forwardNodeFunction)
                                                                                                       .map(Nodes::stream)
                                                                                                       .orElse(Stream.empty())
                                                                                                       .map(Node::getIdentity)
                                                                                                       .map(nodeIdentity -> route.getSubRouteUntilLastNth(1)
                                                                                                                                 .addToNew(nodeIdentity))
                                                                                                       .mapToDouble(nodeWeightByRouteDeterminationFunction)
                                                                                                       .sum()));
        }

        @Override
        public Traversal withWeightedPathTerminationByBranches(double terminationWeightBarrier)
        {
            return this.withWeightedPathTerminationByBranches(terminationWeightBarrier, node -> 1.0);
        }

        @Override
        public Stream<Route> routes()
        {
            return this.routesAndTraversalControls()
                       .map(RouteAndTraversalControl::get);
        }

        @Override
        public Stream<RouteAndTraversalControl> routesAndTraversalControls()
        {
            return this.stream()
                       .flatMap(TraversalRoutes::stream);
        }

        @Override
        public Stream<Node> nodes()
        {
            return this.routes()
                       .map(Route::last)
                       .filter(PredicateUtils.filterNonEmptyOptional())
                       .map(MapperUtils.mapOptionalToValue());
        }

        @Override
        public Hierarchy asHierarchy()
        {
            Set<Node> rootNodes = this.stream()
                                      .limit(1)
                                      .flatMap(TraversalRoutes::stream)
                                      .map(RouteAndTraversalControl::get)
                                      .flatMap(Route::stream)
                                      .collect(Collectors.toSet());
            Map<Node, Set<TraversedEdge>> parentNodeToChildNodes = this.includingFirstRouteOfAlreadyVisitedNodes()
                                                                       .routes()
                                                                       .filter(Route::isNotCyclic)
                                                                       .map(Route::lastEdge)
                                                                       .filter(PredicateUtils.filterNonEmptyOptional())
                                                                       .map(MapperUtils.mapOptionalToValue())
                                                                       //                                                                       .map(Route::edges)
                                                                       //                                                                       .flatMap(GraphRouter.TraversedEdges::stream)
                                                                       .collect(Collectors.groupingBy((TraversedEdge edge) -> edge.getPreviousNode(),
                                                                                                      Collectors.mapping((TraversedEdge edge) -> edge,
                                                                                                                         Collectors.toSet())));

            return new Hierarchy()
            {
                @Override
                public Stream<HierarchicalNode> stream()
                {
                    return rootNodes.stream()
                                    .map(this.createNodeToHierarchicalNodeMapper());
                }

                private Function<Node, HierarchicalNode> createNodeToHierarchicalNodeMapper()
                {
                    return node ->
                    {
                        Set<NodeIdentity> parents = SetUtils.toSet(node.getIdentity());
                        return new HierarchicalNode()
                        {
                            @Override
                            public Node get()
                            {
                                return node;
                            }

                            @Override
                            public Stream<HierarchicalNode> getChildren()
                            {
                                return parentNodeToChildNodes.getOrDefault(node, Collections.emptySet())
                                                             .stream()
                                                             .map(createEdgeToHierarchicalNodeMapper(parents));
                            }

                            @Override
                            public Optional<Edge> getEdge()
                            {
                                return Optional.empty();
                            }
                        };
                    };
                }

                private Function<TraversedEdge, HierarchicalNode> createEdgeToHierarchicalNodeMapper(Set<NodeIdentity> parents)
                {
                    return edge ->
                    {
                        Node node = edge.getNextNode();
                        return new HierarchicalNode()
                        {
                            @Override
                            public Node get()
                            {
                                return node;
                            }

                            @Override
                            public Optional<Edge> getEdge()
                            {
                                return Optional.of(edge.getEdge());
                            }

                            @Override
                            public Stream<HierarchicalNode> getChildren()
                            {
                                return parentNodeToChildNodes.getOrDefault(node, Collections.emptySet())
                                                             .stream()
                                                             .filter(childNode -> !parents.contains(childNode.getNextNode()
                                                                                                             .getIdentity()))
                                                             .map(createEdgeToHierarchicalNodeMapper(SetUtils.addToNew(parents, node.getIdentity())));
                            }
                        };
                    };
                }

                @Override
                public String asJson()
                {
                    return this.asJsonWithData(ConsumerUtils.noOperation());
                }

                @Override
                public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer)
                {
                    return JSONHelper.serializer(List.class)
                                     .withPrettyPrint()
                                     .apply(this.stream()
                                                .map(this.createHierarchyNodeToJsonSerializableNodeMapper(nodeAndDataBuilderConsumer))
                                                .collect(Collectors.toList()));
                }

                @Override
                public Stream<ColumnizedHierarchyNode> asColumnizedNodes()
                {
                    return this.stream()
                               .flatMap(this.createHierarchyNodeFlattener(0))
                               .map(this.createColumnizedHierarchyNode());
                }

                private Function<BiElement<Integer, HierarchicalNode>, ColumnizedHierarchyNode> createColumnizedHierarchyNode()
                {
                    return indexAndNodeAndParent ->
                    {
                        return new ColumnizedHierarchyNode()
                        {
                            @Override
                            public int getColumnIndex()
                            {
                                return indexAndNodeAndParent.getFirst();
                            }

                            @Override
                            public String asJsonWithData(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer)
                            {
                                HierarchicalNode node = indexAndNodeAndParent.getSecond();

                                Map<String, Object> data = new HashMap<>();
                                nodeAndDataBuilderConsumer.accept(node, createDataBuilder(data));

                                return JSONHelper.serializer(JsonSerializableHierarchyNode.class)
                                                 .withPrettyPrint(false)
                                                 .apply(new JsonSerializableHierarchyNode(node.get()
                                                                                              .getIdentity(),
                                                                                          null, data));
                            }

                            @Override
                            public HierarchicalNode get()
                            {
                                return indexAndNodeAndParent.getSecond();
                            }
                        };
                    };
                }

                private Function<HierarchicalNode, Stream<BiElement<Integer, HierarchicalNode>>> createHierarchyNodeFlattener(int depth)
                {
                    return node ->
                    {
                        return Stream.concat(Stream.of(BiElement.of(depth, node)), node.getChildren()
                                                                                       .flatMap(this.createHierarchyNodeFlattener(depth + 1)));
                    };
                }

                private Function<HierarchicalNode, JsonSerializableHierarchyNode> createHierarchyNodeToJsonSerializableNodeMapper(BiConsumer<HierarchicalNode, DataBuilder> nodeAndDataBuilderConsumer)
                {
                    return node ->
                    {
                        Map<String, Object> data = new HashMap<>();
                        nodeAndDataBuilderConsumer.accept(node, this.createDataBuilder(data));
                        return new JsonSerializableHierarchyNode(node.get()
                                                                     .getIdentity(),
                                                                 node.getChildren()
                                                                     .map(this.createHierarchyNodeToJsonSerializableNodeMapper(nodeAndDataBuilderConsumer))
                                                                     .collect(Collectors.toList()),
                                                                 data);
                    };
                }

                private DataBuilder createDataBuilder(Map<String, Object> data)
                {
                    return new DataBuilder()
                    {
                        @Override
                        public DataBuilder put(String key, Object value)
                        {
                            data.put(key, value);
                            return this;
                        }
                    };
                }
            };
        }

        @Override
        public Traversal andTraverseIncoming()
        {
            return this.andTraverse(Direction.INCOMING);
        }

        @Override
        public Traversal andTraverseOutgoing()
        {
            return this.andTraverse(Direction.OUTGOING);
        }

        @Override
        public Traversal andTraverse(TraversalStepFilter filter, Direction... directions)
        {
            Optional.ofNullable(directions)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList())
                    .forEach(direction -> this.forwardFunctionContexts.add(new ForwardFunctionsContext(ForwardFunctions.byDirection(direction), filter)));
            return this;
        }

        @Override
        public Traversal andTraverse(Direction... directions)
        {
            return this.andTraverse(null, directions);
        }
    }

    @SuppressWarnings("unused")
    private static class JsonSerializableHierarchyNode
    {
        @JsonProperty
        private NodeIdentity nodeIdentity;

        @JsonProperty
        @JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
        private Map<String, Object> data;

        @JsonProperty
        @JsonInclude(Include.NON_NULL)
        private List<JsonSerializableHierarchyNode> children;

        public JsonSerializableHierarchyNode(NodeIdentity nodeIdentity, List<JsonSerializableHierarchyNode> children, Map<String, Object> data)
        {
            super();
            this.nodeIdentity = nodeIdentity;
            this.children = children;
            this.data = data;
        }

        public NodeIdentity getNodeIdentity()
        {
            return this.nodeIdentity;
        }

        public List<JsonSerializableHierarchyNode> getChildren()
        {
            return this.children;
        }

        public Map<String, Object> getData()
        {
            return this.data;
        }

    }

    private static class WeightedTerminationHandler implements Consumer<TraversalRoutes>
    {
        private final double                          terminationWeightBarrier;
        private final NodeWeightDeterminationFunction nodeWeightDeterminationFunction;
        private Map<NodeIdentity, Double>             nodeIdentityToWeight = new ConcurrentHashMap<>();

        public WeightedTerminationHandler(double terminationWeightBarrier, NodeWeightDeterminationFunction nodeWeightDeterminationFunction)
        {
            this.terminationWeightBarrier = terminationWeightBarrier;
            this.nodeWeightDeterminationFunction = nodeWeightDeterminationFunction;
        }

        @Override
        public void accept(TraversalRoutes routes)
        {
            routes.stream()
                  .forEach(routeAndTraversalControl ->
                  {
                      Route route = routeAndTraversalControl.get();
                      route.last()
                           .ifPresent(node ->
                           {
                               OptionalDouble parentWeight = route.lastNth(1)
                                                                  .map(Node::getIdentity)
                                                                  .map(this.nodeIdentityToWeight::get)
                                                                  .map(OptionalDouble::of)
                                                                  .orElse(OptionalDouble.empty());
                               NodeIdentity nodeIdentity = node.getIdentity();
                               ForwardNodeFunction forwardNodeFunction = routeAndTraversalControl.getTraversalStepContext()
                                                                                                 .getForwardNodeFunction();
                               double nodeWeight = this.nodeIdentityToWeight.computeIfAbsent(nodeIdentity,
                                                                                             identity -> this.nodeWeightDeterminationFunction.apply(node, route,
                                                                                                                                                    parentWeight,
                                                                                                                                                    forwardNodeFunction));
                               if (nodeWeight < this.terminationWeightBarrier)
                               {
                                   routeAndTraversalControl.skip();
                               }
                           });
                  });
        }

    }

    private Set<NodeIdentity> determineStartNodes(Function<Node, Nodes> forwardFunction)
    {
        return this.graph.stream()
                         .filter(node -> forwardFunction.apply(node)
                                                        .hasNone())
                         .map(Node::getIdentity)
                         .collect(Collectors.toSet());
    }

    private void resolveUnresolvedNodesIfEnabled(Set<NodeIdentity> currentNodes)
    {
        if (this.enableNodeResolving)
        {
            this.graph.findNodesByIds(currentNodes)
                      .resolveAll();
        }
    }

    private Set<NodeIdentity> determineNodesFrom(List<NodeAndContext> currentNodes)
    {
        return currentNodes.stream()
                           .map(NodeAndContext::getNode)
                           .map(Node::getIdentity)
                           .collect(Collectors.toSet());
    }

    private List<Route> determineRoutesByMatchingNodes(Optional<Node> targetNode, List<NodeAndContext> currentNodes)
    {
        if (targetNode.isPresent())
        {
            return this.wrapMatchingNodeAndPathsIntoRoutes(this.determineMatchingNodes(targetNode, currentNodes));
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private List<Route> wrapMatchingNodeAndPathsIntoRoutes(Collection<NodeAndContext> matchingNodes)
    {
        return matchingNodes.stream()
                            .map(nodeAndPath -> new RouteImpl(nodeAndPath.getFullPath()
                                                                         .stream()
                                                                         .map(Node::getIdentity)
                                                                         .collect(Collectors.toList()),
                                                              nodeAndPath.getEdges(), this.graph))
                            .collect(Collectors.toList());
    }

    private List<NodeAndContext> determineMatchingNodes(Optional<Node> targetNode, List<NodeAndContext> currentNodes)
    {
        return currentNodes.stream()
                           .filter(nodeAndPath -> nodeAndPath.getNode()
                                                             .equals(targetNode.get()))
                           .collect(Collectors.toList());
    }

    private List<NodeAndContext> determineNextNodes(ForwardFunctionsProvider forwardFunctions, List<NodeAndContext> currentNodeAndPaths,
                                                    VisitedNodesHandler visitedNodesHandler, SkipNodes skipNodes,
                                                    Consumer<Set<NodeAndContext>> visitedNodesHitConsumer)
    {
        Set<NodeAndContext> visitedNodesHitPaths = new HashSet<>();
        List<NodeAndContext> result = currentNodeAndPaths.stream()
                                                         .filter(skipNodes::matchesNot)
                                                         .flatMap(this.explodeCurrentNodeIntoNextNodes(forwardFunctions, visitedNodesHandler,
                                                                                                       visitedNodesHitPaths))
                                                         .filter(skipNodes::matchesNot)
                                                         .filter(this.createEdgesFilter(forwardFunctions.forwardEdgeFinderFunction()))
                                                         .collect(Collectors.toList());

        if (!visitedNodesHitPaths.isEmpty())
        {
            visitedNodesHitConsumer.accept(visitedNodesHitPaths);
        }

        return this.budgetManager.apply(result);
    }

    private Function<NodeAndContext, Stream<NodeAndContext>> explodeCurrentNodeIntoNextNodes(ForwardFunctionsProvider forwardFunctions,
                                                                                             VisitedNodesHandler visitedNodesHandler,
                                                                                             Set<NodeAndContext> visitedNodesHitPaths)
    {
        return parentNodeAndContext ->
        {
            return visitedNodesHandler.filterVisitedNodesUpfrontOrExecute(parentNodeAndContext, visitedNodesHitPaths, visitedNodesAfterExpansionFilter ->
            {
                TraversedEdges nextEdges = forwardFunctions.forwardEdgeFunction()
                                                           .apply(parentNodeAndContext.getNode());
                return nextEdges.stream()
                                .filter(visitedNodesAfterExpansionFilter)
                                .map(edge -> parentNodeAndContext.append(edge, this.budgetManager.calculateBudgetScore(parentNodeAndContext, nextEdges)));
            });
        };
    }

    private Predicate<NodeAndContext> createEdgesFilter(BiFunction<Node, Node, Optional<Edge>> edgesFunction)
    {
        List<EdgeFilter> edgeFilters = this.edgeFilters;
        if (edgeFilters.isEmpty())
        {
            return nodeAndPath -> true;
        }
        else
        {
            return nodeAndPath ->
            {
                Optional<Node> currentNode = ListUtils.optionalLast(nodeAndPath.getFullPath());
                Optional<Node> previousNode = ListUtils.optionalLast(nodeAndPath.getFullPath(), 1);

                return OptionalUtils.both(previousNode, currentNode)
                                    .flatMap(fromAndTo -> edgesFunction.apply(fromAndTo.getFirst(), fromAndTo.getSecond()))
                                    .map(edge -> PredicateUtils.all(edgeFilters)
                                                               .test(edge))
                                    .orElse(true);
            };
        }
    }

    private static class SkipNodes
    {
        private Set<NodeIdentity>   nodeIdentities = new HashSet<>();
        private Set<NodeAndContext> paths          = new HashSet<>();

        public boolean matchesNot(NodeAndContext nodeAndPath)
        {
            return !this.matches(nodeAndPath);
        }

        public boolean matches(NodeAndContext nodeAndPath)
        {
            boolean isMatchedByNodeIdentities = PredicateUtils.isCollectionContaining(this.nodeIdentities)
                                                              .from(Node::getIdentity)
                                                              .from(NodeAndContext::getNode)
                                                              .test(nodeAndPath);
            boolean isMatchedByPaths = PredicateUtils.isCollectionContaining(this.paths)
                                                     .test(nodeAndPath);
            return isMatchedByNodeIdentities || isMatchedByPaths;
        }

        public SkipNodes addAll(Collection<NodeIdentity> nodeIdentities)
        {
            this.nodeIdentities.addAll(nodeIdentities);
            return this;
        }

        public SkipNodes add(NodeAndContext nodeAndPath)
        {
            this.paths.add(nodeAndPath);
            return this;
        }

        public static SkipNodes empty()
        {
            return new SkipNodes();
        }
    }

    private static class TraversalRoutesImpl implements TraversalRoutes
    {
        private final RoutesControl routesControl;

        private TraversalRoutesImpl(RoutesControl routesControl)
        {
            this.routesControl = routesControl;
        }

        @Override
        public Stream<RouteAndTraversalControl> stream()
        {
            RoutesControl routesControl = this.routesControl;
            return this.routesControl.getRoutes()
                                     .stream()
                                     .map(route -> new RouteAndTraversalControlImpl(route, routesControl, this.routesControl.getTraversalStepContext()));
        }

        private static class RouteAndTraversalControlImpl implements RouteAndTraversalControl
        {
            private final Route                route;
            private final TraversalStepContext traversalStepContext;
            private final RoutesControl        routesControl;

            private RouteAndTraversalControlImpl(Route route, RoutesControl routesControl, TraversalStepContext traversalStepContext)
            {
                this.route = route;
                this.routesControl = routesControl;
                this.traversalStepContext = traversalStepContext;
            }

            @Override
            public Route get()
            {
                return this.route;
            }

            @Override
            public RouteAndTraversalControl skip()
            {
                this.routesControl.addRouteToSkipNodes(this.get());
                return this;
            }

            @Override
            public RouteAndTraversalControl skipEdgesWithTag(Tag tag)
            {
                this.skipNextRouteNodes(this.route.last()
                                                  .get()
                                                  .findAllEdgesWithTag(tag)
                                                  .stream()
                                                  .map(Edge::getNodeIdentities)
                                                  .map(UnaryBiElement::asList)
                                                  .flatMap(List::stream)
                                                  .collect(Collectors.toSet()));
                return this;
            }

            @Override
            public RouteAndTraversalControl skipIf(boolean condition)
            {
                if (condition)
                {
                    this.skip();
                }
                return this;
            }

            @Override
            public RouteAndTraversalControl skipNodes(NodeIdentity... nodeIdentities)
            {
                return this.skipNodes(Arrays.asList(nodeIdentities));
            }

            @Override
            public RouteAndTraversalControl skipNodes(Collection<NodeIdentity> nodeIdentities)
            {
                this.routesControl.addToSkipNodes(nodeIdentities);
                return this;
            }

            @Override
            public RouteAndTraversalControl skipNextRouteNodes(NodeIdentity... nodeIdentities)
            {
                return this.skipNextRouteNodes(Arrays.asList(nodeIdentities));
            }

            @Override
            public RouteAndTraversalControl skipNextRouteNodes(Collection<NodeIdentity> nodeIdentities)
            {
                Optional.ofNullable(nodeIdentities)
                        .orElse(Collections.emptyList())
                        .forEach(nodeIdentity -> this.routesControl.addRouteToSkipNodes(this.route.addToNew(nodeIdentity)));
                return this;
            }

            @Override
            public TraversalStepContext getTraversalStepContext()
            {
                return this.traversalStepContext;
            }
        }
    }

    private class BreadthFirstIterator implements Iterator<RoutesControl>
    {
        private final CachedElement<RoutesControl> currentRoutesControl;
        private final AtomicLong                   counter = new AtomicLong();
        private final TraversalBagManager          traversalBagManager;

        private BreadthFirstIterator(Set<NodeIdentity> startNodes, List<ForwardFunctionsContext> forwardFunctionContexts, Graph graph,
                                     List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers, VisitedNodesFilterStrategy visitedNodesFilterStrategy)
        {
            List<NodeAndContext> startNodeAndContexts = Optional.ofNullable(startNodes)
                                                                .orElse(Collections.emptySet())
                                                                .stream()
                                                                .map(graph::findNodeById)
                                                                .filter(Optional::isPresent)
                                                                .map(Optional::get)
                                                                .map(NodeAndContext::of)
                                                                .collect(Collectors.toList());

            this.traversalBagManager = new TraversalBagManager().apply(forwardFunctionContexts)
                                                                .initializePrimaryBagWithNodes(startNodeAndContexts);

            SkipNodes skipNodes = new SkipNodes();
            this.currentRoutesControl = CachedElement.of(new BreadthFirstSupplier(this.traversalBagManager, this.counter, skipNodes,
                                                                                  alreadyVisitedNodesHitHandlers, visitedNodesFilterStrategy))
                                                     .set(new RoutesControl(new RoutesImpl(BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(startNodeAndContexts)),
                                                                            skipNodes, this.counter.incrementAndGet(), () -> this.counter.get(),
                                                                            this.traversalBagManager.getPrimaryTraversalStepContext()));
        }

        @Override
        public boolean hasNext()
        {
            return this.currentRoutesControl.get()
                                            .getRoutes()
                                            .hasRoutes()
                    || this.traversalBagManager.hasUnprocessedNodes();
        }

        @Override
        public RoutesControl next()
        {
            return this.currentRoutesControl.getAndReset();
        }

        private final class BreadthFirstSupplier implements Supplier<RoutesControl>
        {
            private final VistedNodesContext            visitedNodesContext;
            private final SkipNodes                     skipNodes;
            private final AtomicLong                    counter;
            private final List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers;
            private final TraversalBagManager           traversalBagManager;

            private BreadthFirstSupplier(TraversalBagManager traversalBagManager, AtomicLong counter, SkipNodes skipNodes,
                                         List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers, VisitedNodesFilterStrategy visitedNodesFilterStrategy)
            {
                this.traversalBagManager = traversalBagManager;
                this.counter = counter;
                this.skipNodes = skipNodes;
                this.alreadyVisitedNodesHitHandlers = alreadyVisitedNodesHitHandlers;
                this.visitedNodesContext = visitedNodesFilterStrategy.newVisitedNodesContext();
            }

            @Override
            public RoutesControl get()
            {
                return this.traversalBagManager.processNextAndGet(traversalBag ->
                {
                    //
                    Set<NodeIdentity> currentNodes = BreadthFirstRoutingStrategy.this.determineNodesFrom(traversalBag.getCurrentNodeAndContexts());
                    BreadthFirstRoutingStrategy.this.resolveUnresolvedNodesIfEnabled(currentNodes);

                    //
                    traversalBag.setCurrentNodeAndContexts(BreadthFirstRoutingStrategy.this.determineNextNodes(traversalBag.getForwardFunctions(),
                                                                                                               traversalBag.getCurrentNodeAndContexts(),
                                                                                                               this.visitedNodesContext.newVisitedNodesHandler(traversalBag.getExpandedNodesFilter()),
                                                                                                               this.skipNodes,
                                                                                                               this.createVisitedNodesHitConsumer()));

                    //
                    List<Route> routes = BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(traversalBag.getCurrentNodeAndContexts());
                    return new RoutesControl(new RoutesImpl(routes), this.skipNodes, this.counter.incrementAndGet(), () -> this.counter.get(),
                                             traversalBag.getCurrentTraversalStepContext());
                });
            }

            private Consumer<Set<NodeAndContext>> createVisitedNodesHitConsumer()
            {
                return nodeAndPaths -> this.alreadyVisitedNodesHitHandlers.forEach(handler -> handler.accept(new RoutesImpl(BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(nodeAndPaths))));
            }
        }
    }

    private static class TraversalBagManager
    {
        private List<TraversalBag>                                  traversalBags = Collections.emptyList();
        private CachedElement<RoundRobinListIterator<TraversalBag>> iterator      = CachedElement.of(() -> IteratorUtils.roundRobinListIterator(this.traversalBags));

        public <R> R processNextAndGet(Function<TraversalBag, R> operation)
        {
            //
            TraversalBag traversalBag = this.iterator.get()
                                                     .next();

            //
            List<NodeAndContext> previousNodeAndContexts = ListUtils.addAllToNew(traversalBag.getCurrentNodeAndContexts());

            //
            R result = operation.apply(traversalBag);

            //
            List<NodeAndContext> currentNodeAndContexts = ListUtils.addAllToNew(traversalBag.getCurrentNodeAndContexts());
            Set<NodeAndContext> processedNodeAndContexts = CollectionUtils.delta(previousNodeAndContexts, currentNodeAndContexts)
                                                                          .getRemoved();

            if (!this.iterator.get()
                              .isLastInCycle())
            {
                this.iterator.get()
                             .lookahead()
                             .appendNodeAndContextsFromPreviousBag(processedNodeAndContexts);
            }

            //
            return result;
        }

        public TraversalBagManager apply(List<ForwardFunctionsContext> forwardFunctionContexts)
        {
            this.traversalBags = StreamUtils.withIntCounter(Optional.ofNullable(forwardFunctionContexts)
                                                                    .orElse(Collections.emptyList())
                                                                    .stream())
                                            .map(forwardFunctionContextAndStep -> new TraversalBag(forwardFunctionContextAndStep.getFirst(),
                                                                                                   forwardFunctionContextAndStep.getSecond()))
                                            .collect(Collectors.toList());
            return this;
        }

        public TraversalStepContext getPrimaryTraversalStepContext()
        {
            return this.traversalBags.stream()
                                     .findFirst()
                                     .map(TraversalBag::getCurrentTraversalStepContext)
                                     .get();
        }

        public TraversalBagManager initializePrimaryBagWithNodes(List<NodeAndContext> startNodeAndContexts)
        {
            this.traversalBags.stream()
                              .findFirst()
                              .get()
                              .setCurrentNodeAndContexts(startNodeAndContexts);
            return this;
        }

        public boolean hasUnprocessedNodes()
        {
            return this.traversalBags.stream()
                                     .anyMatch(TraversalBag::hasUnprocessedNodes);
        }
    }

    private static class TraversalBag
    {
        private final ForwardFunctionsContext forwardFunctionContext;
        private final int                     step;
        private final ExpandedNodesFilter     expandedNodesfilter = this.createExpandedNodesFilter();

        private List<NodeAndContext> currentNodeAndContexts = new ArrayList<>();

        public TraversalBag(ForwardFunctionsContext forwardFunctionContext, int step)
        {
            this.forwardFunctionContext = forwardFunctionContext;
            this.step = step;
        }

        public ExpandedNodesFilter getExpandedNodesFilter()
        {
            return this.expandedNodesfilter;
        }

        public TraversalBag appendNodeAndContextsFromPreviousBag(Set<NodeAndContext> processedNodeAndContexts)
        {
            TraversalStepFilter filter = this.forwardFunctionContext.getTraversalStepFilter()
                                                                    .orElse((node, context) -> true);
            Optional.ofNullable(processedNodeAndContexts)
                    .orElse(Collections.emptySet())
                    .stream()
                    .filter(nodeAndContext -> filter.test(nodeAndContext.getNode(), this.getCurrentTraversalStepContext()))
                    .forEach(this.currentNodeAndContexts::add);
            return this;
        }

        public boolean hasUnprocessedNodes()
        {
            return !this.currentNodeAndContexts.isEmpty();
        }

        public TraversalStepContext getCurrentTraversalStepContext()
        {
            return new TraversalStepContext()
            {
                @Override
                public int getStep()
                {
                    return TraversalBag.this.step;
                }

                @Override
                public ForwardNodeFunction getForwardNodeFunction()
                {
                    return TraversalBag.this.forwardFunctionContext.getForwardFunctionsProvider()
                                                                   .forwardNodeFunction();
                }

                @Override
                public Direction getDirection()
                {
                    return TraversalBag.this.forwardFunctionContext.getForwardFunctionsProvider()
                                                                   .direction();
                }
            };
        }

        public List<NodeAndContext> getCurrentNodeAndContexts()
        {
            return this.currentNodeAndContexts;
        }

        public TraversalBag setCurrentNodeAndContexts(List<NodeAndContext> currentNodeAndContexts)
        {
            this.currentNodeAndContexts = currentNodeAndContexts;
            return this;
        }

        public ForwardFunctionsProvider getForwardFunctions()
        {
            return this.forwardFunctionContext.getForwardFunctionsProvider();
        }

        private ExpandedNodesFilter createExpandedNodesFilter()
        {
            return new ExpandedNodesFilter()
            {
                private Set<NodeIdentity> expandedNodes = new HashSet<>();

                @Override
                public void accept(NodeIdentity nodeIdentity)
                {
                    this.expandedNodes.add(nodeIdentity);
                }

                @Override
                public boolean test(NodeIdentity nodeIdentity)
                {
                    return this.expandedNodes.contains(nodeIdentity);
                }
            };
        }
    }

    private static class VisitedNodesFilterStrategy
    {
        private boolean includeFirstVisitedNodeHitRoutes = false;

        public VistedNodesContext newVisitedNodesContext()
        {
            return new VistedNodesContext(this.includeFirstVisitedNodeHitRoutes);
        }

        public VisitedNodesFilterStrategy setIncludeFirstVisitedNodeHitRoutes(boolean includeFirstVisitedNodeHitRoutes)
        {
            this.includeFirstVisitedNodeHitRoutes = includeFirstVisitedNodeHitRoutes;
            return this;
        }

    }

    public static interface ExpandedNodesFilter extends PredicateConsumer<NodeIdentity>
    {

    }

    public static class VistedNodesContext
    {
        private Set<NodeIdentity> visitedNodes = new HashSet<>();
        private boolean           includeFirstVisitedNodeHitRoutes;

        public VistedNodesContext(boolean includeFirstVisitedNodeHitRoutes)
        {
            super();
            this.includeFirstVisitedNodeHitRoutes = includeFirstVisitedNodeHitRoutes;
        }

        public VisitedNodesHandler newVisitedNodesHandler(ExpandedNodesFilter expandedNodesFilter)
        {
            return new VisitedNodesHandler(this.includeFirstVisitedNodeHitRoutes, this.visitedNodes, expandedNodesFilter);
        }
    }

    public static class VisitedNodesHandler
    {
        private Set<NodeIdentity>   visitedNodes;
        private ExpandedNodesFilter expandedNodesFilter;
        private boolean             includeFirstVisitedNodeHitRoutes;

        public VisitedNodesHandler(boolean includeFirstVisitedNodeHitRoutes, Set<NodeIdentity> visitedNodes, ExpandedNodesFilter expandedNodesFilter)
        {
            this.includeFirstVisitedNodeHitRoutes = includeFirstVisitedNodeHitRoutes;
            this.visitedNodes = visitedNodes;
            this.expandedNodesFilter = expandedNodesFilter;
        }

        public <R> Stream<R> filterVisitedNodesUpfrontOrExecute(NodeAndContext currentNodeAndContext, Set<NodeAndContext> visitedNodesHitPaths,
                                                                Function<Predicate<TraversedEdge>, Stream<R>> operation)
        {
            NodeIdentity currentNodeIdentity = currentNodeAndContext.getNode()
                                                                    .getIdentity();
            this.visitedNodes.add(currentNodeIdentity);

            if (!this.expandedNodesFilter.test(currentNodeIdentity))
            {
                this.expandedNodesFilter.accept(currentNodeIdentity);
                if (this.includeFirstVisitedNodeHitRoutes)
                {
                    return operation.apply(PredicateUtils.allMatching());
                }
                else
                {
                    return operation.apply(edge ->
                    {
                        NodeIdentity nodeIdentity = edge.getNextNode()
                                                        .getIdentity();
                        boolean testResult = PredicateUtils.isCollectionNotContaining(this.visitedNodes)

                                                           .ifFalseThen(excludedNode -> visitedNodesHitPaths.add(currentNodeAndContext.append(edge)))
                                                           .test(nodeIdentity);
                        this.visitedNodes.add(nodeIdentity);
                        return testResult;
                    });
                }
            }
            else
            {
                return Stream.empty();
            }
        }
    }

    /**
     * @author omnaest
     */
    private static class RoutesImpl implements Routes
    {
        private final List<Route> routes;

        public RoutesImpl(List<Route> routes)
        {
            this.routes = routes;
        }

        @Override
        public Stream<Route> stream()
        {
            return this.routes.stream();
        }

        @Override
        public int size()
        {
            return this.routes.size();
        }

        @Override
        public Optional<Route> first()
        {
            return ListUtils.optionalFirst(this.routes);
        }

        @Override
        public boolean hasNoRoutes()
        {
            return this.routes.isEmpty();
        }

        @Override
        public boolean hasRoutes()
        {
            return !this.routes.isEmpty();
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("RoutesImpl [routes=")
                   .append(this.routes)
                   .append("]");
            return builder.toString();
        }

    }

    protected static class NodeAndContext
    {
        private static final double DEFAULT_BUDGET_SCORE = 1.0;

        private NodeAndPath         nodeAndPath;
        private List<TraversedEdge> edges;
        private double              budgetScore;

        public NodeAndContext(Node node, List<Node> path, List<TraversedEdge> edges, double budgetScore)
        {
            this(new NodeAndPath(node, path), edges, budgetScore);
        }

        public NodeAndContext(NodeAndPath nodeAndPath, List<TraversedEdge> edges, double budgetScore)
        {
            this.nodeAndPath = nodeAndPath;
            this.edges = edges;
            this.budgetScore = budgetScore;
        }

        public List<TraversedEdge> getEdges()
        {
            return this.edges.stream()
                             .collect(Collectors.toList());
        }

        public double getBudgetScore()
        {
            return this.budgetScore;
        }

        public static NodeAndContext of(Node node)
        {
            return new NodeAndContext(node, Collections.emptyList(), Collections.emptyList(), DEFAULT_BUDGET_SCORE);
        }

        public NodeAndContext append(TraversedEdge edge)
        {
            return this.append(edge, DEFAULT_BUDGET_SCORE);
        }

        public NodeAndContext append(TraversedEdge edge, double budgetScore)
        {
            return new NodeAndContext(this.nodeAndPath.append(edge.getNextNode()), ListUtils.addToNew(this.edges, edge), budgetScore);
        }

        public Node getNode()
        {
            return this.nodeAndPath.getNode();
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("NodeAndContext [nodeAndPath=")
                   .append(this.nodeAndPath)
                   .append(", edges=")
                   .append(this.edges)
                   .append("]");
            return builder.toString();
        }

        public List<Node> getFullPath()
        {
            return this.nodeAndPath.getFullPath();
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.nodeAndPath == null) ? 0 : this.nodeAndPath.hashCode());
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
            if (!(obj instanceof NodeAndContext))
            {
                return false;
            }
            NodeAndContext other = (NodeAndContext) obj;
            if (this.nodeAndPath == null)
            {
                if (other.nodeAndPath != null)
                {
                    return false;
                }
            }
            else if (!this.nodeAndPath.equals(other.nodeAndPath))
            {
                return false;
            }
            return true;
        }

    }

    protected static class NodeAndPath
    {
        private Node       node;
        private List<Node> path;

        public NodeAndPath(Node node, List<Node> path)
        {
            super();
            this.node = node;
            this.path = path;

        }

        public static NodeAndPath of(Node node)
        {
            return new NodeAndPath(node, Collections.emptyList());
        }

        public NodeAndPath append(Node node)
        {

            return new NodeAndPath(node, ListUtils.addToNew(this.path, this.node));
        }

        public Node getNode()
        {
            return this.node;
        }

        @Override
        public String toString()
        {
            return "NodeAndPath [node=" + this.node + ", path=" + this.path + "]";
        }

        public List<Node> getFullPath()
        {
            return ListUtils.addToNew(this.path, this.node);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.node == null) ? 0 : this.node.hashCode());
            result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
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
            if (!(obj instanceof NodeAndPath))
            {
                return false;
            }
            NodeAndPath other = (NodeAndPath) obj;
            if (this.node == null)
            {
                if (other.node != null)
                {
                    return false;
                }
            }
            else if (!this.node.equals(other.node))
            {
                return false;
            }
            if (this.path == null)
            {
                if (other.path != null)
                {
                    return false;
                }
            }
            else if (!this.path.equals(other.path))
            {
                return false;
            }
            return true;
        }

    }

    public static class RoutesControl
    {
        private final Routes               routes;
        private final LongSupplier         currentCounterProvider;
        private final long                 snapshotCounter;
        private final SkipNodes            skipNodes;
        private final TraversalStepContext traversalStepContext;

        public RoutesControl(Routes routes, SkipNodes skipNodes, long snapshotCounter, LongSupplier currentCounterProvider,
                             TraversalStepContext traversalStepContext)
        {
            super();
            this.routes = routes;
            this.skipNodes = skipNodes;
            this.snapshotCounter = snapshotCounter;
            this.currentCounterProvider = currentCounterProvider;
            this.traversalStepContext = traversalStepContext;
        }

        public void addToSkipNodes(NodeIdentity... nodeIdentities)
        {
            this.addToSkipNodes(Arrays.asList(nodeIdentities));
        }

        public void addRouteToSkipNodes(SimpleRoute route)
        {
            if (route != null)
            {
                this.validateSkipIsStillPossible();
                BiElement<List<Node>, Optional<Node>> remainingListAndLast = ListUtils.splitLast(route.toList());
                this.skipNodes.add(new NodeAndContext(remainingListAndLast.getSecond()
                                                                          .get(),
                                                      remainingListAndLast.getFirst(), Collections.emptyList(), 0.0));
            }
        }

        public void addToSkipNodes(Collection<NodeIdentity> nodeIdentities)
        {
            if (nodeIdentities != null)
            {
                this.validateSkipIsStillPossible();
                this.skipNodes.addAll(nodeIdentities);
            }
        }

        private void validateSkipIsStillPossible()
        {
            if (this.snapshotCounter != this.currentCounterProvider.getAsLong())
            {
                throw new IllegalStateException("Node batch has already been traversed. Please ensure sequencial access to the node stream and no lazy access.");
            }
        }

        public Routes getRoutes()
        {
            return this.routes;
        }

        public TraversalStepContext getTraversalStepContext()
        {
            return this.traversalStepContext;
        }

    }
}
