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
package org.omnaest.utils.graph.internal.router.route;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.SimpleRoute;
import org.omnaest.utils.graph.domain.Node;
import org.omnaest.utils.graph.domain.NodeIdentity;

/**
 * @see RouteImpl
 * @author omnaest
 */
public class SimpleRouteImpl implements SimpleRoute
{
    protected List<NodeIdentity> route;
    protected Graph              graph;

    public SimpleRouteImpl(List<NodeIdentity> route, Graph graph)
    {
        super();
        this.route = route;
        this.graph = graph;
    }

    @Override
    public List<NodeIdentity> toNodeIdentities()
    {
        return Collections.unmodifiableList(this.route);
    }

    @Override
    public SimpleRoute addToNew(NodeIdentity nodeIdentity)
    {
        return new SimpleRouteImpl(ListUtils.addToNew(this.route, nodeIdentity), this.graph);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.route == null) ? 0 : this.route.hashCode());
        return result;
    }

    @Override
    public boolean isCyclic()
    {
        Map<NodeIdentity, Long> nodeIdentityToCount = this.toNodeIdentities()
                                                          .stream()
                                                          .collect(Collectors.groupingBy(MapperUtils.identity(), Collectors.counting()));
        return nodeIdentityToCount.values()
                                  .stream()
                                  .filter(count -> count > 1)
                                  .findAny()
                                  .isPresent();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        SimpleRouteImpl other = (SimpleRouteImpl) obj;
        if (this.route == null)
        {
            if (other.route != null)
            {
                return false;
            }
        }
        else if (!this.route.equals(other.route))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "<" + this.route + ">";
    }

    @Override
    public Stream<Node> stream()
    {
        return this.route.stream()
                         .map(this.createNodeIdentityToNodeMapper());
    }

    private Function<NodeIdentity, Node> createNodeIdentityToNodeMapper()
    {
        return nodeIdentity -> this.graph.findNodeById(nodeIdentity)
                                         .get();
    }

    @Override
    public Optional<Node> last()
    {
        return this.lastNth(0);
    }

    @Override
    public Optional<Node> lastNth(int index)
    {
        return ListUtils.optionalLast(this.route, index)
                        .map(this.createNodeIdentityToNodeMapper());
    }

    @Override
    public SimpleRoute getSubRouteUntilLastNth(int index)
    {
        return new SimpleRouteImpl(ListUtils.sublist(this.route, 0, this.route.size() - 1), this.graph);
    }

}
