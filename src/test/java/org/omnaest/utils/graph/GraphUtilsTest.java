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
package org.omnaest.utils.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Test;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.element.bi.UnaryBiElement;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.domain.traversal.Direction;
import org.omnaest.utils.graph.domain.traversal.Route;
import org.omnaest.utils.graph.domain.traversal.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.traversal.Routes;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphLayer;
import org.omnaest.utils.graph.domain.traversal.Traversal.GraphSteppedView;
import org.omnaest.utils.graph.domain.traversal.Traversal.VisitedNodesStatistic;
import org.omnaest.utils.graph.domain.traversal.TraversalRoutes;
import org.omnaest.utils.graph.domain.traversal.TraversedEdge;
import org.omnaest.utils.graph.domain.traversal.hierarchy.HierarchicalNode;
import org.omnaest.utils.graph.domain.traversal.hierarchy.Hierarchy;
import org.omnaest.utils.supplier.SupplierConsumer;

/**
 * @see GraphUtils
 * @author omnaest
 */
public class GraphUtilsTest
{
    @Test
    public void testBuilder() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity childNode = NodeIdentity.of("1.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, childNode)
                                .build();

        assertTrue(graph.isNotEmpty());
        assertFalse(graph.isEmpty());
        assertEquals(SetUtils.toSet("1", "1.1"), graph.stream()
                                                      .map(Node::getIdentity)
                                                      .map(NodeIdentity::getPrimaryId)
                                                      .collect(Collectors.toSet()));

        assertTrue(graph.findNodeById(rootNode)
                        .isPresent());
        assertTrue(graph.containsAny(childNode));
        assertFalse(graph.findNodeById(NodeIdentity.of("non existing node id"))
                         .isPresent());
        assertEquals("1", graph.findNodeById(rootNode)
                               .get()
                               .getIdentity()
                               .getPrimaryId());

        assertEquals("1.1", graph.findNodeById(rootNode)
                                 .get()
                                 .getOutgoingNodes()
                                 .findById(childNode)
                                 .get()
                                 .getIdentity()
                                 .getPrimaryId());
        assertEquals("1", graph.findNodeById(childNode)
                               .get()
                               .getIncomingNodes()
                               .findById(rootNode)
                               .get()
                               .getIdentity()
                               .getPrimaryId());
        assertEquals(SetUtils.toSet(rootNode, childNode), graph.nodes()
                                                               .stream()
                                                               .map(Node::getIdentity)
                                                               .collect(Collectors.toSet()));
    }

    @Test
    public void testBuildByElements() throws Exception
    {
        Map<String, List<String>> parentToChildren = MapUtils.builder()
                                                             .put("root1", Arrays.asList("intermediate1", "intermediate2"))
                                                             .put("intermediate1", Arrays.asList("child1.1", "child1.2"))
                                                             .put("intermediate2", Arrays.asList("child2.1", "child2.2"))
                                                             .build();

        Graph graph = GraphUtils.builder()
                                .addElementsWithChildren(Arrays.asList("root1"), parent -> parentToChildren.get(parent), element -> NodeIdentity.of(element))
                                .build();

        assertEquals(SetUtils.toSet(NodeIdentity.of("root1"), NodeIdentity.of("intermediate1"), NodeIdentity.of("intermediate2"), NodeIdentity.of("child1.1"),
                                    NodeIdentity.of("child1.2"), NodeIdentity.of("child2.1"), NodeIdentity.of("child2.2")),
                     graph.nodes()
                          .identities());
        assertEquals(SetUtils.toSet(NodeIdentity.of("intermediate1"), NodeIdentity.of("intermediate2")), graph.routing()
                                                                                                              .withBreadthFirst()
                                                                                                              .traverse()
                                                                                                              .outgoing()
                                                                                                              .level(1)
                                                                                                              .identities()
                                                                                                              .collect(Collectors.toSet()));
    }

    @Test
    public void testEmptyGraph() throws Exception
    {
        Graph graph = GraphUtils.builder()
                                .build();

        assertFalse(graph.isNotEmpty());
        assertTrue(graph.isEmpty());
    }

    @Test
    public void testAttributes() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity childNode = NodeIdentity.of("1.1");
        Graph graph = GraphUtils.builder()
                                .addEdgeWithAttributes(rootNode, childNode, Arrays.asList(Tag.of("tag1"), Attribute.of("attribute1", "value1")))
                                .build();

        assertEquals(SetUtils.toSet("1", "1.1"), graph.stream()
                                                      .map(Node::getIdentity)
                                                      .map(NodeIdentity::getPrimaryId)
                                                      .collect(Collectors.toSet()));
        assertEquals(SetUtils.toSet(Tag.of("tag1"), Attribute.of("attribute1", "value1")), graph.findEdge(rootNode, childNode)
                                                                                                .get()
                                                                                                .getAttributes());
        assertTrue(graph.findEdge(rootNode, childNode)
                        .get()
                        .hasTag(Tag.of("tag1")));
        assertFalse(graph.findEdge(rootNode, childNode)
                         .get()
                         .hasTag(Tag.of("tagNonExisting")));
        assertEquals(childNode, graph.findNodeById(rootNode)
                                     .map(node -> node.findAllEdgesWithTag(Tag.of("tag1")))
                                     .flatMap(edges -> edges.last())
                                     .get()
                                     .getTo()
                                     .getIdentity());
        assertEquals(1, graph.edges()
                             .size());
        assertEquals(rootNode, graph.edges()
                                    .first()
                                    .get()
                                    .getFrom()
                                    .getIdentity());
        assertEquals(childNode, graph.edges()
                                     .first()
                                     .get()
                                     .getTo()
                                     .getIdentity());
    }

    @Test
    public void testNodeData() throws Exception
    {
        NodeIdentity childNode = NodeIdentity.of("1.1");
        Graph graph = GraphUtils.builder()
                                .addNodeWithData(childNode, data -> data.put("fieldInteger", 123)
                                                                        .put("fieldString", "value"))
                                .build();

        assertEquals(123, graph.findNodeById(childNode)
                               .get()
                               .getData()
                               .getValue("fieldInteger")
                               .get()
                               .getAsInteger());
        assertEquals("value", graph.findNodeById(childNode)
                                   .get()
                                   .getData()
                                   .getValue("fieldString")
                                   .get()
                                   .getAsString());
        assertEquals(false, graph.findNodeById(childNode)
                                 .get()
                                 .getData()
                                 .getValue("fieldNonExisting")
                                 .isPresent());
    }

    @Test
    public void testRouter() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.1+2.3");
        Tag exludingTag = Tag.of("tag");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode1, childNode3)
                                .addEdgeWithAttributes(intermediateNode2, childNode3, exludingTag)
                                .build();

        assertEquals(6, graph.size());

        GraphRouter router = graph.routing();

        {
            Routes routes = router.withBreadthFirst()
                                  .findAllOutgoingRoutesBetween(rootNode, childNode1);
            assertEquals(1, routes.size());
            assertEquals(Arrays.asList("1", "1.1", "1.1.1"), routes.first()
                                                                   .get()
                                                                   .toNodeIdentities()
                                                                   .stream()
                                                                   .map(NodeIdentity::getPrimaryId)
                                                                   .collect(Collectors.toList()));
            assertEquals(Arrays.asList("1", "1.1"), routes.first()
                                                          .get()
                                                          .getSubRouteUntilLastNth(1)
                                                          .stream()
                                                          .map(Node::getIdentity)
                                                          .map(NodeIdentity::getPrimaryId)
                                                          .collect(Collectors.toList()));
        }
        {
            Routes routes = router.withBreadthFirst()
                                  .findAllIncomingRoutesBetween(childNode2, rootNode);
            assertEquals(1, routes.size());
            assertEquals(Arrays.asList("1.1.2", "1.1", "1"), routes.first()
                                                                   .get()
                                                                   .toNodeIdentities()
                                                                   .stream()
                                                                   .map(NodeIdentity::getPrimaryId)
                                                                   .collect(Collectors.toList()));
        }
        {
            Routes routes = router.withBreadthFirst()
                                  .withExcludingEdgeByTagFilter(exludingTag)
                                  .findAllOutgoingRoutesBetween(rootNode, childNode3);
            assertEquals(1, routes.size());
            assertEquals(Arrays.asList("1", "1.1", "1.1+2.3"), routes.first()
                                                                     .get()
                                                                     .toNodeIdentities()
                                                                     .stream()
                                                                     .map(NodeIdentity::getPrimaryId)
                                                                     .collect(Collectors.toList()));
        }
    }

    @Test
    public void testRouterWithNodeResolver() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        Graph graph = GraphUtils.builder()
                                .addNode(rootNode)
                                .withSingleNodeResolver(node ->
                                {
                                    if (rootNode.equals(node))
                                    {
                                        return Arrays.asList(intermediateNode1, intermediateNode2)
                                                     .stream()
                                                     .map(intermediateNode -> EdgeIdentity.of(rootNode, intermediateNode))
                                                     .collect(Collectors.toSet());
                                    }
                                    else if (intermediateNode1.equals(node))
                                    {
                                        return Arrays.asList(childNode1, childNode2)
                                                     .stream()
                                                     .map(childNode -> EdgeIdentity.of(intermediateNode1, childNode))
                                                     .collect(Collectors.toSet());
                                    }
                                    else
                                    {
                                        return Collections.emptySet();
                                    }
                                })
                                .build();

        assertEquals(1, graph.size());

        GraphRouter router = graph.routing();

        {
            Routes routes = router.withBreadthFirst()
                                  .findAllOutgoingRoutesBetween(rootNode, childNode1);
            assertEquals(1, routes.size());
            assertEquals(Arrays.asList("1", "1.1", "1.1.1"), routes.first()
                                                                   .get()
                                                                   .toNodeIdentities()
                                                                   .stream()
                                                                   .map(NodeIdentity::getPrimaryId)
                                                                   .collect(Collectors.toList()));
            assertEquals(5, graph.size());
        }
        {
            Routes routes = router.withBreadthFirst()
                                  .findAllIncomingRoutesBetween(childNode2, rootNode);
            assertEquals(1, routes.size());
            assertEquals(Arrays.asList("1.1.2", "1.1", "1"), routes.first()
                                                                   .get()
                                                                   .toNodeIdentities()
                                                                   .stream()
                                                                   .map(NodeIdentity::getPrimaryId)
                                                                   .collect(Collectors.toList()));
        }
    }

    @Test
    public void testTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .build();

        assertEquals(Arrays.asList(rootNode, intermediateNode1, intermediateNode2, childNode1, childNode2), graph.routing()
                                                                                                                 .withBreadthFirst()
                                                                                                                 .traverseOutgoing()
                                                                                                                 .stream()
                                                                                                                 .flatMap(TraversalRoutes::stream)
                                                                                                                 .peek(control -> control.skipIf(control.get()
                                                                                                                                                        .last()
                                                                                                                                                        .get()
                                                                                                                                                        .getIdentity()
                                                                                                                                                        .equals(intermediateNode2)))
                                                                                                                 .peek(control -> control.skipNextRouteNodes(intermediateNode3))
                                                                                                                 .map(RouteAndTraversalControl::get)
                                                                                                                 .peek(route -> assertFalse(route.isCyclic()))
                                                                                                                 .map(Route::last)
                                                                                                                 .map(Optional::get)
                                                                                                                 .map(Node::getIdentity)
                                                                                                                 .collect(Collectors.toList()));
        assertEquals(Arrays.asList(UnaryBiElement.of(intermediateNode1, childNode1), UnaryBiElement.of(rootNode, intermediateNode1)), graph.routing()
                                                                                                                                           .withBreadthFirst()
                                                                                                                                           .traverseIncoming(childNode1)
                                                                                                                                           .routes()
                                                                                                                                           .map(Route::lastEdge)
                                                                                                                                           .filter(PredicateUtils.filterNonEmptyOptional())
                                                                                                                                           .map(MapperUtils.mapOptionalToValue())
                                                                                                                                           .map(TraversedEdge::getEdge)
                                                                                                                                           .map(Edge::getNodeIdentities)
                                                                                                                                           .collect(Collectors.toList()));
        assertEquals(Arrays.asList(UnaryBiElement.of(intermediateNode1, childNode1), UnaryBiElement.of(intermediateNode1, childNode1)), graph.routing()
                                                                                                                                             .withBreadthFirst()
                                                                                                                                             .traverseIncoming(childNode1)
                                                                                                                                             .routes()
                                                                                                                                             .map(Route::firstEdge)
                                                                                                                                             .filter(PredicateUtils.filterNonEmptyOptional())
                                                                                                                                             .map(MapperUtils.mapOptionalToValue())
                                                                                                                                             .map(TraversedEdge::getEdge)
                                                                                                                                             .map(Edge::getNodeIdentities)
                                                                                                                                             .collect(Collectors.toList()));
        assertEquals(SetUtils.toSet(UnaryBiElement.of(rootNode, intermediateNode1), UnaryBiElement.of(rootNode, intermediateNode2),
                                    UnaryBiElement.of(rootNode, intermediateNode3), UnaryBiElement.of(intermediateNode1, childNode1),
                                    UnaryBiElement.of(intermediateNode1, childNode2), UnaryBiElement.of(intermediateNode2, childNode3)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing(rootNode)
                          .routes()
                          .limit(7)
                          .map(Route::lastEdge)
                          .filter(PredicateUtils.filterNonEmptyOptional())
                          .map(MapperUtils.mapOptionalToValue())
                          .map(TraversedEdge::getEdge)
                          .map(Edge::getNodeIdentities)
                          .collect(Collectors.toSet()));
        assertEquals(SetUtils.toSet(UnaryBiElement.of(rootNode, intermediateNode1), UnaryBiElement.of(rootNode, intermediateNode2),
                                    UnaryBiElement.of(rootNode, intermediateNode3)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing(rootNode)
                          .routes()
                          .limit(7)
                          .map(Route::firstEdge)
                          .filter(PredicateUtils.filterNonEmptyOptional())
                          .map(MapperUtils.mapOptionalToValue())
                          .map(TraversedEdge::getEdge)
                          .map(Edge::getNodeIdentities)
                          .collect(Collectors.toSet()));
    }

    @Test
    public void testTraversalStepLimit() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .build();

        assertEquals(Arrays.asList(rootNode), graph.routing()
                                                   .withBreadthFirst()
                                                   .traverseOutgoing()
                                                   .limitStepsTo(1)
                                                   .nodes()
                                                   .map(Node::getIdentity)
                                                   .collect(Collectors.toList()));
        assertEquals(Arrays.asList(rootNode, intermediateNode1, intermediateNode2, intermediateNode3), graph.routing()
                                                                                                            .withBreadthFirst()
                                                                                                            .traverseOutgoing()
                                                                                                            .limitStepsTo(2)
                                                                                                            .nodes()
                                                                                                            .map(Node::getIdentity)
                                                                                                            .collect(Collectors.toList()));
    }

    @Test
    public void testTraversalLevel() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .build();

        assertEquals(Arrays.asList(rootNode), graph.routing()
                                                   .withBreadthFirst()
                                                   .traverse()
                                                   .outgoing()
                                                   .level(0)
                                                   .nodes()
                                                   .map(Node::getIdentity)
                                                   .collect(Collectors.toList()));
        assertEquals(Arrays.asList(intermediateNode1, intermediateNode2, intermediateNode3), graph.routing()
                                                                                                  .withBreadthFirst()
                                                                                                  .traverse()
                                                                                                  .outgoing()
                                                                                                  .level(1)
                                                                                                  .identities()
                                                                                                  .collect(Collectors.toList()));

        assertEquals(Arrays.asList(childNode1, childNode2, childNode3), graph.routing()
                                                                             .withBreadthFirst()
                                                                             .traverse()
                                                                             .outgoing()
                                                                             .level(graph.routing()
                                                                                         .withBreadthFirst()
                                                                                         .traverse()
                                                                                         .outgoing()
                                                                                         .deepness())
                                                                             .identities()
                                                                             .collect(Collectors.toList()));
        assertEquals(2, graph.routing()
                             .withBreadthFirst()
                             .traverse()
                             .outgoing()
                             .deepness());
    }

    @Test
    public void testDiamondTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode = NodeIdentity.of("1.1/2/3.1");

        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode)
                                .addEdge(intermediateNode2, childNode)
                                .addEdge(intermediateNode3, childNode)
                                .build();

        assertEquals(Arrays.asList(Arrays.asList(rootNode), Arrays.asList(rootNode, intermediateNode1), Arrays.asList(rootNode, intermediateNode2),
                                   Arrays.asList(rootNode, intermediateNode3), Arrays.asList(rootNode, intermediateNode1, childNode)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing(rootNode)
                          .routes()
                          .peek(route -> assertFalse(route.isCyclic()))
                          .map(Route::toNodeIdentities)
                          .collect(Collectors.toList()));

        assertEquals(Arrays.asList(Arrays.asList(childNode), Arrays.asList(childNode, intermediateNode1), Arrays.asList(childNode, intermediateNode2),
                                   Arrays.asList(childNode, intermediateNode3), Arrays.asList(childNode, intermediateNode1, rootNode)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseIncoming(childNode)
                          .routes()
                          .peek(route -> assertFalse(route.isCyclic()))
                          .map(Route::toNodeIdentities)
                          .collect(Collectors.toList()));

    }

    @Test
    public void testDiamondTraversalIncludingFirstAlreadyVisitedNode() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode = NodeIdentity.of("1.1/2/3.1");

        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode)
                                .addEdge(intermediateNode2, childNode)
                                .addEdge(intermediateNode3, childNode)
                                .build();

        assertEquals(Arrays.asList(Arrays.asList(rootNode), Arrays.asList(rootNode, intermediateNode1), Arrays.asList(rootNode, intermediateNode2),
                                   Arrays.asList(rootNode, intermediateNode3), Arrays.asList(rootNode, intermediateNode1, childNode),
                                   Arrays.asList(rootNode, intermediateNode2, childNode), Arrays.asList(rootNode, intermediateNode3, childNode)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing(rootNode)
                          .includingFirstRouteOfAlreadyVisitedNodes()
                          .routes()
                          .peek(route -> assertFalse(route.isCyclic()))
                          .map(Route::toNodeIdentities)
                          .collect(Collectors.toList()));

        assertEquals(Arrays.asList(Arrays.asList(childNode), Arrays.asList(childNode, intermediateNode1), Arrays.asList(childNode, intermediateNode2),
                                   Arrays.asList(childNode, intermediateNode3), Arrays.asList(childNode, intermediateNode1, rootNode),
                                   Arrays.asList(childNode, intermediateNode2, rootNode), Arrays.asList(childNode, intermediateNode3, rootNode)),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseIncoming(childNode)
                          .includingFirstRouteOfAlreadyVisitedNodes()
                          .routes()
                          .peek(route -> assertFalse(route.isCyclic()))
                          .map(Route::toNodeIdentities)
                          .collect(Collectors.toList()));
    }

    @Test
    public void testWeightedTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity intermediateNode4 = NodeIdentity.of("1.4");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity childNode4 = NodeIdentity.of("1.3.1");
        NodeIdentity childNode5 = NodeIdentity.of("1.3.2");
        NodeIdentity childNode6 = NodeIdentity.of("1.3.3");
        NodeIdentity childNode7 = NodeIdentity.of("1.3.4");
        NodeIdentity childNode8 = NodeIdentity.of("1.3.5");
        NodeIdentity leafNode1 = NodeIdentity.of("1.1.1.1");
        NodeIdentity leafNode2 = NodeIdentity.of("1.1.2.1");
        NodeIdentity leafNode3 = NodeIdentity.of("1.2.1.1");
        NodeIdentity leafNode4 = NodeIdentity.of("1.3.1.1");
        NodeIdentity leafNode5 = NodeIdentity.of("1.3.2.1");
        NodeIdentity leafNode6 = NodeIdentity.of("1.3.3.1");
        NodeIdentity leafNode7 = NodeIdentity.of("1.3.4.1");
        NodeIdentity leafNode8 = NodeIdentity.of("1.3.5.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1) // 0.25
                                .addEdge(rootNode, intermediateNode2) // 0.25
                                .addEdge(rootNode, intermediateNode3) // 0.25
                                .addEdge(rootNode, intermediateNode4) // 0.25
                                .addEdge(intermediateNode1, childNode1) // 0.125
                                .addEdge(intermediateNode1, childNode2) // 0.125
                                .addEdge(intermediateNode2, childNode3) // 0.25
                                .addEdge(intermediateNode3, childNode4) // 0.05
                                .addEdge(intermediateNode3, childNode5) // 0.05
                                .addEdge(intermediateNode3, childNode6) // 0.05
                                .addEdge(intermediateNode3, childNode7) // 0.05
                                .addEdge(intermediateNode3, childNode8) // 0.05
                                .addEdge(childNode1, leafNode1)
                                .addEdge(childNode2, leafNode2)
                                .addEdge(childNode3, leafNode3)
                                .addEdge(childNode4, leafNode4) // cut off
                                .addEdge(childNode5, leafNode5) // cut off
                                .addEdge(childNode6, leafNode6) // cut off
                                .addEdge(childNode7, leafNode7) // cut off
                                .addEdge(childNode8, leafNode8) // cut off
                                .build();

        assertEquals(Arrays.asList(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4, childNode1, childNode2, childNode3,
                                   childNode4, childNode5, childNode6, childNode7, childNode8, leafNode1, leafNode2, leafNode3)
                           .stream()
                           .collect(Collectors.toSet()),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing()
                          .withWeightedPathTerminationByBranches(0.1)
                          .stream()
                          .flatMap(TraversalRoutes::stream)
                          .map(RouteAndTraversalControl::get)
                          .map(Route::last)
                          .map(Optional::get)
                          .map(Node::getIdentity)
                          .collect(Collectors.toSet()));
    }

    @Test
    public void testBudgetTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity intermediateNode4 = NodeIdentity.of("1.4");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity childNode4 = NodeIdentity.of("1.3.1");
        NodeIdentity childNode5 = NodeIdentity.of("1.3.2");
        NodeIdentity childNode6 = NodeIdentity.of("1.3.3");
        NodeIdentity childNode7 = NodeIdentity.of("1.3.4");
        NodeIdentity childNode8 = NodeIdentity.of("1.3.5");
        NodeIdentity leafNode1 = NodeIdentity.of("1.1.1.1");
        NodeIdentity leafNode2 = NodeIdentity.of("1.1.2.1");
        NodeIdentity leafNode3 = NodeIdentity.of("1.2.1.1");
        NodeIdentity leafNode4 = NodeIdentity.of("1.3.1.1");
        NodeIdentity leafNode5 = NodeIdentity.of("1.3.2.1");
        NodeIdentity leafNode6 = NodeIdentity.of("1.3.3.1");
        NodeIdentity leafNode7 = NodeIdentity.of("1.3.4.1");
        NodeIdentity leafNode8 = NodeIdentity.of("1.3.5.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1) // 0.25
                                .addEdge(rootNode, intermediateNode2) // 0.25
                                .addEdge(rootNode, intermediateNode3) // 0.25
                                .addEdge(rootNode, intermediateNode4) // 0.25
                                .addEdge(intermediateNode1, childNode1) // 0.125
                                .addEdge(intermediateNode1, childNode2) // 0.125
                                .addEdge(intermediateNode2, childNode3) // 0.25
                                .addEdge(intermediateNode3, childNode4) // 0.05
                                .addEdge(intermediateNode3, childNode5) // 0.05
                                .addEdge(intermediateNode3, childNode6) // 0.05
                                .addEdge(intermediateNode3, childNode7) // 0.05
                                .addEdge(intermediateNode3, childNode8) // 0.05
                                .addEdge(childNode1, leafNode1) // 0.125
                                .addEdge(childNode2, leafNode2) // 0.125
                                .addEdge(childNode3, leafNode3) // 0.25
                                .addEdge(childNode4, leafNode4) // 0.05
                                .addEdge(childNode5, leafNode5) // 0.05
                                .addEdge(childNode6, leafNode6) // 0.05
                                .addEdge(childNode7, leafNode7) // 0.05
                                .addEdge(childNode8, leafNode8) // 0.05
                                .build();

        assertEquals(Arrays.asList(SetUtils.toSet(rootNode), SetUtils.toSet(intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4),
                                   SetUtils.toSet(childNode1, childNode2, childNode3), SetUtils.toSet(leafNode1, leafNode2, leafNode3),
                                   SetUtils.toSet(childNode4, childNode5, childNode6, childNode7, childNode8),
                                   SetUtils.toSet(leafNode4, leafNode5, leafNode6, leafNode7, leafNode8))
                           .stream()
                           .collect(Collectors.toList()),
                     graph.routing()
                          .withBreadthFirst()
                          .budgetOptimized()
                          .traverseOutgoing()
                          .stream()
                          .map(routes -> routes.stream()
                                               .map(RouteAndTraversalControl::get)
                                               .map(Route::last)
                                               .map(Optional::get)
                                               .map(Node::getIdentity)
                                               .collect(Collectors.toSet()))
                          .collect(Collectors.toList()));
    }

    @Test
    public void testSecondaryTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity secondaryRootNode = NodeIdentity.of("2");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity secondaryIntermediateNode1 = NodeIdentity.of("2.1");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1 + 2.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .addEdge(secondaryIntermediateNode1, childNode1)
                                .addEdge(secondaryRootNode, secondaryIntermediateNode1)
                                .build();

        assertEquals(Arrays.asList(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, childNode1, childNode2, childNode3,
                                   secondaryIntermediateNode1, secondaryRootNode),
                     graph.routing()
                          .withBreadthFirst()
                          .traverseOutgoing(rootNode)
                          .andAfterwardsTraverseIncoming()
                          .stream()
                          .flatMap(TraversalRoutes::stream)
                          .map(RouteAndTraversalControl::get)
                          .peek(route -> assertFalse(route.isCyclic()))
                          .map(Route::last)
                          .map(Optional::get)
                          .map(Node::getIdentity)
                          .collect(Collectors.toList()));
    }

    @Test
    public void testSecondaryMultiDirectionTraversal() throws Exception
    {
        NodeIdentity rootNode_1 = NodeIdentity.of("1");
        NodeIdentity rootNode_2 = NodeIdentity.of("2");
        NodeIdentity intermediateNode_1_1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode_2_1 = NodeIdentity.of("2.1");
        NodeIdentity childNode_1_1_1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode_1_1_2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode_1_2_1__2_1_1 = NodeIdentity.of("1.2.1/2.1.1");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode_1, intermediateNode_1_1)
                                .addEdge(intermediateNode_1_1, childNode_1_1_1)
                                .addEdge(intermediateNode_1_1, childNode_1_1_2)
                                .addEdge(intermediateNode_1_1, childNode_1_2_1__2_1_1)
                                .addEdge(rootNode_2, intermediateNode_2_1)
                                .addEdge(intermediateNode_2_1, childNode_1_2_1__2_1_1)
                                .addNode(detachedNode)
                                .build();

        List<Route> routes = graph.routing()
                                  .withBreadthFirst()
                                  .traverseOutgoing(rootNode_1)
                                  .andTraverseIncoming(childNode_1_2_1__2_1_1)
                                  .routes()
                                  .collect(Collectors.toList());
        assertEquals(7, routes.size());
        assertEquals(SetUtils.toSet(rootNode_1, rootNode_2, intermediateNode_1_1, intermediateNode_2_1, childNode_1_1_1, childNode_1_1_2,
                                    childNode_1_2_1__2_1_1),
                     routes.stream()
                           .flatMap(Route::stream)
                           .map(Node::getIdentity)
                           .collect(Collectors.toSet()));
        assertEquals(Arrays.asList(rootNode_1), routes.get(0)
                                                      .toNodeIdentities());
        assertEquals(Arrays.asList(childNode_1_2_1__2_1_1), routes.get(1)
                                                                  .toNodeIdentities());
        assertEquals(Arrays.asList(rootNode_1, intermediateNode_1_1), routes.get(2)
                                                                            .toNodeIdentities());
        assertEquals(Arrays.asList(childNode_1_2_1__2_1_1, intermediateNode_2_1), routes.get(3)
                                                                                        .toNodeIdentities());
        assertEquals(Arrays.asList(rootNode_1, intermediateNode_1_1, childNode_1_1_1), routes.get(4)
                                                                                             .toNodeIdentities());
        assertEquals(Arrays.asList(rootNode_1, intermediateNode_1_1, childNode_1_1_2), routes.get(5)
                                                                                             .toNodeIdentities());
        assertEquals(Arrays.asList(childNode_1_2_1__2_1_1, intermediateNode_2_1, rootNode_2), routes.get(6)
                                                                                                    .toNodeIdentities());
    }

    @Test
    public void testTraversalOfCyclicGraph() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode = NodeIdentity.of("1.1");
        NodeIdentity childNode = NodeIdentity.of("1.1.1");

        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode)
                                .addEdge(intermediateNode, childNode)
                                .addEdge(childNode, rootNode) // cycle
                                .build();

        AtomicReference<Route> cycleHitRoute = new AtomicReference<>();
        assertEquals(Arrays.asList(rootNode, intermediateNode, childNode), graph.routing()
                                                                                .withBreadthFirst()
                                                                                .traverseOutgoing(rootNode)
                                                                                .withAlreadyVisitedNodesHitHandler(routes -> cycleHitRoute.set(routes.first()
                                                                                                                                                     .get()))
                                                                                .stream()
                                                                                .flatMap(TraversalRoutes::stream)
                                                                                .map(RouteAndTraversalControl::get)
                                                                                .map(Route::last)
                                                                                .map(Optional::get)
                                                                                .map(Node::getIdentity)
                                                                                .collect(Collectors.toList()));
        assertEquals(rootNode, cycleHitRoute.get()
                                            .last()
                                            .get()
                                            .getIdentity());
        assertTrue(cycleHitRoute.get()
                                .isCyclic());
    }

    @Test
    public void testTraversalWithEdgeFilter() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Tag nonTraversalEdge = Tag.of("non traversal");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdgeWithAttributes(rootNode, intermediateNode1, nonTraversalEdge)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .build();

        assertEquals(Arrays.asList(rootNode, intermediateNode2, childNode3), graph.routing()
                                                                                  .withBreadthFirst()
                                                                                  .traverseOutgoing()
                                                                                  .withExcludingEdgeByTagFilter(nonTraversalEdge)
                                                                                  .nodes()
                                                                                  .map(Node::getIdentity)
                                                                                  .collect(Collectors.toList()));
    }

    @Test
    public void testClone()
    {
        try
        {
            NodeIdentity rootNode = NodeIdentity.of("1");
            NodeIdentity childNode = NodeIdentity.of("1.1");
            Graph graph = GraphUtils.builder()
                                    .addEdge(rootNode, childNode)
                                    .build()
                                    .clone();

            assertEquals(SetUtils.toSet("1", "1.1"), graph.stream()
                                                          .map(Node::getIdentity)
                                                          .map(NodeIdentity::getPrimaryId)
                                                          .collect(Collectors.toSet()));

            assertTrue(graph.findNodeById(rootNode)
                            .isPresent());
            assertFalse(graph.findNodeById(NodeIdentity.of("non existing node id"))
                             .isPresent());
            assertEquals("1", graph.findNodeById(rootNode)
                                   .get()
                                   .getIdentity()
                                   .getPrimaryId());

            assertEquals("1.1", graph.findNodeById(rootNode)
                                     .get()
                                     .getOutgoingNodes()
                                     .findById(childNode)
                                     .get()
                                     .getIdentity()
                                     .getPrimaryId());
            assertEquals("1", graph.findNodeById(childNode)
                                   .get()
                                   .getIncomingNodes()
                                   .findById(rootNode)
                                   .get()
                                   .getIdentity()
                                   .getPrimaryId());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testSerialization()
    {
        try
        {
            NodeIdentity rootNode = NodeIdentity.of("1");
            NodeIdentity childNode = NodeIdentity.of("1.1");
            String json = GraphUtils.builder()
                                    .addEdge(rootNode, childNode)
                                    .build()
                                    .serialize()
                                    .toJson();
            Graph graph = GraphUtils.deserialize()
                                    .fromJson(json);

            assertEquals(SetUtils.toSet("1", "1.1"), graph.stream()
                                                          .map(Node::getIdentity)
                                                          .map(NodeIdentity::getPrimaryId)
                                                          .collect(Collectors.toSet()));

            assertTrue(graph.findNodeById(rootNode)
                            .isPresent());
            assertFalse(graph.findNodeById(NodeIdentity.of("non existing node id"))
                             .isPresent());
            assertEquals("1", graph.findNodeById(rootNode)
                                   .get()
                                   .getIdentity()
                                   .getPrimaryId());

            assertEquals("1.1", graph.findNodeById(rootNode)
                                     .get()
                                     .getOutgoingNodes()
                                     .findById(childNode)
                                     .get()
                                     .getIdentity()
                                     .getPrimaryId());
            assertEquals("1", graph.findNodeById(childNode)
                                   .get()
                                   .getIncomingNodes()
                                   .findById(rootNode)
                                   .get()
                                   .getIdentity()
                                   .getPrimaryId());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testSubGraph() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .build();
        assertEquals(Arrays.asList(rootNode, intermediateNode1, intermediateNode3, childNode1, childNode2, childNode3)
                           .stream()
                           .collect(Collectors.toSet()),
                     graph.subGraph()
                          .withExcludedNodes(intermediateNode2)
                          .build()
                          .stream()
                          .map(Node::getIdentity)
                          .collect(Collectors.toSet()));
        assertEquals(Arrays.asList(intermediateNode2, intermediateNode3, childNode1)
                           .stream()
                           .collect(Collectors.toSet()),
                     graph.subGraph()
                          .withIncludedNodes(intermediateNode2, intermediateNode3, childNode1)
                          .build()
                          .stream()
                          .map(Node::getIdentity)
                          .collect(Collectors.toSet()));

    }

    @Test
    public void testNewCachedGraph() throws Exception
    {
        SupplierConsumer<String> cache = this.createCache();
        Graph graph = GraphUtils.newCachedGraph(cache, this.createGraphPreparator());
        Graph graph2 = GraphUtils.newCachedGraph(cache, builder ->
        {
            fail("Builder block should not be called again");
        });
        assertEquals(graph, graph2);
    }

    @Test
    public void testHierarchy()
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity secondaryIntermediateNode1 = NodeIdentity.of("2.1");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1/2.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, secondaryIntermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(secondaryIntermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .addEdge(childNode3, rootNode) // should be ignored as this is circular and can not be projected onto a hierarchy
                                .build();

        Hierarchy hierarchy = graph.routing()
                                   .withBreadthFirst()
                                   .traverseOutgoing(rootNode)
                                   .asHierarchy();

        assertEquals(1, hierarchy.stream()
                                 .count());
        assertEquals(rootNode, hierarchy.stream()
                                        .findFirst()
                                        .get()
                                        .get()
                                        .getIdentity());
        assertEquals(SetUtils.toSet(intermediateNode1, secondaryIntermediateNode1, intermediateNode2, intermediateNode3), hierarchy.stream()
                                                                                                                                   .findFirst()
                                                                                                                                   .get()
                                                                                                                                   .getChildren()
                                                                                                                                   .map(HierarchicalNode::get)
                                                                                                                                   .map(Node::getIdentity)
                                                                                                                                   .collect(Collectors.toSet()));
        assertEquals(SetUtils.toSet(childNode1, childNode2, childNode3), hierarchy.stream()
                                                                                  .findFirst()
                                                                                  .get()
                                                                                  .getChildren()
                                                                                  .flatMap(HierarchicalNode::getChildren)
                                                                                  .map(HierarchicalNode::get)
                                                                                  .map(Node::getIdentity)
                                                                                  .collect(Collectors.toSet()));

        String json = hierarchy.asJsonWithData((node, data) -> data.put("id", node.get()
                                                                                  .getIdentity()));
        assertNotNull(json);

        assertEquals(Arrays.asList("0 {\"nodeIdentity\":[\"1\"]}", "1 {\"nodeIdentity\":[\"1.1\"]}", "2 {\"nodeIdentity\":[\"1.1.1/2.1.1\"]}",
                                   "2 {\"nodeIdentity\":[\"1.1.2\"]}", "1 {\"nodeIdentity\":[\"2.1\"]}", "2 {\"nodeIdentity\":[\"1.1.1/2.1.1\"]}",
                                   "1 {\"nodeIdentity\":[\"1.2\"]}", "2 {\"nodeIdentity\":[\"1.2.1\"]}", "1 {\"nodeIdentity\":[\"1.3\"]}")
                           .stream()
                           .collect(Collectors.joining("\n")),
                     hierarchy.asColumnizedNodes()
                              .map(columnNode -> columnNode.getColumnIndex() + " " + columnNode.asJsonWithData(ConsumerUtils.noOperation()))
                              .collect(Collectors.joining("\n")));

        //        System.out.println(json);
    }

    @Test
    public void testGraphIndex()
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2", "AbC");
        NodeIdentity childNode3 = NodeIdentity.of("1.1.3", "abc");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode1, childNode3)
                                .build();

        assertEquals(Arrays.asList(intermediateNode1), graph.index()
                                                            .forAllNodes()
                                                            .withNodeIdentitiesTokenMapper()
                                                            .get()
                                                            .apply("1.1")
                                                            .map(Node::getIdentity)
                                                            .collect(Collectors.toList()));
        assertEquals(Arrays.asList(childNode3), graph.index()
                                                     .forAllNodes()
                                                     .withNodeIdentitiesTokenMapper()
                                                     .get()
                                                     .apply("abc")
                                                     .map(Node::getIdentity)
                                                     .collect(Collectors.toList()));
        assertEquals(SetUtils.toSet(childNode2, childNode3), graph.index()
                                                                  .forAllNodes()
                                                                  .withNodeIdentitiesTokenMapperIgnoringCase()
                                                                  .get()
                                                                  .apply("aBc")
                                                                  .map(Node::getIdentity)
                                                                  .collect(Collectors.toSet()));
    }

    @Test
    public void testTracingGraph() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .addNode(detachedNode)
                                .build();

        AtomicInteger graphStepListenerInvocations = new AtomicInteger();
        List<TraversalRoutes> routes = graph.routing()
                                            .withBreadthFirst()
                                            .traverseOutgoing()
                                            .withTracingGraph()
                                            .withTracingGraphStepListener(graphView ->
                                            {
                                                graphStepListenerInvocations.incrementAndGet();
                                                assertTrue(graphView.get()
                                                                    .size() >= 2);
                                            })
                                            .stream()
                                            .collect(Collectors.toList());
        assertEquals(3, routes.size());
        assertEquals(3, graphStepListenerInvocations.get());
        assertEquals(SetUtils.toSet(rootNode, detachedNode), routes.get(0)
                                                                   .getTracingGraph()
                                                                   .get()
                                                                   .nodes()
                                                                   .stream()
                                                                   .map(Node::getIdentity)
                                                                   .collect(Collectors.toSet()));
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, detachedNode), routes.get(1)
                                                                                                                            .getTracingGraph()
                                                                                                                            .get()
                                                                                                                            .nodes()
                                                                                                                            .stream()
                                                                                                                            .map(Node::getIdentity)
                                                                                                                            .collect(Collectors.toSet()));
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, detachedNode, childNode1, childNode2, childNode3),
                     routes.get(2)
                           .getTracingGraph()
                           .get()
                           .nodes()
                           .stream()
                           .map(Node::getIdentity)
                           .collect(Collectors.toSet()));
    }

    @Test
    public void testTracingGraphWithBudgetTraversal() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity intermediateNode4 = NodeIdentity.of("1.4");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity childNode4 = NodeIdentity.of("1.3.1");
        NodeIdentity childNode5 = NodeIdentity.of("1.3.2");
        NodeIdentity childNode6 = NodeIdentity.of("1.3.3");
        NodeIdentity childNode7 = NodeIdentity.of("1.3.4");
        NodeIdentity childNode8 = NodeIdentity.of("1.3.5");
        NodeIdentity leafNode1 = NodeIdentity.of("1.1.1.1");
        NodeIdentity leafNode2 = NodeIdentity.of("1.1.2.1");
        NodeIdentity leafNode3 = NodeIdentity.of("1.2.1.1");
        NodeIdentity leafNode4 = NodeIdentity.of("1.3.1.1");
        NodeIdentity leafNode5 = NodeIdentity.of("1.3.2.1");
        NodeIdentity leafNode6 = NodeIdentity.of("1.3.3.1");
        NodeIdentity leafNode7 = NodeIdentity.of("1.3.4.1");
        NodeIdentity leafNode8 = NodeIdentity.of("1.3.5.1");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1) // 0.25
                                .addEdge(rootNode, intermediateNode2) // 0.25
                                .addEdge(rootNode, intermediateNode3) // 0.25
                                .addEdge(rootNode, intermediateNode4) // 0.25
                                .addEdge(intermediateNode1, childNode1) // 0.125
                                .addEdge(intermediateNode1, childNode2) // 0.125
                                .addEdge(intermediateNode2, childNode3) // 0.25
                                .addEdge(intermediateNode3, childNode4) // 0.05
                                .addEdge(intermediateNode3, childNode5) // 0.05
                                .addEdge(intermediateNode3, childNode6) // 0.05
                                .addEdge(intermediateNode3, childNode7) // 0.05
                                .addEdge(intermediateNode3, childNode8) // 0.05
                                .addEdge(childNode1, leafNode1) // 0.125
                                .addEdge(childNode2, leafNode2) // 0.125
                                .addEdge(childNode3, leafNode3) // 0.25
                                .addEdge(childNode4, leafNode4) // 0.05
                                .addEdge(childNode5, leafNode5) // 0.05
                                .addEdge(childNode6, leafNode6) // 0.05
                                .addEdge(childNode7, leafNode7) // 0.05
                                .addEdge(childNode8, leafNode8) // 0.05
                                .build();

        List<TraversalRoutes> routes = graph.routing()
                                            .withBreadthFirst()
                                            .budgetOptimized()
                                            .traverseOutgoing()
                                            .withTracingGraph()
                                            .stream()
                                            .collect(Collectors.toList());

        assertEquals(6, routes.size());
        assertEquals(SetUtils.toSet(rootNode), routes.get(0)
                                                     .getTracingGraph()
                                                     .get()
                                                     .nodes()
                                                     .identities());
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4), routes.get(1)
                                                                                                                                 .getTracingGraph()
                                                                                                                                 .get()
                                                                                                                                 .nodes()
                                                                                                                                 .identities());
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4, childNode1, childNode2, childNode3),
                     routes.get(2)
                           .getTracingGraph()
                           .get()
                           .nodes()
                           .identities());
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4, childNode1, childNode2, childNode3,
                                    leafNode1, leafNode2, leafNode3),
                     routes.get(3)
                           .getTracingGraph()
                           .get()
                           .nodes()
                           .identities());
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4, childNode1, childNode2, childNode3,
                                    leafNode1, leafNode2, leafNode3, childNode4, childNode5, childNode6, childNode7, childNode8),
                     routes.get(4)
                           .getTracingGraph()
                           .get()
                           .nodes()
                           .identities());
        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, intermediateNode4, childNode1, childNode2, childNode3,
                                    leafNode1, leafNode2, leafNode3, childNode4, childNode5, childNode6, childNode7, childNode8, leafNode4, leafNode5,
                                    leafNode6, leafNode7, leafNode8),
                     routes.get(5)
                           .getTracingGraph()
                           .get()
                           .nodes()
                           .identities());
    }

    @Test
    public void testTraverseIntoGraph() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(rootNode, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .addEdge(intermediateNode2, childNode3)
                                .addNode(detachedNode)
                                .build();

        GraphSteppedView traversalGraphSteps = graph.routing()
                                                    .withBreadthFirst()
                                                    .traverseOutgoing()
                                                    .and()
                                                    .viewAsGraph();

        assertEquals(SetUtils.toSet(rootNode, intermediateNode1, intermediateNode2, intermediateNode3, detachedNode, childNode1, childNode2, childNode3),
                     traversalGraphSteps.last()
                                        .get()
                                        .nodes()
                                        .identities());
    }

    @Test
    public void testTraverseEach() throws Exception
    {
        NodeIdentity rootNode1 = NodeIdentity.of("1");
        NodeIdentity rootNode2 = NodeIdentity.of("2");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2/2.1");
        NodeIdentity intermediateNode3 = NodeIdentity.of("2.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2/2.1.1");
        NodeIdentity childNode3 = NodeIdentity.of("2.1.2");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode1, intermediateNode1)
                                .addEdge(rootNode1, intermediateNode2)
                                .addEdge(rootNode2, intermediateNode2)
                                .addEdge(rootNode2, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode2, childNode2)
                                .addEdge(intermediateNode3, childNode3)
                                .addNode(detachedNode)
                                .build();

        VisitedNodesStatistic nodeIdentityToCount = graph.routing()
                                                         .withBreadthFirst()
                                                         .traverseEach(Direction.OUTGOING, rootNode1, rootNode2)
                                                         .and()
                                                         .getVisitedNodesStatistic();

        assertEquals(1, nodeIdentityToCount.getCountFor(rootNode1));
        assertEquals(1, nodeIdentityToCount.getCountFor(rootNode2));
        assertEquals(1, nodeIdentityToCount.getCountFor(intermediateNode1));
        assertEquals(2, nodeIdentityToCount.getCountFor(intermediateNode2));
        assertEquals(1, nodeIdentityToCount.getCountFor(intermediateNode3));
        assertEquals(1, nodeIdentityToCount.getCountFor(childNode1));
        assertEquals(2, nodeIdentityToCount.getCountFor(childNode2));
        assertEquals(1, nodeIdentityToCount.getCountFor(childNode3));
        assertEquals(0, nodeIdentityToCount.getCountFor(detachedNode));
    }

    @Test
    public void testTraverseAndViewAsLayers() throws Exception
    {
        NodeIdentity rootNode1 = NodeIdentity.of("1");
        NodeIdentity rootNode2 = NodeIdentity.of("2");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2/2.1");
        NodeIdentity intermediateNode3 = NodeIdentity.of("2.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2/2.1.1");
        NodeIdentity childNode3 = NodeIdentity.of("2.1.2");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode1, intermediateNode1)
                                .addEdge(rootNode1, intermediateNode2)
                                .addEdge(rootNode2, intermediateNode2)
                                .addEdge(rootNode2, intermediateNode3)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode2, childNode2)
                                .addEdge(intermediateNode3, childNode3)
                                .addNode(detachedNode)
                                .build();

        {
            List<Graph> layers = graph.routing()
                                      .withBreadthFirst()
                                      .traverseEach(Direction.OUTGOING, rootNode1, rootNode2)
                                      .and()
                                      .viewAsGraph()
                                      .layers()
                                      .byVisitedCount()
                                      .stream()
                                      .map(GraphLayer::get)
                                      .collect(Collectors.toList());

            assertEquals(3, layers.size());
            assertEquals(Collections.emptySet(), layers.get(0)
                                                       .nodes()
                                                       .identities());
            assertEquals(SetUtils.toSet(rootNode1, rootNode2, intermediateNode1, intermediateNode3, childNode1, childNode3), layers.get(1)
                                                                                                                                   .nodes()
                                                                                                                                   .identities());
            assertEquals(SetUtils.toSet(intermediateNode2, childNode2), layers.get(2)
                                                                              .nodes()
                                                                              .identities());
        }
        {
            List<Graph> layers = graph.routing()
                                      .withBreadthFirst()
                                      .traverseEach(Direction.OUTGOING, rootNode1, rootNode2)
                                      .and()
                                      .viewAsGraph()
                                      .layers()
                                      .byAtLeastVisitedCount()
                                      .stream()
                                      .map(GraphLayer::get)
                                      .collect(Collectors.toList());

            assertEquals(3, layers.size());
            assertEquals(SetUtils.toSet(rootNode1, rootNode2, intermediateNode1, intermediateNode3, childNode1, childNode3, intermediateNode2, childNode2),
                         layers.get(0)
                               .nodes()
                               .identities());
            assertEquals(SetUtils.toSet(rootNode1, rootNode2, intermediateNode1, intermediateNode3, childNode1, childNode3, intermediateNode2, childNode2),
                         layers.get(1)
                               .nodes()
                               .identities());
            assertEquals(SetUtils.toSet(intermediateNode2, childNode2), layers.get(2)
                                                                              .nodes()
                                                                              .identities());
        }
    }

    @Test
    public void testSifSerialization() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdgeWithAttributes(rootNode, intermediateNode1, Tag.of("->"))
                                .addEdgeWithAttributes(rootNode, intermediateNode2, Tag.of("->"))
                                .addEdgeWithAttributes(rootNode, intermediateNode3, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode1, childNode1, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode1, childNode2, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode2, childNode3, Tag.of("->"))
                                .addNode(detachedNode)
                                .build();
        assertEquals(StringUtils.builder()
                                .withLineSeparator("\n")
                                .addLine("1.1\t->\t1.1.1")
                                .addLine("1.1\t->\t1.1.2")
                                .addLine("1.2\t->\t1.2.1")
                                .addLine("1\t->\t1.1")
                                .addLine("1\t->\t1.2")
                                .addLine("1\t->\t1.3")
                                .addLine("detached\t \t")
                                .build(),
                     graph.serialize()
                          .toSif()
                          .withLabelProvider(node -> node.getIdentity()
                                                         .getPrimaryId())
                          .get());

        assertEquals(graph, GraphUtils.deserialize()
                                      .asSif()
                                      .from(graph.serialize()
                                                 .toSif()
                                                 .get()));
    }

    @Test
    public void testPlantUmlSerialization() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity intermediateNode3 = NodeIdentity.of("1.3");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        NodeIdentity childNode3 = NodeIdentity.of("1.2.1");
        NodeIdentity detachedNode = NodeIdentity.of("detached");
        Graph graph = GraphUtils.builder()
                                .addEdgeWithAttributes(rootNode, intermediateNode1, Tag.of("->"))
                                .addEdgeWithAttributes(rootNode, intermediateNode2, Tag.of("->"))
                                .addEdgeWithAttributes(rootNode, intermediateNode3, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode1, childNode1, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode1, childNode2, Tag.of("->"))
                                .addEdgeWithAttributes(intermediateNode2, childNode3, Tag.of("->"))
                                .addNode(detachedNode)
                                .build();
        assertEquals(StringUtils.builder()
                                .withLineSeparator("\n")
                                .addLine("@startuml")
                                .addLine("object 1")
                                .addLine("object 1.1")
                                .addLine("object 1.1.1")
                                .addLine("object 1.1.2")
                                .addLine("object 1.2")
                                .addLine("object 1.2.1")
                                .addLine("object 1.3")
                                .addLine("object detached")
                                .addLine("1 --> 1.1")
                                .addLine("1 --> 1.2")
                                .addLine("1 --> 1.3")
                                .addLine("1.1 --> 1.1.1")
                                .addLine("1.1 --> 1.1.2")
                                .addLine("1.2 --> 1.2.1")
                                .addLine("@enduml")
                                .build(),
                     graph.serialize()
                          .toPlantUml()
                          .withLabelProvider(node -> node.getIdentity()
                                                         .getPrimaryId())
                          .get());

    }

    @Test
    public void testTransformAndCollect() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity childNode1 = NodeIdentity.of("1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.2");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, childNode1)
                                .addEdge(rootNode, childNode2)
                                .build();
        assertEquals(graph, graph.transform()
                                 .collect());
    }

    @Test
    public void testTransformAndAdd() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity childNode1 = NodeIdentity.of("1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.2");
        Graph graph = GraphUtils.builder()
                                .addNodes(rootNode, childNode1)
                                .addEdge(rootNode, childNode2)
                                .build();
        assertEquals(graph, GraphUtils.empty()
                                      .transform()
                                      .addNodes(rootNode, childNode1)
                                      .addEdge(rootNode, childNode2)
                                      .collect());
    }

    @Test
    public void testTransformAndMap() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity childNode1 = NodeIdentity.of("1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.2");
        assertEquals(GraphUtils.builder()
                               .addEdge(rootNode, childNode2)
                               .build(),
                     GraphUtils.builder()
                               .addEdge(rootNode, childNode1)
                               .build()
                               .transform()
                               .map(node -> childNode1.equals(node.getIdentity()) ? childNode2 : node.getIdentity())
                               .collect());
    }

    private Consumer<GraphBuilder> createGraphPreparator()
    {
        return graphBuilder ->
        {
            NodeIdentity rootNode = NodeIdentity.of("C1");
            NodeIdentity childNode = NodeIdentity.of("C1.1");
            graphBuilder.addBidirectionalEdge(rootNode, childNode);
        };
    }

    private SupplierConsumer<String> createCache()
    {
        return new SupplierConsumer<String>()
        {
            private String json;

            @Override
            public void accept(String json)
            {
                this.json = JSONHelper.serialize(json);
            }

            @Override
            public String get()
            {
                return JSONHelper.deserializer(String.class)
                                 .apply(this.json);
            }
        };
    }
}
