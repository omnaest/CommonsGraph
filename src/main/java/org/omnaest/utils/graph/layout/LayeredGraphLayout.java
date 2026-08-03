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
package org.omnaest.utils.graph.layout;

import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutResult;

/**
 * A domain-free layered (Sugiyama-family) graph layout algorithm: consumes a {@link LayoutGraph} of sized nodes and
 * identified edges and returns computed geometry. Holds no text, no styling, no diagram-specific concepts.
 * <p>
 * {@link #apply(LayoutGraph)} is pure: calling it twice with an equal {@link LayoutGraph} returns equal
 * {@link LayoutResult}s, and it never mutates its input.
 *
 * @see GraphLayoutUtils#newLayeredLayout()
 * @author omnaest
 */
public interface LayeredGraphLayout
{
    public LayoutResult apply(LayoutGraph graph);
}
