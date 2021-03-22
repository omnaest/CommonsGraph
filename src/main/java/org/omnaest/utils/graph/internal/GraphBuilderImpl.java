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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.omnaest.utils.SetUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.internal.index.GraphIndex;

public class GraphBuilderImpl implements GraphBuilder
{
    private GraphIndex          graphIndex          = new GraphIndex();
    private NodeResolverSupport nodeResolverSupport = new NodeResolverSupport(this.graphIndex);

    public static class NodeResolverSupport
    {
        private List<MultiNodeResolver> nodeResolvers = new ArrayList<>();
        private GraphIndex              graphIndex;

        public NodeResolverSupport(GraphIndex graphIndex)
        {
            this.graphIndex = graphIndex;
        }

        public NodeResolverSupport add(SingleNodeResolver nodeResolver)
        {
            if (nodeResolver != null)
            {
                this.add(nodeResolver.asMultiNodeResolver());
            }
            return this;
        }

        public NodeResolverSupport add(MultiNodeResolver nodeResolver)
        {
            if (nodeResolver != null)
            {
                this.nodeResolvers.add(nodeResolver);
            }
            return this;
        }

        public NodeResolverSupport resolve(Set<NodeIdentity> nodeIdentities)
        {
            if (nodeIdentities != null)
            {
                Set<NodeIdentity> unresolvedNodes = nodeIdentities.stream()
                                                                  .filter(this.graphIndex::isUnresolvedNode)
                                                                  .collect(Collectors.toSet());
                this.nodeResolvers.forEach(nodeResolver -> nodeResolver.apply(unresolvedNodes)
                                                                       .forEach(this.graphIndex::addEdge));
                this.graphIndex.markNodesAsResolved(unresolvedNodes);
            }
            return this;
        }

        public boolean isLazyLoadingActive()
        {
            return !this.nodeResolvers.isEmpty();
        }

        public NodeResolverSupport resolve(NodeIdentity nodeIdentity)
        {
            return this.resolve(SetUtils.toSet(nodeIdentity));
        }

    }

    @Override
    public GraphBuilder addNode(NodeIdentity nodeIdentity)
    {
        this.graphIndex.addNode(nodeIdentity);
        return this;
    }

    @Override
    public GraphBuilder addNodes(Collection<NodeIdentity> nodeIdentities)
    {
        this.graphIndex.addNodes(nodeIdentities);
        return this;
    }

    @Override
    public GraphBuilder addEdge(NodeIdentity from, NodeIdentity to)
    {
        this.graphIndex.addEdge(from, to);
        return this;
    }

    @Override
    public GraphBuilder addEdge(EdgeIdentity edgeIdentity)
    {
        this.graphIndex.addEdge(edgeIdentity);
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
        return new GraphImpl(this.graphIndex, this.nodeResolverSupport);
    }

    @Override
    public GraphBuilder withSingleNodeResolver(SingleNodeResolver nodeResolver)
    {
        this.nodeResolverSupport.add(nodeResolver);
        return this;
    }

    @Override
    public GraphBuilder withMultiNodeResolver(MultiNodeResolver nodeResolver)
    {
        this.nodeResolverSupport.add(nodeResolver);
        return this;
    }

}
