package org.omnaest.utils.graph.domain.node;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.graph.domain.node.NodeIdentity;

/**
 * @see NodeIdentity
 * @author omnaest
 */
public class NodeIdentityTest
{
    @Test
    public void testGet() throws Exception
    {
        assertEquals("[\"a\",\"b\"]", JSONHelper.serialize(NodeIdentity.of("a", "b")));
        assertEquals(NodeIdentity.of("a", "b"), JSONHelper.clone(NodeIdentity.of("a", "b")));
    }

}
