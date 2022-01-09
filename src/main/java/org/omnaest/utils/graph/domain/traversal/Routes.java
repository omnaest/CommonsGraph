package org.omnaest.utils.graph.domain.traversal;

import java.util.Optional;

import org.omnaest.utils.stream.Streamable;

public interface Routes extends Streamable<Route>
{

    public int size();

    /**
     * Returns the first {@link Route}
     * 
     * @return
     */
    @Override
    public Optional<Route> first();

    public boolean hasNoRoutes();

    public boolean hasRoutes();

}