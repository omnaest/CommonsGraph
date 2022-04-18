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
package org.omnaest.utils.graph;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder;
import org.omnaest.utils.graph.domain.GraphDeserializer;
import org.omnaest.utils.graph.domain.GraphDeserializerImpl;
import org.omnaest.utils.graph.internal.GraphBuilderImpl;
import org.omnaest.utils.supplier.SupplierConsumer;

/**
 * Utility to create a {@link Graph}. The default {@link Graph} will utilize {@link ConcurrentHashMap} instances, but it also can operate on top of database
 * repositories / key-value stores.
 *
 * @see #builder()
 * @author omnaest
 */
public class GraphUtils
{
    public static GraphBuilder builder()
    {
        return new GraphBuilderImpl();
    }

    /**
     * Creates a {@link Graph} using the {@link GraphBuilder} passed to the given {@link Consumer}.The created {@link Graph} is serialized and written to the
     * given {@link SupplierConsumer}. If the {@link SupplierConsumer} returns an instance json representation, the {@link Graph} is deserialized and not
     * created from scratch by the {@link GraphBuilder}.
     * 
     * @param cache
     * @param graphBuilderConsumer
     * @return
     */
    public static Graph newCachedGraph(SupplierConsumer<String> cache, Consumer<GraphBuilder> graphBuilderConsumer)
    {
        return Optional.ofNullable(cache.get())
                       .map(deserialize()::fromJson)
                       .orElseGet(() ->
                       {
                           GraphBuilder graphBuilder = builder();
                           graphBuilderConsumer.accept(graphBuilder);
                           Graph graph = graphBuilder.build();
                           cache.accept(graph.serialize()
                                             .toJson());
                           return graph;
                       });
    }

    /**
     * Creates a new {@link GraphDeserializer} instance.
     * 
     * @return
     */
    public static GraphDeserializer deserialize()
    {
        return new GraphDeserializerImpl();
    }

    public static Graph empty()
    {
        return builder().build();
    }
}
