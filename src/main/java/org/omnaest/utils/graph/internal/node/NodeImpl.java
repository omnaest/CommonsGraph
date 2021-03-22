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
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class NodeImpl implements Node
{
    private final NodeIdentity  nodeIdentity;
    private final GraphIndex    graphIndex;
    private NodeResolverSupport nodeResolverSupport;

    public NodeImpl(NodeIdentity nodeIdentity, GraphIndex graphIndex, NodeResolverSupport nodeResolverSupport)
    {
        this.nodeIdentity = nodeIdentity;
        this.graphIndex = graphIndex;
        this.nodeResolverSupport = nodeResolverSupport;
    }

    @Override
    public NodeIdentity getIdentity()
    {
        return this.nodeIdentity;
    }

    @Override
    public Nodes getOutgoingNodes()
    {
        return new NodesImpl(this.graphIndex.getOutgoingNodes(this.nodeIdentity), this.graphIndex, this.nodeResolverSupport);
    }

    @Override
    public Nodes getIncomingNodes()
    {
        return new NodesImpl(this.graphIndex.getIncomingNodes(this.nodeIdentity), this.graphIndex, this.nodeResolverSupport);
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

    @Override
    public Node resolve()
    {
        this.nodeResolverSupport.resolve(this.nodeIdentity);
        return this;
    }

    @Override
    public String toString()
    {
        return this.nodeIdentity.toString();
    }

}
