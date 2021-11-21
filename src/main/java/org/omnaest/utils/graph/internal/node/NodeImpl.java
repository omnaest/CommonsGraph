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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.graph.domain.Attribute;
import org.omnaest.utils.graph.domain.Edge;
import org.omnaest.utils.graph.domain.Edges;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;
import org.omnaest.utils.graph.domain.Nodes;
import org.omnaest.utils.graph.domain.Tag;
import org.omnaest.utils.graph.internal.GraphBuilderImpl.NodeResolverSupport;
import org.omnaest.utils.graph.internal.edge.EdgeImpl;
import org.omnaest.utils.graph.internal.edge.EdgesImpl;
import org.omnaest.utils.graph.internal.index.GraphIndexAccessor;

/**
 * @see Node
 * @author omnaest
 */
public class NodeImpl implements Node
{
    private final NodeIdentity        nodeIdentity;
    private final GraphIndexAccessor  graphIndexAccessor;
    private final NodeResolverSupport nodeResolverSupport;

    public NodeImpl(NodeIdentity nodeIdentity, GraphIndexAccessor graphIndexAccessor, NodeResolverSupport nodeResolverSupport)
    {
        this.nodeIdentity = nodeIdentity;
        this.graphIndexAccessor = graphIndexAccessor;
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
        return new NodesImpl(this.graphIndexAccessor.getOutgoingNodes(this.nodeIdentity), this.graphIndexAccessor, this.nodeResolverSupport);
    }

    @Override
    public Nodes getIncomingNodes()
    {
        return new NodesImpl(this.graphIndexAccessor.getIncomingNodes(this.nodeIdentity), this.graphIndexAccessor, this.nodeResolverSupport);
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

    @Override
    public Edges getOutgoingEdges()
    {
        return new EdgesImpl(this.getOutgoingNodes()
                                 .stream()
                                 .map(node ->
                                 {
                                     Node from = this;
                                     Node to = node;
                                     Set<Attribute> attributes = this.graphIndexAccessor.getEdgeAttributes(from.getIdentity(), to.getIdentity())
                                                                                        .orElse(Collections.emptySet());

                                     return new EdgeImpl(from, to, attributes);
                                 })
                                 .collect(Collectors.toList()));
    }

    @Override
    public Edges getIncomingEdges()
    {
        return new EdgesImpl(this.getIncomingNodes()
                                 .stream()
                                 .map(node ->
                                 {
                                     Node from = node;
                                     Node to = this;
                                     Set<Attribute> attributes = this.graphIndexAccessor.getEdgeAttributes(from.getIdentity(), to.getIdentity())
                                                                                        .orElse(Collections.emptySet());

                                     return new EdgeImpl(from, to, attributes);
                                 })
                                 .collect(Collectors.toList()));
    }

    @Override
    public Edges getAllEdges()
    {
        return new EdgesImpl(Stream.concat(this.getIncomingEdges()
                                               .stream(),
                                           this.getOutgoingEdges()
                                               .stream())
                                   .collect(Collectors.toList()));
    }

    @Override
    public Edges findAllEdgesWithTag(Tag tag)
    {
        return new EdgesImpl(this.getAllEdges()
                                 .stream()
                                 .filter(edge -> edge.hasTag(tag))
                                 .collect(Collectors.toList()));
    }

    @Override
    public Optional<Edge> findOutgoingEdgeTo(NodeIdentity nodeIdentity)
    {
        if (this.graphIndexAccessor.getOutgoingNodes(this.nodeIdentity)
                                   .contains(nodeIdentity))
        {
            Node from = this;
            Node to = new NodeImpl(nodeIdentity, this.graphIndexAccessor, this.nodeResolverSupport);
            Set<Attribute> attributes = this.graphIndexAccessor.getEdgeAttributes(from.getIdentity(), to.getIdentity())
                                                               .orElse(Collections.emptySet());
            return Optional.of(new EdgeImpl(from, to, attributes));
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Edge> findIncomingEdgeFrom(NodeIdentity nodeIdentity)
    {
        if (this.graphIndexAccessor.getIncomingNodes(this.nodeIdentity)
                                   .contains(nodeIdentity))
        {
            Node from = new NodeImpl(nodeIdentity, this.graphIndexAccessor, this.nodeResolverSupport);
            Node to = this;
            Set<Attribute> attributes = this.graphIndexAccessor.getEdgeAttributes(from.getIdentity(), to.getIdentity())
                                                               .orElse(Collections.emptySet());
            return Optional.of(new EdgeImpl(from, to, attributes));
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public Stream<Edge> findEdgesTo(NodeIdentity nodeIdentity)
    {
        return Stream.of(this.findIncomingEdgeFrom(nodeIdentity), this.findOutgoingEdgeTo(nodeIdentity))
                     .filter(Optional::isPresent)
                     .map(Optional::get);
    }

}
