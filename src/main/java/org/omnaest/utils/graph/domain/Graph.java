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

import java.util.Collection;
import java.util.Optional;

import org.omnaest.utils.graph.GraphUtils;
import org.omnaest.utils.stream.Streamable;

/**
 * A {@link Graph} with {@link Node}s
 * 
 * @see GraphUtils#builder()
 * @author omnaest
 */
public interface Graph extends Streamable<Node>
{
    /**
     * Returns a {@link Node} for a given {@link NodeIdentity} of the {@link Graph}. If the {@link Graph} does not contain the {@link NodeIdentity} then
     * {@link Optional#empty()} is returned, but never null.
     * 
     * @param nodeIdentity
     * @return
     */
    public Optional<Node> findNodeById(NodeIdentity nodeIdentity);

    /**
     * Similar to {@link #findNodeById(NodeIdentity)} but for a batch of {@link NodeIdentity}s
     * 
     * @see Nodes
     * @param nodeIdentities
     * @return
     */
    public Nodes findNodesByIds(Collection<NodeIdentity> nodeIdentities);

    public GraphRouter routing();

    public int size();

    /**
     * @see GraphResolver
     * @return
     */
    public GraphResolver resolver();

    /**
     * Clones the current {@link Graph} instance into a new in memory {@link Graph}
     * 
     * @return
     */
    public Graph clone();

    /**
     * Returns a {@link GraphSerializer} for this {@link Graph} instance.
     * 
     * @return
     */
    public GraphSerializer serialize();

    /**
     * Finds an {@link Edge} from one {@link NodeIdentity} to another. Be aware that the edge is direction dependent.
     * 
     * @param from
     * @param to
     * @return
     */
    public Optional<Edge> findEdge(NodeIdentity from, NodeIdentity to);

}
