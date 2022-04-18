package org.omnaest.utils.graph.domain.edge;

import java.util.Set;
import java.util.stream.Stream;

import org.omnaest.utils.element.bi.UnaryBiElement;
import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentity;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentityWithAttributes;
import org.omnaest.utils.graph.domain.attributes.Attribute;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.node.Node;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

/**
 * {@link Edge} between a source and target {@link Node} in a {@link Graph}
 * <br>
 * <br>
 * Implements {@link #equals(Object)} and {@link #hashCode()} using the {@link EdgeIdentity} methods.
 * 
 * @author omnaest
 */
public interface Edge
{
    public Set<Attribute> getAttributes();

    public Set<Tag> getTags();

    /**
     * Returns the from and to {@link NodeIdentity}s
     * 
     * @return
     */
    public UnaryBiElement<NodeIdentity> getNodeIdentities();

    public Stream<Attribute> getAttributesByKey(String key);

    /**
     * Returns the target {@link Node}
     * 
     * @return
     */
    public Node getTo();

    /**
     * Returns the source {@link Node}
     * 
     * @return
     */
    public Node getFrom();

    /**
     * Returns true, if the given {@link Tag} is present.
     * 
     * @param tag
     * @return
     */
    public boolean hasTag(Tag tag);

    /**
     * Returns true, if the any of the given {@link Tag}s is present
     * 
     * @param tags
     * @return
     */
    public boolean hasAnyTag(Tag... tags);

    /**
     * Returns the {@link EdgeIdentity} of the current {@link Edge}
     * 
     * @return
     */
    public EdgeIdentity getIdentity();

    /**
     * Returns the {@link EdgeIdentityWithAttributes} of the current {@link Edge}
     * 
     * @return
     */
    public EdgeIdentityWithAttributes getIdentityWithAttributes();

    /**
     * Returns a {@link Stream} containing the {@link #getFrom()} and {@link #getTo()} {@link Node}s
     * 
     * @return
     */
    public Stream<Node> stream();

}