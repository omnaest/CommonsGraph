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
package org.omnaest.utils.graph.domain.node;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder.NodeResolver;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.edge.Edges;

/**
 * @author omnaest
 */
public interface Node
{
    public NodeIdentity getIdentity();

    public Nodes getOutgoingNodes();

    public Nodes getIncomingNodes();

    /**
     * Returns all {@link #getIncomingNodes() and all #getOutgoingNodes()
     * 
     * @return
     */
    public Nodes getConnectedNodes();

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

    public NodeDataAccessor getData();

    public static interface NodeDataAccessor
    {
        public Optional<NodeDataFieldValueAccessor> getValue(String key);

        public Map<String, Object> toMap();

        /**
         * Returns an {@link Object} instance where the field values are provided by the underlying data of the node.
         * 
         * @param type
         * @return
         */
        public <T> T to(Class<T> type);
    }

    public static interface NodeDataFieldValueAccessor extends Supplier<Object>
    {
        public String getAsString();

        public int getAsInteger();

        public boolean getAsBoolean();

        public long getAsLong();

        public double getAsDouble();
    }

    /**
     * Returns true, if this {@link Node} has no {@link Edge} to any other {@link Node}
     * 
     * @return
     */
    public boolean isDetached();

}
