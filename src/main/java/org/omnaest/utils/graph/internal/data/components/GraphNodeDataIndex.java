package org.omnaest.utils.graph.internal.data.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.utils.graph.domain.GraphBuilder.RepositoryProvider;
import org.omnaest.utils.graph.domain.node.NodeIdentity;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphNodeDataIndex extends AbstractJSONSerializable
{
    @JsonProperty
    private Map<NodeIdentity, NodeData> nodeIdentityToNodeData;

    @JsonCreator
    protected GraphNodeDataIndex()
    {
        this((name, keyType, valueType) -> new ConcurrentHashMap<>());
    }

    public GraphNodeDataIndex(RepositoryProvider repositoryProvider)
    {
        super();
        this.nodeIdentityToNodeData = repositoryProvider.createMap("nodeIdentityToNodeData");
    }

    public static class NodeData
    {
        @JsonProperty
        private Map<String, Object> data = new HashMap<>();

        public Map<String, Object> getData()
        {
            return this.data;
        }

        public NodeData setData(Map<String, Object> data)
        {
            this.data = data;
            return this;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("NodeData [data=")
                   .append(this.data)
                   .append("]");
            return builder.toString();
        }

    }

    public NodeData attachNodeDataToNodeAndGet(NodeIdentity nodeIdentity)
    {
        return this.nodeIdentityToNodeData.computeIfAbsent(nodeIdentity, id -> new NodeData());
    }

    public void updateNodeData(NodeIdentity nodeIdentity, NodeData nodeData)
    {
        this.nodeIdentityToNodeData.put(nodeIdentity, nodeData);
    }

    public Optional<NodeData> getNodeData(NodeIdentity nodeIdentity)
    {
        return Optional.ofNullable(this.nodeIdentityToNodeData.get(nodeIdentity));
    }
}
