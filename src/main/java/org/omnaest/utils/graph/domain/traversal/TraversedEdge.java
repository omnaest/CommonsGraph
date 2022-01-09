package org.omnaest.utils.graph.domain.traversal;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.edge.Edge;
import org.omnaest.utils.graph.domain.node.Node;

/**
 * A traversed {@link Edge} where #getNextNode() returns the next node of the traversal and #getPreviousNode()
 * returns the previous node in the traversal.<br>
 * <br>
 * #getEdge() returns the orginal {@link Edge} from the {@link Graph}. This edge is in the original direction and not aligned to the traversal direction.
 * <br>
 * <br>
 * Implements the {@link #hashCode()} and {@link #equals(Object)} redirecting to the underlying {@link #getEdge()}
 * 
 * @author omnaest
 */
public interface TraversedEdge
{
    public Node getNextNode();

    public Node getPreviousNode();

    public Edge getEdge();
}