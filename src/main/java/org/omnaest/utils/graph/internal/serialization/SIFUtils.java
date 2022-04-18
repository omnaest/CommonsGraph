package org.omnaest.utils.graph.internal.serialization;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.utils.FileUtils;

/**
 * Supports SIF file format
 * 
 * @author omnaest
 */
public class SIFUtils
{
    public static SIFResourceBuilder builder()
    {
        return new SIFResourceBuilder()
        {
            private String delimiter             = "\t";
            private String delimiterEscapeString = "_";
            private String lineSeparator         = "\n";

            private StringBuilder stringBuilder = new StringBuilder();

            @Override
            public SIFResourceBuilder withDelimiter(String delimiter)
            {
                this.delimiter = delimiter;
                return this;
            }

            @Override
            public SIFResourceBuilder addEdge(String from, String type, String... to)
            {
                this.stringBuilder.append(this.escapeDelimiter(from))
                                  .append(this.delimiter)
                                  .append(StringUtils.defaultIfBlank(this.escapeDelimiter(type), " "))
                                  .append(this.delimiter)
                                  .append(Optional.ofNullable(to)
                                                  .map(Arrays::asList)
                                                  .orElse(Collections.emptyList())
                                                  .stream()
                                                  .map(this::escapeDelimiter)
                                                  .collect(Collectors.joining(this.delimiter)))
                                  .append(this.lineSeparator);
                return this;
            }

            private String escapeDelimiter(String text)
            {
                return StringUtils.replace(text, this.delimiter, this.delimiterEscapeString);
            }

            @Override
            public SIFResource build()
            {
                String sif = this.stringBuilder.toString();
                return new SIFResource()
                {

                    @Override
                    public String get()
                    {
                        return sif;
                    }

                    @Override
                    public String toString()
                    {
                        return this.get();
                    }

                    @Override
                    public SIFResource writeInto(File file)
                    {
                        FileUtils.toConsumer(file)
                                 .accept(this.get());
                        return this;
                    }

                };
            }
        };
    }

    public static interface SIFResourceBuilder
    {
        public SIFResource build();

        public SIFResourceBuilder addEdge(String from, String type, String... to);

        public SIFResourceBuilder withDelimiter(String delimiter);
    }

    public static interface SIFResource extends Supplier<String>
    {

        public SIFResource writeInto(File file);

    }

    public static SIFParser parse()
    {
        return new SIFParser()
        {
            private String delimiter = "\t";

            @Override
            public SIFParser withDelimiter(String delimiter)
            {
                this.delimiter = delimiter;
                return this;
            }

            @Override
            public Stream<SIFEntry> from(String sif)
            {
                return org.omnaest.utils.StringUtils.splitToStreamByLineSeparator(sif)
                                                    .filter(StringUtils::isNotBlank)
                                                    .map(line ->
                                                    {
                                                        String[] tokens = StringUtils.splitPreserveAllTokens(line, this.delimiter);
                                                        String from = tokens.length >= 1 ? tokens[0] : null;
                                                        String to = tokens.length >= 1 ? tokens[2] : null;
                                                        String attribute = tokens.length >= 1 ? tokens[1] : null;
                                                        return new SIFEntry(from, to, attribute);
                                                    });
            }
        };

    }

    public static interface SIFParser
    {
        public SIFParser withDelimiter(String delimiter);

        public Stream<SIFEntry> from(String sif);

    }

    public static class SIFEntry
    {
        private String from;
        private String to;
        private String type;

        public SIFEntry(String from, String to, String type)
        {
            super();
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public String getFrom()
        {
            return this.from;
        }

        public String getTo()
        {
            return this.to;
        }

        public String getType()
        {
            return this.type;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("SIFEntry [from=")
                   .append(this.from)
                   .append(", to=")
                   .append(this.to)
                   .append(", type=")
                   .append(this.type)
                   .append("]");
            return builder.toString();
        }

    }

}
