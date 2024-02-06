package org.omnaest.utils.graph.domain;

import java.io.File;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.utils.graph.domain.node.Node;

public interface GraphSerializer
{
    public String toJson();

    public SIFSerializer toSif();

    public PlantUmlSerializer toPlantUml();

    public static interface SIFSerializer extends Supplier<String>
    {

        public SIFSerializer writeInto(File file);

        public SIFSerializer withLabelProvider(Function<Node, String> labelProvider);

    }

    public static interface PlantUmlSerializer extends Supplier<String>
    {

        public PlantUmlSerializer writeInto(File file);

        public PlantUmlSerializer withLabelProvider(Function<Node, String> labelProvider);

        public PlantUmlSerializer withFieldsProvider(Function<Node, Map<String, String>> attributesProvider);
    }
}