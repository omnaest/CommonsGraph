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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.ListUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.GraphRouter.RoutingStrategy;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.router.route.RouteImpl;

public class BreadthFirstRoutingStrategy implements RoutingStrategy
{
    private Graph graph;

    public BreadthFirstRoutingStrategy(Graph graph)
    {
        this.graph = graph;
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
        Optional<Node> targetNode = this.graph.findNodeById(to);

        List<Route> routes = new ArrayList<>();
        if (startNode.isPresent() && targetNode.isPresent())
        {
            List<NodeAndPath> currentNodes = new ArrayList<>();
            currentNodes.add(NodeAndPath.of(startNode.get()));

            if (targetNode.get()
                          .equals(startNode.get()))
            {
                routes.add(new RouteImpl(Arrays.asList(startNode.get()
                                                                .getIdentity()),
                                         this.graph));
            }
            Set<NodeIdentity> visitedNodes = new HashSet<>();
            while (!currentNodes.isEmpty())
            {
                visitedNodes.addAll(currentNodes.stream()
                                                .map(NodeAndPath::getNode)
                                                .map(Node::getIdentity)
                                                .collect(Collectors.toList()));
                currentNodes = currentNodes.stream()
                                           .flatMap(nodeAndPath -> forwardFunction.apply(nodeAndPath.getNode())
                                                                                  .filter(node -> !visitedNodes.contains(node.getIdentity()))
                                                                                  .map(nextNode -> nodeAndPath.append(nextNode)))
                                           .collect(Collectors.toList());

                List<NodeAndPath> matchingNodes = currentNodes.stream()
                                                              .filter(nap -> nap.getNode()
                                                                                .equals(targetNode.get()))
                                                              .collect(Collectors.toList());
                routes.addAll(matchingNodes.stream()
                                           .map(nodeAndPath -> new RouteImpl(nodeAndPath.getFullPath()
                                                                                        .stream()
                                                                                        .map(Node::getIdentity)
                                                                                        .collect(Collectors.toList()),
                                                                             this.graph))
                                           .collect(Collectors.toList()));
            }

        }

        return new Routes()
        {
            @Override
            public Stream<Route> stream()
            {
                return routes.stream();
            }

            @Override
            public int size()
            {
                return routes.size();
            }

            @Override
            public Optional<Route> first()
            {
                return ListUtils.optionalFirst(routes);
            }
        };
    }
}
