package org.omnaest.utils.graph.domain;

import java.io.File;
import java.io.InputStream;
import java.util.function.Function;

import org.omnaest.utils.graph.domain.node.NodeIdentity;

public interface GraphDeserializer
{
    public Graph fromJson(String json);

    public SIFDeserializer asSif();

    public static interface SIFDeserializer
    {
        public Graph from(String sif);

        public Graph from(InputStream inputStream);

        public Graph from(File file);

        public SIFDeserializer withNodeDeserializer(Function<String, NodeIdentity> nodeDeserializer);
    }
}