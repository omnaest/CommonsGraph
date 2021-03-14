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
package org.omnaest.utils.graph.domain;

public interface GraphBuilder
{

    /**
     * Builds a {@link Graph} instance
     * 
     * @return
     */
    public Graph build();

    /**
     * Adds an edge from a {@link Node} with the given {@link NodeIdentity} to another. This added edge is directed. If a bidirectional edge should be added
     * consider using {@link #addBidirectionalEdge(NodeIdentity, NodeIdentity)} instead.
     * 
     * @param from
     * @param to
     * @return
     */
    public GraphBuilder addEdge(NodeIdentity from, NodeIdentity to);

    /**
     * Adds the forward and backward directed edges
     * 
     * @param from
     * @param to
     * @return
     */
    public GraphBuilder addBidirectionalEdge(NodeIdentity from, NodeIdentity to);

    /**
     * Adds a {@link Node} with the given {@link NodeIdentity} to the {@link Graph}
     * 
     * @param nodeIdentity
     * @return
     */
    public GraphBuilder addNode(NodeIdentity nodeIdentity);

}
