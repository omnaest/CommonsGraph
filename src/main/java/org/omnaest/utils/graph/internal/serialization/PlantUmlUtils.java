package org.omnaest.utils.graph.internal.serialization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.omnaest.utils.FileUtils;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.StringUtils.StringTextBuilder;

public class PlantUmlUtils
{
    public static PlantUmlDocumentBuilder builder()
    {
        return new PlantUmlDocumentBuilder()
        {

            @Override
            public ObjectGraphDocumentBuilder forObjectGraphDocument()
            {
                return new ObjectGraphDocumentBuilder()
                {
                    private List<String>                     objects          = new ArrayList<>();
                    private Map<String, Map<String, String>> objectIdToFields = new HashMap<>();
                    private Map<String, Set<String>>         assocations      = new LinkedHashMap<>();

                    @Override
                    public ObjectGraphDocumentBuilder addObject(String id)
                    {
                        this.objects.add(id);
                        return this;
                    }

                    @Override
                    public ObjectGraphDocumentBuilder addObjectWithFields(String id, Map<String, String> fields)
                    {
                        if (fields != null)
                        {
                            this.objectIdToFields.put(id, fields);
                        }
                        else
                        {
                            this.objectIdToFields.remove(id);
                        }
                        return this.addObject(id);
                    }

                    @Override
                    public ObjectGraphDocumentBuilder addAssociation(String sourceId, String targetId)
                    {
                        this.assocations.computeIfAbsent(sourceId, id -> new LinkedHashSet<>())
                                        .add(targetId);
                        return this;
                    }

                    @Override
                    public ObjectGraphDocument build()
                    {
                        StringTextBuilder builder = StringUtils.builder()
                                                               .withLineSeparator("\n")
                                                               .addLine("@startuml");
                        this.objects.forEach(object ->
                        {
                            builder.addLine("object " + object);

                            if (this.objectIdToFields.containsKey(object))
                            {
                                Optional.ofNullable(this.objectIdToFields.get(object))
                                        .orElse(Collections.emptyMap())
                                        .forEach((field, value) -> builder.addLine(object + " : " + field + " = " + value));
                            }
                        });

                        this.assocations.forEach((source, targets) -> targets.forEach(target -> builder.addLine(source + " --> " + target)));
                        builder.addLine("@enduml");

                        String documentBody = builder.toString();

                        return new ObjectGraphDocument()
                        {
                            @Override
                            public String get()
                            {
                                return documentBody;
                            }

                            @Override
                            public ObjectGraphDocument writeInto(File file)
                            {
                                FileUtils.toConsumer(file)
                                         .accept(this.get());
                                return this;
                            }
                        };
                    }
                };
            }
        };
    }

    public static interface PlantUmlDocumentBuilder
    {
        public ObjectGraphDocumentBuilder forObjectGraphDocument();
    }

    public static interface ObjectGraphDocumentBuilder
    {
        public ObjectGraphDocumentBuilder addObject(String id);

        public ObjectGraphDocumentBuilder addObjectWithFields(String id, Map<String, String> fields);

        public ObjectGraphDocumentBuilder addAssociation(String sourceId, String targetId);

        public ObjectGraphDocument build();
    }

    public static interface ObjectGraphDocument extends Supplier<String>
    {
        public ObjectGraphDocument writeInto(File file);
    }
}
