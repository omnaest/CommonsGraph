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
package org.omnaest.utils.graph.internal.router;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphRouter;
import org.omnaest.utils.graph.internal.router.strategy.BreadthFirstRoutingStrategy;

public class GraphRouterImpl implements GraphRouter
{
    private final Graph graph;

    public GraphRouterImpl(Graph graph)
    {
        this.graph = graph;
    }

    @Override
    public RoutingStrategy withBreadthFirst()
    {
        return new BreadthFirstRoutingStrategy(this.graph);
    }
}
