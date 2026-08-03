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

/**
 * Builder for a {@link LayoutGraph}. Obtain via {@link org.omnaest.utils.graph.layout.GraphLayoutUtils#newLayoutGraph()}.
 *
 * @author omnaest
 */
public interface LayoutGraphBuilder
{
    public LayoutGraphBuilder addNode(LayoutNode node);

    public LayoutGraphBuilder addNode(LayoutNodeId id, Size size);

    public LayoutGraphBuilder addEdge(LayoutEdge edge);

    public LayoutGraphBuilder addEdge(LayoutEdgeId id, LayoutNodeId from, LayoutNodeId to);

    public LayoutGraph build();
}
