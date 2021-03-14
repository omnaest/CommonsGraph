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
package org.omnaest.utils.graph.internal.node;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class NodesImpl implements Nodes
{
    private final Set<NodeIdentity> nodeIdentities;
    private final GraphIndex        graphIndex;

    public NodesImpl(Set<NodeIdentity> nodeIdentities, GraphIndex graphIndex)
    {
        this.nodeIdentities = nodeIdentities;
        this.graphIndex = graphIndex;
    }

    @Override
    public Stream<Node> stream()
    {
        return this.nodeIdentities.stream()
                                  .map(nodeIdentity -> this.wrapIntoNode(nodeIdentity));
    }

    private NodeImpl wrapIntoNode(NodeIdentity nodeIdentity)
    {
        return new NodeImpl(nodeIdentity, this.graphIndex);
    }

    @Override
    public Optional<Node> findById(NodeIdentity nodeIdentity)
    {
        return Optional.ofNullable(nodeIdentity)
                       .filter(this.nodeIdentities::contains)
                       .map(this::wrapIntoNode);
    }
}
