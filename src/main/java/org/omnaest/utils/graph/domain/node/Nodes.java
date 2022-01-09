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
package org.omnaest.utils.graph.domain.node;

import java.util.Optional;

import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphBuilder.MultiNodeResolver;
import org.omnaest.utils.stream.Streamable;

public interface Nodes extends Streamable<Node>
{
    public Optional<Node> findById(NodeIdentity nodeIdentity);

    /**
     * Similar to {@link Node#resolve()} but executing as {@link MultiNodeResolver}
     * 
     * @return
     */
    public Nodes resolveAll();

    /**
     * Returns true, if this {@link Nodes} instance contains any {@link Node}
     * 
     * @return
     */
    public boolean hasAny();

    /**
     * Returns true, if this {@link Nodes} instance contains no {@link Node}s at all.
     * 
     * @return
     */
    public boolean hasNone();

    /**
     * Returns the number of {@link Node}s
     * 
     * @return
     */
    public int size();
}
