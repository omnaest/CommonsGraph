package org.omnaest.utils.graph.internal.serialization;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.omnaest.utils.graph.internal.serialization.SIFUtils.SIFEntry;

public class SIFUtilsTest
{
    @Test
    public void testParser()
    {
        List<SIFEntry> entries = SIFUtils.parse()
                                         .from(SIFUtils.builder()
                                                       .addEdge("1", "->", "2")
                                                       .addEdge("2", "->", "3")
                                                       .addEdge("3", "->", "4")
                                                       .build()
                                                       .get())
                                         .collect(Collectors.toList());
        assertEquals(3, entries.size());

        assertEquals("1", entries.get(0)
                                 .getFrom());
        assertEquals("2", entries.get(0)
                                 .getTo());
        assertEquals("->", entries.get(0)
                                  .getType());

        assertEquals("2", entries.get(1)
                                 .getFrom());
        assertEquals("3", entries.get(1)
                                 .getTo());
        assertEquals("->", entries.get(1)
                                  .getType());

        assertEquals("3", entries.get(2)
                                 .getFrom());
        assertEquals("4", entries.get(2)
                                 .getTo());
        assertEquals("->", entries.get(2)
                                  .getType());
    }
}
