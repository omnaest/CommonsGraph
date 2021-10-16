package org.omnaest.utils.graph.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link Edge} between a source and target {@link Node} in a {@link Graph}
 * 
 * @author omnaest
 */
public interface Edge
{
    public Set<Attribute> getAttributes();

    /**
     * Returns the from and to {@link NodeIdentity}s
     * 
     * @return
     */
    public List<NodeIdentity> getNodeIdentities();

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
}