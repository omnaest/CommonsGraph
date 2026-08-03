package org.omnaest.utils.graph.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.omnaest.utils.graph.layout.domain.LayoutDirection;
import org.omnaest.utils.graph.layout.domain.LayoutEdge;
import org.omnaest.utils.graph.layout.domain.LayoutEdgeId;
import org.omnaest.utils.graph.layout.domain.LayoutGraph;
import org.omnaest.utils.graph.layout.domain.LayoutGraphBuilder;
import org.omnaest.utils.graph.layout.domain.LayoutNode;
import org.omnaest.utils.graph.layout.domain.LayoutNodeId;
import org.omnaest.utils.graph.layout.domain.LayoutOptions;
import org.omnaest.utils.graph.layout.domain.LayoutResult;
import org.omnaest.utils.graph.layout.domain.Point;
import org.omnaest.utils.graph.layout.domain.Rectangle;
import org.omnaest.utils.graph.layout.domain.Size;

/**
 * Acceptance tests for plan-97 slice S1: the domain-free layered graph layout engine.
 *
 * @see LayeredGraphLayout
 * @author omnaest
 */
public class LayeredGraphLayoutTest
{
    private static final double EPS = 1e-6;

    // S1-AC1
    @Test
    public void testSingleNode() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(100, 50))
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        assertEquals(0, result.getLayerIndex(a));
        Rectangle nodeRect = result.getNodeBounds(a);
        assertEquals(0.0, nodeRect.getX(), EPS);
        assertEquals(0.0, nodeRect.getY(), EPS);
        assertEquals(100.0, nodeRect.getWidth(), EPS);
        assertEquals(50.0, nodeRect.getHeight(), EPS);

        Rectangle bounds = result.getBounds();
        assertEquals(0.0, bounds.getX(), EPS);
        assertEquals(0.0, bounds.getY(), EPS);
        assertEquals(100.0, bounds.getWidth(), EPS);
        assertEquals(50.0, bounds.getHeight(), EPS);
    }

    // S1-AC2
    @Test
    public void testTwoNodesOneEdge() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        LayoutEdgeId edgeId = LayoutEdgeId.of("ab");
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(80, 40))
                                            .addNode(b, Size.of(80, 40))
                                            .addEdge(edgeId, a, b)
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        assertEquals(0, result.getLayerIndex(a));
        assertEquals(1, result.getLayerIndex(b));
        assertTrue(result.getLayerIndex(a) != result.getLayerIndex(b));

        List<Point> waypoints = result.getEdgeWaypoints(edgeId);
        assertTrue(waypoints.size() >= 2);
        assertTrue("first waypoint must lie on A's border", liesOnBorder(result.getNodeBounds(a), waypoints.get(0)));
        assertTrue("last waypoint must lie on B's border", liesOnBorder(result.getNodeBounds(b), waypoints.get(waypoints.size() - 1)));
    }

    // S1-AC3
    @Test
    public void testLongEdgeSpanningLayersInsertsDummyWaypoints() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        LayoutNodeId c = LayoutNodeId.of("c");
        LayoutNodeId d = LayoutNodeId.of("d");
        LayoutEdgeId longEdgeId = LayoutEdgeId.of("long");
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(60, 30))
                                            .addNode(b, Size.of(60, 30))
                                            .addNode(c, Size.of(60, 30))
                                            .addNode(d, Size.of(60, 30))
                                            .addEdge(LayoutEdgeId.of("ab"), a, b)
                                            .addEdge(LayoutEdgeId.of("bc"), b, c)
                                            .addEdge(LayoutEdgeId.of("cd"), c, d)
                                            .addEdge(longEdgeId, a, d)
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        assertEquals(0, result.getLayerIndex(a));
        assertEquals(3, result.getLayerIndex(d));

        List<Point> waypoints = result.getEdgeWaypoints(longEdgeId);
        assertTrue("expected more than 2 waypoints (dummy nodes inserted), got " + waypoints.size(), waypoints.size() > 2);
    }

    // S1-AC4
    @Test
    public void testCycleTerminatesAndWaypointsReadFromSourceToTarget() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        LayoutNodeId c = LayoutNodeId.of("c");
        LayoutEdgeId caEdgeId = LayoutEdgeId.of("ca");
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(60, 30))
                                            .addNode(b, Size.of(60, 30))
                                            .addNode(c, Size.of(60, 30))
                                            .addEdge(LayoutEdgeId.of("ab"), a, b)
                                            .addEdge(LayoutEdgeId.of("bc"), b, c)
                                            .addEdge(caEdgeId, c, a)
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        // terminates and every node received a layer index
        result.getLayerIndex(a);
        result.getLayerIndex(b);
        result.getLayerIndex(c);

        List<Point> waypoints = result.getEdgeWaypoints(caEdgeId);
        assertTrue(waypoints.size() >= 2);
        assertTrue("first waypoint of c->a must lie on C's border (source), reversal must be invisible",
                   liesOnBorder(result.getNodeBounds(c), waypoints.get(0)));
        assertTrue("last waypoint of c->a must lie on A's border (target), reversal must be invisible",
                   liesOnBorder(result.getNodeBounds(a), waypoints.get(waypoints.size() - 1)));
    }

    // S1-AC5
    @Test
    public void testMultiEdgeBetweenSamePairBothPresentWithWaypoints() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        LayoutEdgeId e1 = LayoutEdgeId.of("e1");
        LayoutEdgeId e2 = LayoutEdgeId.of("e2");
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(80, 40))
                                            .addNode(b, Size.of(80, 40))
                                            .addEdge(e1, a, b)
                                            .addEdge(e2, a, b)
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        List<Point> waypoints1 = result.getEdgeWaypoints(e1);
        List<Point> waypoints2 = result.getEdgeWaypoints(e2);
        assertTrue(waypoints1.size() >= 2);
        assertTrue(waypoints2.size() >= 2);
    }

    // S1-AC6
    @Test
    public void testSelfLoopRouteStaysClearAndReservesSpace() throws Exception
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        LayoutEdgeId loopId = LayoutEdgeId.of("loop");
        LayoutOptions options = LayoutOptions.builder()
                                             .selfLoopReserve(40)
                                             .build();
        LayoutGraph graph = GraphLayoutUtils.newLayoutGraph()
                                            .addNode(a, Size.of(60, 30))
                                            .addNode(b, Size.of(60, 30))
                                            .addEdge(loopId, a, a)
                                            .build();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout(options)
                                              .apply(graph);

        List<Point> waypoints = result.getEdgeWaypoints(loopId);
        assertTrue("self-loop route must have at least 4 waypoints", waypoints.size() >= 4);

        Rectangle aRect = result.getNodeBounds(a);
        for (Point point : waypoints)
        {
            assertTrue("self-loop route must stay clear of A's own rectangle", point.getX() >= aRect.getRight() - EPS);
        }

        Rectangle bRect = result.getNodeBounds(b);
        double gap = bRect.getX() - aRect.getRight();
        assertTrue("gap to nearest same-layer neighbour must be >= selfLoopReserve, was " + gap, gap >= options.getSelfLoopReserve() - EPS);
    }

    // S1-AC7
    @Test
    public void testNoTwoNodeRectanglesOverlap() throws Exception
    {
        LayoutGraph graph = buildWideSampleGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        List<LayoutNode> nodes = graph.getNodes();
        for (int i = 0; i < nodes.size(); i++)
        {
            for (int j = i + 1; j < nodes.size(); j++)
            {
                Rectangle r1 = result.getNodeBounds(nodes.get(i)
                                                         .getId());
                Rectangle r2 = result.getNodeBounds(nodes.get(j)
                                                         .getId());
                assertFalse("node rectangles must not overlap: " + r1 + " vs " + r2, r1.overlaps(r2));
            }
        }
    }

    // S1-AC8
    @Test
    public void testBoundsNormalizedAndContainEveryNode() throws Exception
    {
        LayoutGraph graph = buildWideSampleGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        Rectangle bounds = result.getBounds();
        assertEquals(0.0, bounds.getX(), EPS);
        assertEquals(0.0, bounds.getY(), EPS);

        for (LayoutNode node : graph.getNodes())
        {
            Rectangle nodeRect = result.getNodeBounds(node.getId());
            assertTrue(nodeRect.getX() >= bounds.getX() - EPS);
            assertTrue(nodeRect.getY() >= bounds.getY() - EPS);
            assertTrue(nodeRect.getRight() <= bounds.getRight() + EPS);
            assertTrue(nodeRect.getBottom() <= bounds.getBottom() + EPS);
        }
    }

    // S1-AC9
    @Test
    public void testLeftToRightIsTransposedButLayersUnchanged() throws Exception
    {
        LayoutGraph graphTopToBottom = buildChainGraph();
        LayoutGraph graphLeftToRight = buildChainGraph();

        LayoutResult topToBottomResult = GraphLayoutUtils.newLayeredLayout(LayoutOptions.builder()
                                                                                        .direction(LayoutDirection.TOP_TO_BOTTOM)
                                                                                        .build())
                                                         .apply(graphTopToBottom);
        LayoutResult leftToRightResult = GraphLayoutUtils.newLayeredLayout(LayoutOptions.builder()
                                                                                        .direction(LayoutDirection.LEFT_TO_RIGHT)
                                                                                        .build())
                                                         .apply(graphLeftToRight);

        Rectangle tbBounds = topToBottomResult.getBounds();
        Rectangle lrBounds = leftToRightResult.getBounds();
        assertTrue("TOP_TO_BOTTOM layout of a chain should be taller than wide", tbBounds.getHeight() > tbBounds.getWidth());
        assertTrue("LEFT_TO_RIGHT layout of a chain should be wider than tall", lrBounds.getWidth() > lrBounds.getHeight());

        for (LayoutNode node : graphTopToBottom.getNodes())
        {
            assertEquals("layer index must be unchanged across direction", topToBottomResult.getLayerIndex(node.getId()),
                         leftToRightResult.getLayerIndex(node.getId()));
        }
    }

    // S1-AC10
    @Test
    public void testDeterminismAcrossIndependentlyBuiltGraphs() throws Exception
    {
        LayoutGraph graph1 = buildWideSampleGraph();
        LayoutGraph graph2 = buildWideSampleGraph();
        assertTrue("test setup: independently built graphs must not share identity", graph1 != graph2);

        LayoutResult result1 = GraphLayoutUtils.newLayeredLayout()
                                               .apply(graph1);
        LayoutResult result2 = GraphLayoutUtils.newLayeredLayout()
                                               .apply(graph2);

        String rendering1 = canonicalRender(graph1, result1);
        String rendering2 = canonicalRender(graph2, result2);
        assertEquals(rendering1, rendering2);
    }

    // S1-AC11
    @Test
    public void testNoHashMapOrHashSetInMainSources() throws Exception
    {
        Path root = Paths.get("src", "main", "java", "org", "omnaest", "utils", "graph", "layout");
        assertTrue("expected the layout main-source root to exist: " + root.toAbsolutePath(), Files.isDirectory(root));

        List<String> forbiddenTokenHits = new ArrayList<>();
        // word-boundary patterns so "LinkedHashMap"/"LinkedHashSet" (permitted) do not falsely match "HashMap"/"HashSet"
        java.util.regex.Pattern[] forbiddenPatterns = {java.util.regex.Pattern.compile("\\bHashMap\\b"), java.util.regex.Pattern.compile("\\bHashSet\\b"),
                java.util.regex.Pattern.compile("\\bCollectors\\.toMap\\b"), java.util.regex.Pattern.compile("\\bCollectors\\.toSet\\b")};

        try (Stream<Path> files = Files.walk(root))
        {
            List<Path> javaFiles = files.filter(p -> p.toString()
                                                      .endsWith(".java"))
                                        .collect(Collectors.toList());
            for (Path file : javaFiles)
            {
                List<String> lines = Files.readAllLines(file);
                for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++)
                {
                    String line = lines.get(lineNumber);
                    for (java.util.regex.Pattern pattern : forbiddenPatterns)
                    {
                        if (pattern.matcher(line)
                                   .find())
                        {
                            forbiddenTokenHits.add(file + ":" + (lineNumber + 1) + " contains forbidden token matching '" + pattern.pattern() + "'");
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            fail("failed to scan main sources: " + e.getMessage());
        }

        assertTrue("forbidden determinism-hazard tokens found:\n" + String.join("\n", forbiddenTokenHits), forbiddenTokenHits.isEmpty());
    }

    // S1-AC12
    @Test
    public void testApplyDoesNotMutateInput() throws Exception
    {
        LayoutGraph graph = buildWideSampleGraph();
        List<LayoutNode> nodesBefore = new ArrayList<>(graph.getNodes());
        List<LayoutEdge> edgesBefore = new ArrayList<>(graph.getEdges());

        GraphLayoutUtils.newLayeredLayout()
                        .apply(graph);

        assertEquals(nodesBefore, graph.getNodes());
        assertEquals(edgesBefore, graph.getEdges());
    }

    private static LayoutGraph buildChainGraph()
    {
        LayoutGraphBuilder builder = GraphLayoutUtils.newLayoutGraph();
        String[] ids = {"n1", "n2", "n3", "n4", "n5"};
        for (String id : ids)
        {
            builder.addNode(LayoutNodeId.of(id), Size.of(80, 40));
        }
        for (int i = 0; i < ids.length - 1; i++)
        {
            builder.addEdge(LayoutEdgeId.of(ids[i] + "-" + ids[i + 1]), LayoutNodeId.of(ids[i]), LayoutNodeId.of(ids[i + 1]));
        }
        return builder.build();
    }

    /**
     * 8 nodes across 3 layers: n1,n2,n3 -> n4,n5,n6 -> n7,n8
     */
    private static LayoutGraph buildWideSampleGraph()
    {
        LayoutGraphBuilder builder = GraphLayoutUtils.newLayoutGraph();
        for (int i = 1; i <= 8; i++)
        {
            builder.addNode(LayoutNodeId.of("n" + i), Size.of(70, 35));
        }
        addEdge(builder, "n1", "n4");
        addEdge(builder, "n2", "n4");
        addEdge(builder, "n2", "n5");
        addEdge(builder, "n3", "n5");
        addEdge(builder, "n3", "n6");
        addEdge(builder, "n1", "n6");
        addEdge(builder, "n4", "n7");
        addEdge(builder, "n5", "n7");
        addEdge(builder, "n5", "n8");
        addEdge(builder, "n6", "n8");
        return builder.build();
    }

    private static void addEdge(LayoutGraphBuilder builder, String from, String to)
    {
        builder.addEdge(LayoutEdgeId.of(from + "-" + to), LayoutNodeId.of(from), LayoutNodeId.of(to));
    }

    private static boolean liesOnBorder(Rectangle rect, Point point)
    {
        boolean onVerticalEdge = (approxEquals(point.getX(), rect.getX()) || approxEquals(point.getX(), rect.getRight()))
                                 && point.getY() >= rect.getY() - EPS && point.getY() <= rect.getBottom() + EPS;
        boolean onHorizontalEdge = (approxEquals(point.getY(), rect.getY()) || approxEquals(point.getY(), rect.getBottom()))
                                   && point.getX() >= rect.getX() - EPS && point.getX() <= rect.getRight() + EPS;
        return onVerticalEdge || onHorizontalEdge;
    }

    private static boolean approxEquals(double a, double b)
    {
        return Math.abs(a - b) <= EPS;
    }

    private static String canonicalRender(LayoutGraph graph, LayoutResult result)
    {
        StringBuilder builder = new StringBuilder();

        List<LayoutNodeId> nodeIds = graph.getNodes()
                                          .stream()
                                          .map(LayoutNode::getId)
                                          .sorted()
                                          .collect(Collectors.toList());
        for (LayoutNodeId id : nodeIds)
        {
            builder.append("N:")
                   .append(id.getValue())
                   .append("=")
                   .append(result.getNodeBounds(id))
                   .append(";layer=")
                   .append(result.getLayerIndex(id))
                   .append("\n");
        }

        List<LayoutEdgeId> edgeIds = graph.getEdges()
                                          .stream()
                                          .map(LayoutEdge::getId)
                                          .sorted()
                                          .collect(Collectors.toList());
        for (LayoutEdgeId id : edgeIds)
        {
            builder.append("E:")
                   .append(id.getValue())
                   .append("=")
                   .append(result.getEdgeWaypoints(id))
                   .append("\n");
        }

        return builder.toString();
    }

}
