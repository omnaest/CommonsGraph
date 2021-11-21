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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Test;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.Attribute;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.RouteAndTraversalControl;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.GraphRouter.TraversalRoutes;
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
