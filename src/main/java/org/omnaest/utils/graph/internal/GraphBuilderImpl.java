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

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class GraphBuilderImpl implements GraphBuilder
{
    private GraphIndex graphIndex = new GraphIndex();

    @Override
    public GraphBuilder addNode(NodeIdentity nodeIdentity)
    {
        this.graphIndex.addNode(nodeIdentity);
        return this;
    }

    @Override
    public GraphBuilder addEdge(NodeIdentity from, NodeIdentity to)
    {
        this.graphIndex.addEdge(from, to);
        return this;
    }

    @Override
    public GraphBuilder addBidirectionalEdge(NodeIdentity from, NodeIdentity to)
    {
        return this.addEdge(from, to)
                   .addEdge(to, from);
    }

    @Override
    public Graph build()
    {
        return new GraphImpl(this.graphIndex);
    }
}
