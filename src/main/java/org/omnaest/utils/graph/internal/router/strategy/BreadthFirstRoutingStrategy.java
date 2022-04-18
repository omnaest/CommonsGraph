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
import java.util.LinkedHashSet;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.omnaest.utils.graph.GraphUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphRouter.RoutingStrategy;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.edge.Edges;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.node.Nodes;
import org.omnaest.utils.graph.domain.traversal.Direction;
import org.omnaest.utils.graph.domain.traversal.ForwardNodeFunction;
import org.omnaest.utils.graph.domain.traversal.Route;
import org.omnaest.utils.graph.domain.traversal.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.traversal.Routes;
import org.omnaest.utils.graph.domain.traversal.SimpleRoute;
import org.omnaest.utils.graph.domain.traversal.TracingGraphAndStatisticsProvider;
import org.omnaest.utils.graph.domain.traversal.Traversal;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphLayer;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphLayers;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphLayersSelector;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphSteppedView;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphView;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphViewConsumer;
import org.omnaest.utils.graph.domain.traversal.Traversal.NodeWeightDeterminationFunction;
import org.omnaest.utils.graph.domain.traversal.Traversal.TraversalStepContext;
import org.omnaest.utils.graph.domain.traversal.Traversal.TraversalStepFilter;
import org.omnaest.utils.graph.domain.traversal.Traversal.VisitedNodesStatistic;
import org.omnaest.utils.graph.domain.traversal.TraversalRoutes;
import org.omnaest.utils.graph.domain.traversal.TraversalRoutesConsumer;
import org.omnaest.utils.graph.domain.traversal.TraversedEdge;
import org.omnaest.utils.graph.domain.traversal.hierarchy.DataBuilder;
import org.omnaest.utils.graph.domain.traversal.hierarchy.HierarchicalNode;
import org.omnaest.utils.graph.domain.traversal.hierarchy.Hierarchy;
import org.omnaest.utils.graph.internal.router.route.RouteImpl;
import org.omnaest.utils.map.counter.IntegerCountedMap;
import org.omnaest.utils.map.counter.IntegerCountedMap.CountedKey;
import org.omnaest.utils.map.counter.IntegerCounterMap;

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

    private static interface BudgetManager extends BiFunction<SuspendedNodesStack, List<NodeAndContext>, List<NodeAndContext>>
    {

        public double calculateBudgetScore(NodeAndContext parentNodeAndContext, TraversedEdges siblingEdges);

    }

    private static class NoBudgetManager implements BudgetManager
    {

        @Override
        public List<NodeAndContext> apply(SuspendedNodesStack suspendedNodesStack, List<NodeAndContext> nodeAndPaths)
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

        @Override
        public List<NodeAndContext> apply(SuspendedNodesStack suspendedNodesStack, List<NodeAndContext> nodeAndContext)
        {
            List<NodeAndContext> sortedNodeAndContext = Stream.concat(suspendedNodesStack.drainAll(), nodeAndContext.stream())
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

            suspendedNodesStack.accept(splittedStream.excluded()
                                                     .collect(Collectors.toList()));
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
            TraversalBag traversalBag = new TraversalBag(new TrackStepContext(forwardFunctions, null), 0);
            VisitedNodesHandler visitedNodesHandler = new VisitedNodesFilterStrategy().newVisitedNodesContext()
                                                                                      .newVisitedNodesHandler(traversalBag, null);
            while (!currentNodeAndPaths.isEmpty())
            {
                Set<NodeIdentity> currentNodes = this.determineNodesFrom(currentNodeAndPaths);
                this.resolveUnresolvedNodesIfEnabled(currentNodes);
                currentNodeAndPaths = this.determineNextNodes(forwardFunctions, currentNodeAndPaths, visitedNodesHandler, SkipNodes.empty(),
                                                              ConsumerUtils.noOperation(), Collections.emptyList(), new SuspendedNodesStack());
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
    public Traversal traverseOutgoing(Collection<NodeIdentity> startNodes)
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
    public Traversal traverseIncoming(Collection<NodeIdentity> startNodes)
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

    @Override
    public Traversal traverseEach(Direction direction, NodeIdentity... startNodes)
    {
        return this.traverseEach(direction, Optional.ofNullable(startNodes)
                                                    .map(Arrays::asList)
                                                    .orElse(Collections.emptyList()));
    }

    @Override
    public Traversal traverseEach(Direction direction, Collection<NodeIdentity> startNodes)
    {
        Traversal traversal = this.traverse(direction);
        Optional.ofNullable(startNodes)
                .map(ListUtils::toList)
                .orElse(Collections.emptyList())
                .forEach(startNode -> traversal.andTraverse(direction, startNode)
                                               .withOwnContext());
        return traversal;
    }

    private Traversal traverse(Collection<NodeIdentity> startNodes, ForwardFunctionsProvider forwardFunction)
    {
        Graph graph = this.graph;
        return new TraversalImpl(forwardFunction, graph, Optional.ofNullable(startNodes)
                                                                 .map(LinkedHashSet::new)
                                                                 .map(MapperUtils.<Set<NodeIdentity>, Set<NodeIdentity>>identity())
                                                                 .orElse(Collections.emptySet()));
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

    private static class TrackStepContext
    {
        private final ForwardFunctionsProvider forwardFunctionsProvider;
        private final TraversalStepFilter      traversalStepFilter;
        private final List<EdgeFilter>         edgeFilters = new ArrayList<>();

        private boolean ownVisitedNodesContext = false;

        public TrackStepContext(ForwardFunctionsProvider forwardFunctionsProvider, TraversalStepFilter traversalStepFilter)
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

        public TrackStepContext addEdgeFilter(EdgeFilter edgeFilter)
        {
            if (edgeFilter != null)
            {
                this.edgeFilters.add(edgeFilter);
            }
            return this;
        }

        public List<EdgeFilter> getEdgeFilters()
        {
            return this.edgeFilters;
        }

        public TrackStepContext setOwnVistedNodesContext(boolean ownVisitedNodesContext)
        {
            this.ownVisitedNodesContext = ownVisitedNodesContext;
            return this;
        }

        public boolean hasOwnVisitedNodesContext()
        {
            return this.ownVisitedNodesContext;
        }

    }

    private static class TracingGraph implements EdgeAndNodeTraversalListener
    {
        private GraphBuilder graphBuilder = GraphUtils.builder();

        @Override
        public void accept(TraversedEdge edge)
        {
            this.graphBuilder.addEdge(edge.getEdge()
                                          .getIdentity());

        }

        @Override
        public void accept(Node node)
        {
            this.graphBuilder.addNode(node.getIdentity());
        }

        public Graph build()
        {
            return this.graphBuilder.build()
                                    .clone();
        }

    }

    private static class TracingGraphManager implements EdgeAndNodeTraversalListener
    {
        private TracingGraph                tracingGraph    = new TracingGraph();
        private TracingGraphConsumerManager consumerManager = new TracingGraphConsumerManager();

        @Override
        public void accept(TraversedEdge edge)
        {
            this.tracingGraph.accept(edge);
        }

        @Override
        public void accept(Node node)
        {
            this.tracingGraph.accept(node);
        }

        public Graph buildGraph()
        {
            return this.tracingGraph.build();
        }

        public Graph buildGraphAndTriggerListeners(VisitedNodesStatistic visitedNodesStatistic)
        {
            Graph graph = this.buildGraph();

            this.consumerManager.accept(new GraphViewImpl(Optional.of(new TracingGraphAndStatisticsProvider()
            {
                @Override
                public VisitedNodesStatistic getVisitedNodesStatistic()
                {
                    return visitedNodesStatistic;
                }

                @Override
                public Optional<Graph> getTracingGraph()
                {
                    return Optional.of(graph);
                }
            }), visitedNodesStatistic.getNumberOfSteps()));

            return graph;
        }

        public TracingGraphManager addListener(GraphViewConsumer graphConsumer)
        {
            this.consumerManager.addListener(graphConsumer);
            return this;
        }
    }

    private static class TracingGraphConsumerManager implements GraphViewConsumer
    {
        private List<GraphViewConsumer> consumers = new ArrayList<>();

        @Override
        public void accept(GraphView graphView)
        {
            this.consumers.forEach(consumer ->
            {
                consumer.accept(graphView);
            });
        }

        public void addListener(GraphViewConsumer graphConsumer)
        {
            if (graphConsumer != null)
            {
                this.consumers.add(graphConsumer);
            }
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

    private static class StartNodeAndForwardFunctionContext
    {
        private final Set<NodeIdentity>      startNodes        = new HashSet<>();
        private final List<TrackStepContext> trackStepContexts = new ArrayList<>();

        public StartNodeAndForwardFunctionContext addStartNodesWithContext(Set<NodeIdentity> startNodes, TrackStepContext trackStepContext)
        {
            this.startNodes.addAll(Optional.ofNullable(startNodes)
                                           .orElse(Collections.emptySet()));
            boolean hasOwnVisitedNodesContext = this.getCurrentTrackStepContext()
                                                    .map(TrackStepContext::hasOwnVisitedNodesContext)
                                                    .orElse(false);
            this.trackStepContexts.add(trackStepContext.setOwnVistedNodesContext(hasOwnVisitedNodesContext));
            return this;
        }

        public StartNodeAndForwardFunctionContext addEdgeFilter(EdgeFilter edgeFilter)
        {
            this.getCurrentTrackStepContext()
                .ifPresent(context -> context.addEdgeFilter(edgeFilter));
            return this;
        }

        private Optional<TrackStepContext> getCurrentTrackStepContext()
        {
            return ListUtils.optionalLast(this.trackStepContexts);
        }

        public Set<NodeIdentity> getStartNodes()
        {
            return this.startNodes;
        }

        public List<TrackStepContext> getForwardFunctionContexts()
        {
            return this.trackStepContexts;
        }

        public StartNodeAndForwardFunctionContext setOwnVistedNodesContext(boolean ownVisitedNodesContext)
        {
            this.getCurrentTrackStepContext()
                .orElseThrow(() -> new IllegalStateException("Please define a traversal before calling this method. Consider the javadoc of withOwnContext()"))
                .setOwnVistedNodesContext(ownVisitedNodesContext);
            return this;
        }

    }

    private static class StartNodeAndForwardFunctionContextTracks
    {
        private List<StartNodeAndForwardFunctionContext> contexts = new ArrayList<>();

        public StartNodeAndForwardFunctionContext getOrCreateCurrentContextTrack()
        {
            if (this.contexts.isEmpty())
            {
                this.createNewContextTrack();
            }
            return ListUtils.last(this.contexts);
        }

        public StartNodeAndForwardFunctionContextTracks addStartNodesWithContextToCurrentTrack(Set<NodeIdentity> startNodes,
                                                                                               TrackStepContext forwardFunctionsContext)
        {
            this.getOrCreateCurrentContextTrack()
                .addStartNodesWithContext(startNodes, forwardFunctionsContext);
            return this;
        }

        public StartNodeAndForwardFunctionContextTracks addForwardFunctionContextToCurrentTrack(TrackStepContext forwardFunctionsContext)
        {
            this.getOrCreateCurrentContextTrack()
                .addStartNodesWithContext(Collections.emptySet(), forwardFunctionsContext);
            return this;
        }

        public StartNodeAndForwardFunctionContextTracks createNewContextTrack()
        {
            this.contexts.add(new StartNodeAndForwardFunctionContext());
            return this;
        }

        public StartNodeAndForwardFunctionContextTracks forEachTrack(Consumer<StartNodeAndForwardFunctionContext> trackConsumer)
        {
            this.contexts.forEach(trackConsumer);
            return this;
        }

        public StartNodeAndForwardFunctionContextTracks addEdgeFilter(EdgeFilter edgeFilter)
        {
            this.getOrCreateCurrentContextTrack()
                .addEdgeFilter(edgeFilter);
            return this;
        }
    }

    public static interface TraversalStatistics
    {

    }

    private static class TraversalStatisticsCollector implements TraversalStatistics, EdgeAndNodeTraversalListener
    {
        private IntegerCounterMap<NodeIdentity> nodeToVisitedCount = IntegerCounterMap.newInstance();

        public IntegerCountedMap<NodeIdentity> getNodeToVisitedCount()
        {
            return this.nodeToVisitedCount.clone()
                                          .asImmutable();
        }

        @Override
        public void accept(TraversedEdge edge)
        {
            if (edge != null)
            {
                this.accept(edge.getNextNode());
            }
        }

        @Override
        public void accept(Node node)
        {
            if (node != null)
            {
                this.nodeToVisitedCount.incrementByOne(node.getIdentity());
            }
        }

    }

    private static class VisitedNodesStatisticImpl implements VisitedNodesStatistic
    {
        private final IntegerCountedMap<NodeIdentity> nodeToVisitedCount;
        private final long                            numberOfSteps;

        private VisitedNodesStatisticImpl(IntegerCountedMap<NodeIdentity> nodeToVisitedCount, long numberOfSteps)
        {
            this.nodeToVisitedCount = nodeToVisitedCount;
            this.numberOfSteps = numberOfSteps;
        }

        @Override
        public int getCountFor(NodeIdentity nodeIdentity)
        {
            return this.nodeToVisitedCount.getAsInt(nodeIdentity);
        }

        @Override
        public Set<NodeIdentity> nodes()
        {
            return this.nodeToVisitedCount.keySet();
        }

        @Override
        public Set<NodeIdentity> nodesByCount(int count)
        {
            return this.nodeToVisitedCount.entries()
                                          .filter(entry -> entry.getCount() == count)
                                          .map(CountedKey<NodeIdentity>::getKey)
                                          .collect(Collectors.toSet());
        }

        @Override
        public int getNumberOfSteps()
        {
            return (int) this.numberOfSteps;
        }

        @Override
        public int getMaxCount()
        {
            return this.nodeToVisitedCount.getMaxCount();
        }
    }

    private static class GraphViewImpl implements GraphView
    {
        private final Optional<TracingGraphAndStatisticsProvider> lastProvider;
        private final long                                        numberOfSteps;

        private GraphViewImpl(Optional<TracingGraphAndStatisticsProvider> lastProvider, int numberOfSteps)
        {
            this.lastProvider = lastProvider;
            this.numberOfSteps = numberOfSteps;
        }

        @Override
        public Graph get()
        {
            return this.lastProvider.flatMap(TracingGraphAndStatisticsProvider::getTracingGraph)
                                    .orElse(GraphUtils.empty());
        }

        @Override
        public GraphLayersSelector layers()
        {
            return new GraphLayersSelector()
            {
                @Override
                public GraphLayers byVisitedCount()
                {
                    return this.byVisitedCount(VisitedNodesStatistic::nodesByCount);
                }

                @Override
                public GraphLayers byAtLeastVisitedCount()
                {
                    return this.byVisitedCount((visitedNodesStatistic, count) -> IntStream.rangeClosed(count, visitedNodesStatistic.getMaxCount())
                                                                                          .mapToObj(visitedNodesStatistic::nodesByCount)
                                                                                          .flatMap(Set<NodeIdentity>::stream)
                                                                                          .collect(Collectors.toSet()));
                }

                public GraphLayers byVisitedCount(BiFunction<VisitedNodesStatistic, Integer, Set<NodeIdentity>> nodesResolver)
                {
                    VisitedNodesStatistic visitedNodesStatistic = GraphViewImpl.this.lastProvider.map(TracingGraphAndStatisticsProvider::getVisitedNodesStatistic)
                                                                                                 .orElse(new VisitedNodesStatisticImpl(IntegerCountedMap.empty(),
                                                                                                                                       GraphViewImpl.this.numberOfSteps));
                    Graph finalTracingGraph = GraphViewImpl.this.get();
                    return new GraphLayers()
                    {
                        @Override
                        public Stream<GraphLayer> stream()
                        {
                            return IntStream.rangeClosed(0, visitedNodesStatistic.getMaxCount())
                                            .mapToObj(count -> new GraphLayer()
                                            {
                                                @Override
                                                public Graph get()
                                                {

                                                    Set<NodeIdentity> nodes = nodesResolver.apply(visitedNodesStatistic, count);
                                                    return finalTracingGraph.subGraph()
                                                                            .withIncludedNodes(nodes)
                                                                            .build();
                                                }

                                                @Override
                                                public int getIndex()
                                                {
                                                    return count;
                                                }
                                            });
                        }
                    };
                }

            };
        }
    }

    private static class GraphSteppedViewImpl extends GraphViewImpl implements GraphSteppedView
    {
        private final List<TracingGraphAndStatisticsProvider> providers;

        private GraphSteppedViewImpl(Optional<TracingGraphAndStatisticsProvider> lastProvider, List<TracingGraphAndStatisticsProvider> providers)
        {
            super(lastProvider, providers.size());
            this.providers = providers;
        }

        @Override
        public Stream<Graph> stream()
        {
            return this.steps();
        }

        @Override
        public Stream<Graph> steps()
        {
            return this.providers.stream()
                                 .map(TracingGraphAndStatisticsProvider::getTracingGraph)
                                 .map(MapperUtils.mapOptionalToValue());
        }

    }

    private class TraversalImpl implements Traversal
    {
        private final Graph                                    graph;
        private final StartNodeAndForwardFunctionContextTracks startNodeAndForwardFunctionContextTrack = new StartNodeAndForwardFunctionContextTracks();
        private final List<TraversalRoutesConsumer>            alreadyVisitedNodesHitHandlers          = new ArrayList<>();
        private final VisitedNodesFilterStrategy               visitedNodesFilterStrategy              = new VisitedNodesFilterStrategy();
        private final List<WeightedTerminationHandler>         weightedTerminationHandlers             = new ArrayList<>();
        private final TraversalStatisticsCollector             traversalStatisticsCollector            = new TraversalStatisticsCollector();
        private final EdgeAndNodeTraversalListenerManager      edgeTraversalListenerManager            = new EdgeAndNodeTraversalListenerManager().add(this.traversalStatisticsCollector);;

        private Optional<TracingGraphManager> tracingGraphManager = Optional.empty();
        private int                           stepsLimit          = Integer.MAX_VALUE;
        private int                           stepsToSkip         = 0;

        private TraversalImpl(ForwardFunctionsProvider forwardFunction, Graph graph, Set<NodeIdentity> startNodes)
        {
            this.graph = graph;
            this.startNodeAndForwardFunctionContextTrack.addStartNodesWithContextToCurrentTrack(startNodes, new TrackStepContext(forwardFunction, null));
        }

        @Override
        public Traversal withEdgeFilter(EdgeFilter edgeFilter)
        {
            if (edgeFilter != null)
            {
                this.startNodeAndForwardFunctionContextTrack.addEdgeFilter(edgeFilter);
            }
            return this;
        }

        @Override
        public Traversal withExcludingEdgeByTagFilter(Tag... tag)
        {
            return this.withEdgeFilter(edge -> !edge.hasAnyTag(tag));
        }

        @Override
        public Traversal withTracingGraph()
        {
            if (!this.tracingGraphManager.isPresent())
            {
                TracingGraphManager manager = new TracingGraphManager();
                this.edgeTraversalListenerManager.add(manager);
                this.tracingGraphManager = Optional.of(manager);
            }
            return this;
        }

        @Override
        public Traversal withTracingGraphStepListener(GraphViewConsumer graphConsumer)
        {
            if (graphConsumer != null)
            {
                this.withTracingGraph();
                this.tracingGraphManager.get()
                                        .addListener(graphConsumer);
            }
            return this;
        }

        private Optional<Graph> determineCurrentTracingGraphForLastTraversalStepAndTriggerListeners(int traversalStep)
        {
            return this.tracingGraphManager.map(manager -> manager.buildGraphAndTriggerListeners(new VisitedNodesStatisticImpl(this.traversalStatisticsCollector.getNodeToVisitedCount(),
                                                                                                                               traversalStep + 1)));
        }

        @Override
        public Stream<TraversalRoutes> stream()
        {
            return StreamUtils.fromIterator(new BreadthFirstIterator(this.startNodeAndForwardFunctionContextTrack, this.graph,
                                                                     this.alreadyVisitedNodesHitHandlers, this.visitedNodesFilterStrategy,
                                                                     this.edgeTraversalListenerManager))
                              .map(routesControl -> new TraversalRoutesImpl(routesControl,
                                                                            this.determineCurrentTracingGraphForLastTraversalStepAndTriggerListeners(routesControl.getTraversalStepContext()
                                                                                                                                                                  .getStep()),
                                                                            new VisitedNodesStatisticImpl(this.traversalStatisticsCollector.getNodeToVisitedCount(),
                                                                                                          routesControl.getTraversalStepContext()
                                                                                                                       .getStep()
                                                                                                                  + 1)))
                              .map(MapperUtils.identityCast(TraversalRoutes.class))
                              .peek(PeekUtils.all(this.weightedTerminationHandlers))
                              .skip(this.stepsToSkip)
                              .limit(this.stepsLimit);
        }

        @Override
        public TraversalTerminal and()
        {
            List<TracingGraphAndStatisticsProvider> providers = this.withTracingGraph()
                                                                    .includingFirstRouteOfAlreadyVisitedNodes()
                                                                    .stream()
                                                                    .map(routes -> (TracingGraphAndStatisticsProvider) routes)
                                                                    .collect(Collectors.toList());
            return new TraversalTerminal()
            {
                @Override
                public GraphSteppedView viewAsGraph()
                {
                    Optional<TracingGraphAndStatisticsProvider> lastProvider = ListUtils.optionalLast(providers);
                    return new GraphSteppedViewImpl(lastProvider, providers);
                }

                @Override
                public VisitedNodesStatistic getVisitedNodesStatistic()
                {
                    long numberOfSteps = providers.size();
                    IntegerCountedMap<NodeIdentity> nodeToVisitedCount = TraversalImpl.this.traversalStatisticsCollector.getNodeToVisitedCount();
                    return new VisitedNodesStatisticImpl(nodeToVisitedCount, numberOfSteps);
                }

            };
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
        public Traversal andAfterwardsTraverseIncoming()
        {
            return this.andAfterwardsTraverse(Direction.INCOMING);
        }

        @Override
        public Traversal andAfterwardsTraverseOutgoing()
        {
            return this.andAfterwardsTraverse(Direction.OUTGOING);
        }

        @Override
        public Traversal andAfterwardsTraverse(TraversalStepFilter filter, Direction... directions)
        {
            Optional.ofNullable(directions)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList())
                    .forEach(direction -> this.startNodeAndForwardFunctionContextTrack.addForwardFunctionContextToCurrentTrack(new TrackStepContext(ForwardFunctions.byDirection(direction),
                                                                                                                                                    filter)));
            return this;
        }

        @Override
        public Traversal andTraverseIncoming(NodeIdentity... startNodes)
        {
            return this.andTraverse(Direction.INCOMING, startNodes);
        }

        @Override
        public Traversal andTraverseOutgoing(NodeIdentity... startNodes)
        {
            return this.andTraverse(Direction.OUTGOING, startNodes);
        }

        @Override
        public Traversal andTraverse(Direction direction, NodeIdentity... startNodes)
        {
            return this.andTraverse(direction, Arrays.asList(startNodes));
        }

        @Override
        public Traversal andTraverse(Direction direction, Collection<NodeIdentity> startNodes)
        {
            TraversalStepFilter filter = null;
            return this.andTraverse(direction, filter, startNodes);
        }

        @Override
        public Traversal andTraverseEach(Direction direction, Collection<NodeIdentity> startNodes)
        {
            Traversal traversal = this.andTraverse(direction);
            Optional.ofNullable(startNodes)
                    .map(ListUtils::toList)
                    .orElse(Collections.emptyList())
                    .forEach(startNode -> traversal.andTraverse(direction, startNode)
                                                   .withOwnContext());
            return traversal;
        }

        @Override
        public Traversal andTraverse(Direction direction, TraversalStepFilter filter, NodeIdentity... startNodes)
        {
            return this.andTraverse(direction, filter, Arrays.asList(startNodes));
        }

        public Traversal andTraverse(Direction direction, TraversalStepFilter filter, Collection<NodeIdentity> startNodes)
        {
            Optional.ofNullable(direction)
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList())
                    .forEach(iDirection -> this.startNodeAndForwardFunctionContextTrack.createNewContextTrack()
                                                                                       .addStartNodesWithContextToCurrentTrack(SetUtils.toSet(startNodes),
                                                                                                                               new TrackStepContext(ForwardFunctions.byDirection(iDirection),
                                                                                                                                                    filter)));
            return this;
        }

        @Override
        public Traversal withOwnContext()
        {
            this.startNodeAndForwardFunctionContextTrack.getOrCreateCurrentContextTrack()
                                                        .setOwnVistedNodesContext(true);
            return this;
        }

        @Override
        public Traversal andAfterwardsTraverse(Direction... directions)
        {
            return this.andAfterwardsTraverse(null, directions);
        }

        @Override
        public Traversal limitStepsTo(int steps)
        {
            this.stepsLimit = steps;
            return this;
        }

        @Override
        public Traversal skipSteps(int steps)
        {
            this.stepsToSkip = steps;
            return this;
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
                                                    Consumer<Set<NodeAndContext>> visitedNodesHitConsumer, List<EdgeFilter> edgeFilters,
                                                    SuspendedNodesStack suspendedNodesStack)
    {
        Set<NodeAndContext> visitedNodesHitPaths = new HashSet<>();
        List<NodeAndContext> result = currentNodeAndPaths.stream()
                                                         .filter(skipNodes::matchesNot)
                                                         .flatMap(this.explodeCurrentNodeIntoNextNodes(forwardFunctions, visitedNodesHandler,
                                                                                                       visitedNodesHitPaths))
                                                         .filter(skipNodes::matchesNot)
                                                         .filter(this.createEffectiveEdgesFilter(forwardFunctions.forwardEdgeFinderFunction(), edgeFilters))
                                                         .collect(Collectors.toList());

        if (!visitedNodesHitPaths.isEmpty())
        {
            visitedNodesHitConsumer.accept(visitedNodesHitPaths);
        }

        return visitedNodesHandler.peekAndPopulateToEdgeTraversalListener(this.budgetManager.apply(suspendedNodesStack, result));
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

    private Predicate<NodeAndContext> createEffectiveEdgesFilter(BiFunction<Node, Node, Optional<Edge>> edgesFunction, List<EdgeFilter> edgeFilters)
    {
        List<EdgeFilter> mergedEdgeFilters = ListUtils.mergedList(this.edgeFilters, edgeFilters);
        if (mergedEdgeFilters.isEmpty())
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
                                    .map(edge -> PredicateUtils.all(mergedEdgeFilters)
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
        private final RoutesControl         routesControl;
        private final VisitedNodesStatistic visitedNodesStatistic;
        private final Optional<Graph>       tracingGraph;

        private TraversalRoutesImpl(RoutesControl routesControl, Optional<Graph> tracingGraph, VisitedNodesStatistic visitedNodesStatistic)
        {
            this.routesControl = routesControl;
            this.tracingGraph = tracingGraph;
            this.visitedNodesStatistic = visitedNodesStatistic;
        }

        @Override
        public Optional<Graph> getTracingGraph()
        {
            return this.tracingGraph;
        }

        @Override
        public VisitedNodesStatistic getVisitedNodesStatistic()
        {
            return this.visitedNodesStatistic;
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

        private BreadthFirstIterator(StartNodeAndForwardFunctionContextTracks startNodeAndForwardFunctionContextTracks, Graph graph,
                                     List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers, VisitedNodesFilterStrategy visitedNodesFilterStrategy,
                                     EdgeAndNodeTraversalListenerManager edgeTraversalListenerManager)
        {

            this.traversalBagManager = new TraversalBagManager().apply(graph, startNodeAndForwardFunctionContextTracks);

            List<NodeAndContext> primaryStartNodesAndContexts = this.traversalBagManager.getStartNodesAndContexts();
            this.populateStartNodesToEdgeTraversalListener(edgeTraversalListenerManager, primaryStartNodesAndContexts);

            SkipNodes skipNodes = new SkipNodes();
            this.currentRoutesControl = CachedElement.of(new BreadthFirstSupplier(this.traversalBagManager, this.counter, skipNodes,
                                                                                  alreadyVisitedNodesHitHandlers, visitedNodesFilterStrategy,
                                                                                  edgeTraversalListenerManager))
                                                     .set(new RoutesControl(new RoutesImpl(BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(primaryStartNodesAndContexts)),
                                                                            skipNodes, this.counter.incrementAndGet(), () -> this.counter.get(),
                                                                            this.traversalBagManager.getPrimaryTraversalStepContext()));
        }

        private void populateStartNodesToEdgeTraversalListener(EdgeAndNodeTraversalListenerManager edgeTraversalListenerManager,
                                                               List<NodeAndContext> startNodeAndContexts)
        {
            Optional.ofNullable(edgeTraversalListenerManager)
                    .ifPresent(manager -> startNodeAndContexts.stream()
                                                              .map(NodeAndContext::getNode)
                                                              .forEach(manager::accept));
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
            private final VistedNodesContext                  visitedNodesContext;
            private final SkipNodes                           skipNodes;
            private final AtomicLong                          counter;
            private final List<TraversalRoutesConsumer>       alreadyVisitedNodesHitHandlers;
            private final TraversalBagManager                 traversalBagManager;
            private final EdgeAndNodeTraversalListenerManager edgeTraversalListenerManager;

            private BreadthFirstSupplier(TraversalBagManager traversalBagManager, AtomicLong counter, SkipNodes skipNodes,
                                         List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers, VisitedNodesFilterStrategy visitedNodesFilterStrategy,
                                         EdgeAndNodeTraversalListenerManager edgeTraversalListenerManager)
            {
                this.traversalBagManager = traversalBagManager;
                this.counter = counter;
                this.skipNodes = skipNodes;
                this.alreadyVisitedNodesHitHandlers = alreadyVisitedNodesHitHandlers;
                this.edgeTraversalListenerManager = edgeTraversalListenerManager;
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
                                                                                                               this.visitedNodesContext.newVisitedNodesHandler(traversalBag,
                                                                                                                                                               this.edgeTraversalListenerManager),
                                                                                                               this.skipNodes,
                                                                                                               this.createVisitedNodesHitConsumer(),
                                                                                                               traversalBag.getEdgeFilters(),
                                                                                                               traversalBag.getSuspendedNodesStack()));

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

    private static class TraversalBagChain
    {
        private List<TraversalBag>                                  traversalBags;
        private CachedElement<RoundRobinListIterator<TraversalBag>> iterator = CachedElement.of(() -> IteratorUtils.roundRobinListIterator(this.traversalBags));

        public TraversalBagChain(List<TraversalBag> traversalBags)
        {
            this.traversalBags = traversalBags;
        }

        public TraversalBag getPrimaryTraversalBag()
        {
            return ListUtils.first(this.traversalBags);
        }

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

        public TraversalStepContext getPrimaryTraversalStepContext()
        {
            return this.traversalBags.stream()
                                     .findFirst()
                                     .map(TraversalBag::getCurrentTraversalStepContext)
                                     .get();
        }

        public boolean hasUnprocessedNodes()
        {
            return this.traversalBags.stream()
                                     .anyMatch(TraversalBag::hasUnprocessedNodes);
        }
    }

    private static class TraversalBagManager
    {
        private List<TraversalBagChain>                                  traversalBagChains = new ArrayList<>();
        private CachedElement<RoundRobinListIterator<TraversalBagChain>> iterator           = CachedElement.of(() -> IteratorUtils.roundRobinListIterator(this.traversalBagChains));

        public <R> R processNextAndGet(Function<TraversalBag, R> operation)
        {
            return this.iterator.get()
                                .next()
                                .processNextAndGet(operation);
        }

        /**
         * Returns all the start nodes of the overall first iterations.
         */
        public List<NodeAndContext> getStartNodesAndContexts()
        {
            return this.traversalBagChains.stream()
                                          .map(TraversalBagChain::getPrimaryTraversalBag)
                                          .map(TraversalBag::getCurrentNodeAndContexts)
                                          .flatMap(List<NodeAndContext>::stream)
                                          .collect(Collectors.toList());
        }

        public TraversalBagManager apply(Graph graph, StartNodeAndForwardFunctionContextTracks startNodeAndForwardFunctionContextTracks)
        {
            startNodeAndForwardFunctionContextTracks.forEachTrack(track ->
            {
                List<TrackStepContext> forwardFunctionContexts = track.getForwardFunctionContexts();
                Set<NodeIdentity> startNodes = track.getStartNodes();
                List<NodeAndContext> startNodeAndContexts = Optional.ofNullable(startNodes)
                                                                    .orElse(Collections.emptySet())
                                                                    .stream()
                                                                    .map(graph::findNodeById)
                                                                    .filter(Optional::isPresent)
                                                                    .map(Optional::get)
                                                                    .map(NodeAndContext::of)
                                                                    .collect(Collectors.toList());
                List<TraversalBag> traversalBags = StreamUtils.withIntCounter(Optional.ofNullable(forwardFunctionContexts)
                                                                                      .orElse(Collections.emptyList())
                                                                                      .stream())
                                                              .map(forwardFunctionContextAndStep -> new TraversalBag(forwardFunctionContextAndStep.getFirst(),
                                                                                                                     forwardFunctionContextAndStep.getSecond()).setCurrentNodeAndContexts(startNodeAndContexts))
                                                              .collect(Collectors.toList());
                this.traversalBagChains.add(new TraversalBagChain(traversalBags));
            });
            return this;
        }

        public TraversalStepContext getPrimaryTraversalStepContext()
        {
            return ListUtils.first(this.traversalBagChains)
                            .getPrimaryTraversalStepContext();
        }

        public boolean hasUnprocessedNodes()
        {
            return this.traversalBagChains.stream()
                                          .anyMatch(TraversalBagChain::hasUnprocessedNodes);
        }
    }

    private static class SuspendedNodesStack implements Consumer<List<NodeAndContext>>
    {
        private Set<NodeAndContext> nodes = new HashSet<>();

        @Override
        public void accept(List<NodeAndContext> nodes)
        {
            if (nodes != null)
            {
                this.nodes.addAll(nodes);
            }
        }

        public Stream<NodeAndContext> drainAll()
        {
            return SetUtils.drainAll(this.nodes)
                           .stream();
        }

    }

    public static class TraversalBagId
    {
    }

    private static class TraversalBag
    {
        private final TraversalBagId      traversalBagId      = new TraversalBagId();
        private final TrackStepContext    trackStepContext;
        private final int                 step;
        private final ExpandedNodesFilter expandedNodesfilter = this.createExpandedNodesFilter();
        private final SuspendedNodesStack suspendedNodesStack = new SuspendedNodesStack();

        private List<NodeAndContext> currentNodeAndContexts = new ArrayList<>();

        public TraversalBag(TrackStepContext trackStepContext, int step)
        {
            this.trackStepContext = trackStepContext;
            this.step = step;
        }

        public TraversalBagId getTraversalBagId()
        {
            return this.traversalBagId;
        }

        public SuspendedNodesStack getSuspendedNodesStack()
        {
            return this.suspendedNodesStack;
        }

        public List<EdgeFilter> getEdgeFilters()
        {
            return this.trackStepContext.getEdgeFilters();
        }

        public ExpandedNodesFilter getExpandedNodesFilter()
        {
            return this.expandedNodesfilter;
        }

        public TraversalBag appendNodeAndContextsFromPreviousBag(Set<NodeAndContext> processedNodeAndContexts)
        {
            TraversalStepFilter filter = this.trackStepContext.getTraversalStepFilter()
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

        public boolean hasOwnVisitedNodesContext()
        {
            return this.trackStepContext.hasOwnVisitedNodesContext();
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
                    return TraversalBag.this.trackStepContext.getForwardFunctionsProvider()
                                                             .forwardNodeFunction();
                }

                @Override
                public Direction getDirection()
                {
                    return TraversalBag.this.trackStepContext.getForwardFunctionsProvider()
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
            return this.trackStepContext.getForwardFunctionsProvider();
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
        private Set<NodeIdentity>                      globalVisitedNodes           = new HashSet<>();
        private Map<TraversalBagId, Set<NodeIdentity>> traversalBagIdToVisitedNodes = new HashMap<>();
        private boolean                                includeFirstVisitedNodeHitRoutes;

        public VistedNodesContext(boolean includeFirstVisitedNodeHitRoutes)
        {
            super();
            this.includeFirstVisitedNodeHitRoutes = includeFirstVisitedNodeHitRoutes;
        }

        protected VisitedNodesHandler newVisitedNodesHandler(TraversalBag traversalBag, EdgeAndNodeTraversalListener edgeTraversalListener)
        {
            Set<NodeIdentity> visitedNodes = traversalBag.hasOwnVisitedNodesContext()
                    ? this.traversalBagIdToVisitedNodes.computeIfAbsent(traversalBag.getTraversalBagId(), id -> new HashSet<>())
                    : this.globalVisitedNodes;
            return new VisitedNodesHandler(this.includeFirstVisitedNodeHitRoutes, visitedNodes, traversalBag.getExpandedNodesFilter(), edgeTraversalListener);
        }
    }

    public static interface EdgeAndNodeTraversalListener extends Consumer<TraversedEdge>
    {
        public void accept(Node node);
    }

    private static class EdgeAndNodeTraversalListenerManager implements EdgeAndNodeTraversalListener
    {
        private List<EdgeAndNodeTraversalListener> listeners = new ArrayList<>();

        @Override
        public void accept(TraversedEdge edge)
        {
            this.listeners.forEach(listener -> listener.accept(edge));
        }

        @Override
        public void accept(Node node)
        {
            this.listeners.forEach(listener -> listener.accept(node));
        }

        public EdgeAndNodeTraversalListenerManager add(EdgeAndNodeTraversalListener listener)
        {
            if (listener != null)
            {
                this.listeners.add(listener);
            }
            return this;
        }

    }

    public static class VisitedNodesHandler
    {
        private Set<NodeIdentity>            visitedNodes;
        private ExpandedNodesFilter          expandedNodesFilter;
        private boolean                      includeFirstVisitedNodeHitRoutes;
        private EdgeAndNodeTraversalListener edgeTraversalListener;

        public VisitedNodesHandler(boolean includeFirstVisitedNodeHitRoutes, Set<NodeIdentity> visitedNodes, ExpandedNodesFilter expandedNodesFilter,
                                   EdgeAndNodeTraversalListener edgeTraversalListener)
        {
            this.includeFirstVisitedNodeHitRoutes = includeFirstVisitedNodeHitRoutes;
            this.visitedNodes = visitedNodes;
            this.expandedNodesFilter = expandedNodesFilter;
            this.edgeTraversalListener = edgeTraversalListener;
        }

        public <R> Stream<R> filterVisitedNodesUpfrontOrExecute(NodeAndContext currentNodeAndContext, Set<NodeAndContext> visitedNodesHitPaths,
                                                                Function<Predicate<TraversedEdge>, Stream<R>> operation)
        {
            Node currentNode = currentNodeAndContext.getNode();
            NodeIdentity currentNodeIdentity = currentNode.getIdentity();

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

        private void populateToEdgeTraversalListener(TraversedEdge edge)
        {
            Optional.ofNullable(this.edgeTraversalListener)
                    .ifPresent(listener -> listener.accept(edge));
        }

        private void populateToEdgeTraversalListener(Node node)
        {
            Optional.ofNullable(this.edgeTraversalListener)
                    .ifPresent(listener -> listener.accept(node));
        }

        public List<NodeAndContext> peekAndPopulateToEdgeTraversalListener(List<NodeAndContext> nodes)
        {
            Optional.ofNullable(nodes)
                    .orElse(Collections.emptyList())
                    .forEach(nodeAndContext ->
                    {
                        Optional<TraversedEdge> edge = nodeAndContext.getEdge();
                        edge.ifPresent(this::populateToEdgeTraversalListener);

                        if (!edge.isPresent())
                        {
                            this.populateToEdgeTraversalListener(nodeAndContext.getNode());
                        }
                    });

            return nodes;
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

        public Optional<TraversedEdge> getEdge()
        {
            return ListUtils.optionalLast(this.edges);
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
