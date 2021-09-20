package org.omnaest.utils.graph.domain;

import org.omnaest.utils.graph.internal.GraphImpl;

public class GraphDeserializerImpl implements GraphDeserializer
{
    @Override
    public Graph fromJson(String json)
    {
        return GraphImpl.fromJson(json);
    }
}