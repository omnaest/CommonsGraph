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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Test;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.element.bi.UnaryBiElement;
import org.omnaest.utils.graph.domain.Attribute;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphRouter.HierarchicalNode;
import org.omnaest.utils.graph.domain.GraphRouter.Hierarchy;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutes;
import org.omnaest.utils.graph.domain.GraphRouter.TraversedEdge;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Tag;
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
                          .andTraverseIncoming()
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
