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

import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class NodeImpl implements Node
{
    private final NodeIdentity nodeIdentity;
    private final GraphIndex   graphIndex;

    public NodeImpl(NodeIdentity nodeIdentity, GraphIndex graphIndex)
    {
        this.nodeIdentity = nodeIdentity;
        this.graphIndex = graphIndex;
    }

    @Override
    public NodeIdentity getIdentity()
    {
        return this.nodeIdentity;
    }

    @Override
    public Nodes getOutgoingNodes()
    {
        return new NodesImpl(this.graphIndex.getOutgoingNodes(this.nodeIdentity), this.graphIndex);
    }

    @Override
    public Nodes getIncomingNodes()
    {
        return new NodesImpl(this.graphIndex.getIncomingNodes(this.nodeIdentity), this.graphIndex);
    }

    @Override
    public int hashCode()
    {
        return this.nodeIdentity.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Node)
        {
            return this.nodeIdentity.equals(((Node) obj).getIdentity());
        }
        else if (obj instanceof NodeIdentity)
        {
            return this.nodeIdentity.equals(obj);
        }
        else
        {
            return false;
        }
    }

}
