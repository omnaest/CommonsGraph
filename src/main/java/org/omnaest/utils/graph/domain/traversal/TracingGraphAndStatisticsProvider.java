package org.omnaest.utils.graph.domain.traversal;

import java.util.Optional;

import org.omnaest.utils.graph.domain.Graph;
import org.omnaest.utils.graph.domain.traversal.Traversal.VisitedNodesStatistic;

public interface TracingGraphAndStatisticsProvider
{
    public Optional<Graph> getTracingGraph();

    public VisitedNodesStatistic getVisitedNodesStatistic();
}