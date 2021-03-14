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
package org.omnaest.utils.graph.internal;

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.GraphIndex;
import org.omnaest.utils.graph.internal.node.NodeImpl;
import org.omnaest.utils.graph.internal.router.GraphRouterImpl;
import org.omnaest.utils.json.AbstractJSONSerializable;

public class GraphImpl extends AbstractJSONSerializable implements Graph
{
    private GraphIndex graphIndex;

    public GraphImpl(GraphIndex graphIndex)
    {
        super();
        this.graphIndex = graphIndex;
    }

    @Override
    public Stream<Node> stream()
    {
        return this.graphIndex.getNodes()
                              .stream()
                              .map(this::wrapIntoNode);
    }

    private Node wrapIntoNode(NodeIdentity nodeIdentity)
    {
        return new NodeImpl(nodeIdentity, this.graphIndex);
    }

    @Override
    public Optional<Node> findNodeById(NodeIdentity nodeIdentity)
    {
        return Optional.ofNullable(nodeIdentity)
                       .filter(this.graphIndex::containsNode)
                       .map(this::wrapIntoNode);
    }

    @Override
    public GraphRouter newRouter()
    {
        return new GraphRouterImpl(this);
    }

    @Override
    public int size()
    {
        return this.graphIndex.getNodes()
                              .size();
    }

}
