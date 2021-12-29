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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.utils.ListUtils;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter.Route;
import org.omnaest.utils.graph.domain.GraphRouter.TraversedEdge;
import org.omnaest.utils.graph.domain.GraphRouter.TraversedEdges;
import org.omnaest.utils.graph.domain.NodeIdentity;

public class RouteImpl extends SimpleRouteImpl implements Route
{
    private List<TraversedEdge> edges;

    public RouteImpl(List<NodeIdentity> route, List<TraversedEdge> edges, Graph graph)
    {
        super(route, graph);
        this.edges = edges;
    }

    @Override
    public Optional<TraversedEdge> lastEdge()
    {
        return ListUtils.optionalLast(this.edges);
    }

    @Override
    public Optional<TraversedEdge> firstEdge()
    {
        return ListUtils.optionalFirst(this.edges);
    }

    @Override
    public TraversedEdges edges()
    {
        return new TraversedEdges()
        {
            @Override
            public Stream<TraversedEdge> stream()
            {
                return RouteImpl.this.edges.stream();
            }
        };
    }

}
