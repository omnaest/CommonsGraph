package org.omnaest.utils.graph.internal.serialization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.graph.internal.serialization.PlantUmlUtils.ObjectGraphDocument;

public class PlantUmlUtilsTest
{

    @Test
    public void testBuilder() throws Exception
    {
        ObjectGraphDocument document = PlantUmlUtils.builder()
                                                    .forObjectGraphDocument()
                                                    .addObject("o1")
                                                    .addObject("o2")
                                                    .addAssociation("o1", "o2")
                                                    .build();

        assertEquals(StringUtils.builder()
                                .withLineSeparator("\n")
                                .addLine("@startuml")
                                .addLine("object o1")
                                .addLine("object o2")
                                .addLine("o1 --> o2")
                                .addLine("@enduml")
                                .build(),
                     document.get());
    }

    @Test
    public void testBuilderWithFields() throws Exception
    {
        ObjectGraphDocument document = PlantUmlUtils.builder()
                                                    .forObjectGraphDocument()
                                                    .addObject("o1")
                                                    .addObjectWithFields("o2", MapUtils.builder()
                                                                                       .put("field1", "value1")
                                                                                       .build())
                                                    .addAssociation("o1", "o2")
                                                    .build();

        assertEquals(StringUtils.builder()
                                .withLineSeparator("\n")
                                .addLine("@startuml")
                                .addLine("object o1")
                                .addLine("object o2")
                                .addLine("o2 : field1 = value1")
                                .addLine("o1 --> o2")
                                .addLine("@enduml")
                                .build(),
                     document.get());
    }

}
