/*******************************************************************************
 * Copyright 2026 Danny Kunz
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
package org.omnaest.utils.graph.layout.domain;

import java.util.List;
import java.util.Optional;

/**
 * Immutable, domain-free input graph for the layered layout engine: sized nodes and identified edges only - no text, no
 * styling. Build one via {@link org.omnaest.utils.graph.layout.GraphLayoutUtils#newLayoutGraph()}.
 *
 * @author omnaest
 */
public interface LayoutGraph
{
    public List<LayoutNode> getNodes();

    public List<LayoutEdge> getEdges();

    public Optional<LayoutNode> findNodeById(LayoutNodeId id);
}
