package org.omnaest.utils.graph.internal;

import java.io.IOException;

import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.graph.domain.NodeIdentity;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class NodeIdentityKeyDeserializer extends KeyDeserializer
{
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException
    {
        return JSONHelper.deserializer(NodeIdentity.class)
                         .apply(key);
    }
}