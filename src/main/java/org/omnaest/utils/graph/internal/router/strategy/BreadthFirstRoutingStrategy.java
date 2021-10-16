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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.AssertionUtils;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.element.cached.CachedElement;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.Direction;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.GraphRouter.RoutingStrategy;
import org.omnaest.utils.graph.domain.GraphRouter.Traversal;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutes;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutesConsumer;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.domain.Tag;
import org.omnaest.utils.graph.internal.router.route.RouteImpl;

public class BreadthFirstRoutingStrategy implements RoutingStrategy
{
    private Graph   graph;
    private boolean enableNodeResolving = true;

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
    public Routes findAllIncomingRoutesBetween(NodeIdentity from, NodeIdentity to)
    {
        Function<Node, Stream<Node>> forwardFunction = node -> node.getIncomingNodes()
                                                                   .stream();
        return this.findAllRoutesBetween(from, to, forwardFunction);
    }

    @Override
    public Routes findAllOutgoingRoutesBetween(NodeIdentity from, NodeIdentity to)
    {
        Function<Node, Stream<Node>> forwardFunction = node -> node.getOutgoingNodes()
                                                                   .stream();
        return this.findAllRoutesBetween(from, to, forwardFunction);
    }

    public Routes findAllRoutesBetween(NodeIdentity from, NodeIdentity to, Function<Node, Stream<Node>> forwardFunction)
    {
        Optional<Node> startNode = this.graph.findNodeById(from);
        List<Route> routes = new ArrayList<>();
        if (startNode.isPresent())
        {
            List<NodeAndPath> currentNodeAndPaths = new ArrayList<>();
            currentNodeAndPaths.add(NodeAndPath.of(startNode.get()));

            if (this.graph.findNodeById(to)
                          .map(node -> node.equals(startNode.get()))
                          .orElse(false))
            {
                routes.add(new RouteImpl(Arrays.asList(startNode.get()
                                                                .getIdentity()),
                                         this.graph));
            }
            Set<NodeIdentity> visitedNodes = new HashSet<>();
            while (!currentNodeAndPaths.isEmpty())
            {
                Set<NodeIdentity> currentNodes = this.determineNodesFrom(currentNodeAndPaths);
                visitedNodes.addAll(currentNodes);
                this.resolveUnresolvedNodesIfEnabled(currentNodes);
                currentNodeAndPaths = this.determineNextNodes(forwardFunction, currentNodeAndPaths, visitedNodes, SkipNodes.empty(),
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
        return this.traverse(startNodes, node -> node.getOutgoingNodes()
                                                     .stream());

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
        return this.traverse(startNodes, node -> node.getIncomingNodes()
                                                     .stream());
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

    private Traversal traverse(Set<NodeIdentity> startNodes, Function<Node, Stream<Node>> forwardFunction)
    {
        Graph graph = this.graph;
        return new Traversal()
        {
            private List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers = new ArrayList<>();

            @Override
            public Stream<TraversalRoutes> stream()
            {
                return StreamUtils.fromIterator(new BreadthFirstIterator(startNodes, forwardFunction, graph, this.alreadyVisitedNodesHitHandlers))
                                  .map(TraversalRoutesImpl::new);
            }

            @Override
            public Traversal withAlreadyVisitedNodesHitHandler(TraversalRoutesConsumer routesConsumer)
            {
                this.alreadyVisitedNodesHitHandlers.add(routesConsumer);
                return this;
            }
        };

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

    private Set<NodeIdentity> determineNodesFrom(List<NodeAndPath> currentNodes)
    {
        return currentNodes.stream()
                           .map(NodeAndPath::getNode)
                           .map(Node::getIdentity)
                           .collect(Collectors.toSet());
    }

    private List<Route> determineRoutesByMatchingNodes(Optional<Node> targetNode, List<NodeAndPath> currentNodes)
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

    private List<Route> wrapMatchingNodeAndPathsIntoRoutes(Collection<NodeAndPath> matchingNodes)
    {
        return matchingNodes.stream()
                            .map(nodeAndPath -> new RouteImpl(nodeAndPath.getFullPath()
                                                                         .stream()
                                                                         .map(Node::getIdentity)
                                                                         .collect(Collectors.toList()),
                                                              this.graph))
                            .collect(Collectors.toList());
    }

    private List<NodeAndPath> determineMatchingNodes(Optional<Node> targetNode, List<NodeAndPath> currentNodes)
    {
        return currentNodes.stream()
                           .filter(nodeAndPath -> nodeAndPath.getNode()
                                                             .equals(targetNode.get()))
                           .collect(Collectors.toList());
    }

    private List<NodeAndPath> determineNextNodes(Function<Node, Stream<Node>> forwardFunction, List<NodeAndPath> currentNodes, Set<NodeIdentity> visitedNodes,
                                                 SkipNodes skipNodes, Consumer<Set<NodeAndPath>> visitedNodesHitConsumer)
    {
        Set<NodeAndPath> visitedNodesHitPaths = new HashSet<>();
        List<NodeAndPath> result = currentNodes.stream()
                                               .filter(skipNodes::matchesNot)
                                               .flatMap(nodeAndPath -> forwardFunction.apply(nodeAndPath.getNode())
                                                                                      .filter(PredicateUtils.isCollectionNotContaining(visitedNodes)
                                                                                                            .from(Node::getIdentity)
                                                                                                            .ifFalseThen(excludedNode -> visitedNodesHitPaths.add(nodeAndPath.append(excludedNode))))
                                                                                      .map(nodeAndPath::append))
                                               .filter(skipNodes::matchesNot)
                                               .collect(Collectors.toList());

        if (!visitedNodesHitPaths.isEmpty())
        {
            visitedNodesHitConsumer.accept(visitedNodesHitPaths);
        }

        return result;
    }

    private static class SkipNodes
    {
        private Set<NodeIdentity> nodeIdentities = new HashSet<>();
        private Set<NodeAndPath>  paths          = new HashSet<>();

        public boolean matchesNot(NodeAndPath nodeAndPath)
        {
            return !this.matches(nodeAndPath);
        }

        public boolean matches(NodeAndPath nodeAndPath)
        {
            boolean isMatchedByNodeIdentities = PredicateUtils.isCollectionContaining(this.nodeIdentities)
                                                              .from(Node::getIdentity)
                                                              .from(NodeAndPath::getNode)
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

        public SkipNodes add(NodeAndPath nodeAndPath)
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
        private static class RouteAndTraversalControlImpl implements RouteAndTraversalControl
        {
            private final Route         route;
            private final RoutesControl routesControl;

            private RouteAndTraversalControlImpl(Route route, RoutesControl routesControl)
            {
                this.route = route;
                this.routesControl = routesControl;
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
        }

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
                                     .map(route -> new RouteAndTraversalControlImpl(route, routesControl));
        }
    }

    private class BreadthFirstIterator implements Iterator<RoutesControl>
    {
        private final CachedElement<RoutesControl> currentRoutesControl;
        private final AtomicLong                   counter = new AtomicLong();

        private BreadthFirstIterator(Set<NodeIdentity> startNodes, Function<Node, Stream<Node>> forwardFunction, Graph graph,
                                     List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers)
        {
            List<NodeAndPath> startNodeAndPaths = Optional.ofNullable(startNodes)
                                                          .orElse(Collections.emptySet())
                                                          .stream()
                                                          .map(graph::findNodeById)
                                                          .filter(Optional::isPresent)
                                                          .map(Optional::get)
                                                          .map(NodeAndPath::of)
                                                          .collect(Collectors.toList());
            SkipNodes skipNodes = new SkipNodes();
            this.currentRoutesControl = CachedElement.of(new BreadthFirstSupplier(startNodeAndPaths, forwardFunction, this.counter, skipNodes,
                                                                                  alreadyVisitedNodesHitHandlers))
                                                     .set(new RoutesControl(new RoutesImpl(BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(startNodeAndPaths)),
                                                                            skipNodes, this.counter.incrementAndGet(), () -> this.counter.get()));
        }

        @Override
        public boolean hasNext()
        {
            return this.currentRoutesControl.get()
                                            .getRoutes()
                                            .hasRoutes();
        }

        @Override
        public RoutesControl next()
        {
            return this.currentRoutesControl.getAndReset();
        }

        private final class BreadthFirstSupplier implements Supplier<RoutesControl>
        {
            private final Function<Node, Stream<Node>>  forwardFunction;
            private final Set<NodeIdentity>             visitedNodes = new HashSet<>();
            private final SkipNodes                     skipNodes;
            private final AtomicLong                    counter;
            private final List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers;

            private List<NodeAndPath> currentNodeAndPaths;

            private BreadthFirstSupplier(List<NodeAndPath> startNodeAndPaths, Function<Node, Stream<Node>> forwardFunction, AtomicLong counter,
                                         SkipNodes skipNodes, List<TraversalRoutesConsumer> alreadyVisitedNodesHitHandlers)
            {
                this.forwardFunction = forwardFunction;
                this.currentNodeAndPaths = startNodeAndPaths;
                this.counter = counter;
                this.skipNodes = skipNodes;
                this.alreadyVisitedNodesHitHandlers = alreadyVisitedNodesHitHandlers;
            }

            @Override
            public RoutesControl get()
            {
                Set<NodeIdentity> currentNodes = BreadthFirstRoutingStrategy.this.determineNodesFrom(this.currentNodeAndPaths);
                this.visitedNodes.addAll(currentNodes);
                BreadthFirstRoutingStrategy.this.resolveUnresolvedNodesIfEnabled(currentNodes);
                this.currentNodeAndPaths = BreadthFirstRoutingStrategy.this.determineNextNodes(this.forwardFunction, this.currentNodeAndPaths,
                                                                                               this.visitedNodes, this.skipNodes,
                                                                                               this.createVisitedNodesHitConsumer());
                List<Route> routes = BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(this.currentNodeAndPaths);
                return new RoutesControl(new RoutesImpl(routes), this.skipNodes, this.counter.incrementAndGet(), () -> this.counter.get());
            }

            private Consumer<Set<NodeAndPath>> createVisitedNodesHitConsumer()
            {
                return nodeAndPaths -> this.alreadyVisitedNodesHitHandlers.forEach(handler -> handler.accept(new RoutesImpl(BreadthFirstRoutingStrategy.this.wrapMatchingNodeAndPathsIntoRoutes(nodeAndPaths))));
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
        private final Routes       routes;
        private final LongSupplier currentCounterProvider;
        private final long         snapshotCounter;
        private final SkipNodes    skipNodes;

        public RoutesControl(Routes routes, SkipNodes skipNodes, long snapshotCounter, LongSupplier currentCounterProvider)
        {
            super();
            this.routes = routes;
            this.skipNodes = skipNodes;
            this.snapshotCounter = snapshotCounter;
            this.currentCounterProvider = currentCounterProvider;
        }

        public void addToSkipNodes(NodeIdentity... nodeIdentities)
        {
            this.addToSkipNodes(Arrays.asList(nodeIdentities));
        }

        public void addRouteToSkipNodes(Route route)
        {
            if (route != null)
            {
                this.validateSkipIsStillPossible();
                BiElement<List<Node>, Optional<Node>> remainingListAndLast = ListUtils.splitLast(route.toList());
                this.skipNodes.add(new NodeAndPath(remainingListAndLast.getSecond()
                                                                       .get(),
                                                   remainingListAndLast.getFirst()));
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

    }
}
