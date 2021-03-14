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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.GraphRouter.Routes;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;

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
    public void testRouter() throws Exception
    {
        NodeIdentity rootNode = NodeIdentity.of("1");
        NodeIdentity intermediateNode1 = NodeIdentity.of("1.1");
        NodeIdentity intermediateNode2 = NodeIdentity.of("1.2");
        NodeIdentity childNode1 = NodeIdentity.of("1.1.1");
        NodeIdentity childNode2 = NodeIdentity.of("1.1.2");
        Graph graph = GraphUtils.builder()
                                .addEdge(rootNode, intermediateNode1)
                                .addEdge(rootNode, intermediateNode2)
                                .addEdge(intermediateNode1, childNode1)
                                .addEdge(intermediateNode1, childNode2)
                                .build();

        assertEquals(5, graph.size());

        GraphRouter router = graph.newRouter();

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

}
