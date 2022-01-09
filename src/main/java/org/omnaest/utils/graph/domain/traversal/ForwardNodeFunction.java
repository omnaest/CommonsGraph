package org.omnaest.utils.graph.domain.traversal;

import java.util.function.Function;

import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.Nodes;

/**
 * {@link Function} that transforms a given {@link Node} into the next {@link Nodes} based on the underlying {@link Direction}
 * 
 * @author omnaest
 */
public interface ForwardNodeFunction extends Function<Node, Nodes>
{
}