package org.omnaest.utils.graph.domain.traversal;

public enum Direction
{
    OUTGOING, INCOMING;

    /**
     * Returns the inverse {@link Direction}
     * 
     * @return
     */
    public Direction inverse()
    {
        return OUTGOING.equals(this) ? Direction.INCOMING : Direction.OUTGOING;
    }
}