package org.omnaest.utils.graph.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.exception.RuntimeFileNotFoundException;
import org.omnaest.utils.exception.RuntimeIOException;
import org.omnaest.utils.graph.GraphUtils;
import org.omnaest.utils.graph.domain.GraphBuilder.EdgeIdentityWithAttributes;
import org.omnaest.utils.graph.domain.attributes.Tag;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.graph.internal.GraphImpl;
import org.omnaest.utils.graph.internal.serialization.SIFUtils;

public class GraphDeserializerImpl implements GraphDeserializer
{
    @Override
    public Graph fromJson(String json)
    {
        return GraphImpl.fromJson(json);
    }

    @Override
    public SIFDeserializer asSif()
    {
        return new SIFDeserializer()
        {
            private Function<String, NodeIdentity> nodeDeserializer = JSONHelper.deserializer(NodeIdentity.class);

            @Override
            public SIFDeserializer withNodeDeserializer(Function<String, NodeIdentity> nodeDeserializer)
            {
                this.nodeDeserializer = nodeDeserializer;
                return this;
            }

            @Override
            public Graph from(File file)
            {
                try
                {
                    return this.from(new FileInputStream(file));
                }
                catch (FileNotFoundException e)
                {
                    throw new RuntimeFileNotFoundException(e);
                }
            }

            @Override
            public Graph from(InputStream inputStream)
            {
                try
                {
                    return this.from(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
                }
                catch (IOException e)
                {
                    throw new RuntimeIOException(e);
                }
            }

            @Override
            public Graph from(String sif)
            {
                return GraphUtils.builder()
                                 .addEdgesWithAttributes(SIFUtils.parse()
                                                                 .from(sif)
                                                                 .map(entry -> EdgeIdentityWithAttributes.of(this.nodeDeserializer.apply(entry.getFrom()),
                                                                                                            this.nodeDeserializer.apply(entry.getTo()),
                                                                                                            Tag.of(entry.getType())))
                                                                 .collect(Collectors.toList()))
                                 .build();
            }
        };
    }
}