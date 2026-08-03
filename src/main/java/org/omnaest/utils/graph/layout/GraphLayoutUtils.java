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

import java.util.Objects;

import org.omnaest.utils.graph.layout.domain.LayoutGraphBuilder;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;
import org.omnaest.utils.graph.layout.internal.LayeredGraphLayoutImpl;
import org.omnaest.utils.graph.layout.internal.LayoutGraphBuilderImpl;

/**
 * Facade for the domain-free layered graph layout engine, mirroring the {@link org.omnaest.utils.graph.GraphUtils}
 * convention.
 *
 * @see #newLayoutGraph()
 * @see #newLayeredLayout()
 * @author omnaest
 */
public class GraphLayoutUtils
{
    private GraphLayoutUtils()
    {
        super();
    }

    public static LayoutGraphBuilder newLayoutGraph()
    {
        return new LayoutGraphBuilderImpl();
    }

    public static LayeredGraphLayout newLayeredLayout()
    {
        return new LayeredGraphLayoutImpl(LayoutOptions.defaults());
    }

    public static LayeredGraphLayout newLayeredLayout(LayoutOptions options)
    {
        return new LayeredGraphLayoutImpl(Objects.requireNonNull(options, "options must not be null"));
    }
}
