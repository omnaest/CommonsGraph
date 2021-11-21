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

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.GraphBuilder.NodeResolver;

/**
 * @author omnaest
 */
public interface Node
{
    public NodeIdentity getIdentity();

    public Nodes getOutgoingNodes();

    public Nodes getIncomingNodes();

    public Edges getOutgoingEdges();

    public Edges getIncomingEdges();

    /**
     * Returns all {@link #getIncomingEdges()} and {@link #getOutgoingEdges()}
     * 
     * @return
     */
    public Edges getAllEdges();

    /**
     * Finds all {@link #getAllEdges()} having the given {@link Tag}
     * 
     * @param tag
     * @return
     */
    public Edges findAllEdgesWithTag(Tag tag);

    public Optional<Edge> findOutgoingEdgeTo(NodeIdentity nodeIdentity);

    public Optional<Edge> findIncomingEdgeFrom(NodeIdentity nodeIdentity);

    public Stream<Edge> findEdgesTo(NodeIdentity nodeIdentity);

    /**
     * Resolves all lazy connections to this {@link Node} using the defined {@link NodeResolver}s of the {@link Graph}
     * 
     * @return
     */
    public Node resolve();

}
