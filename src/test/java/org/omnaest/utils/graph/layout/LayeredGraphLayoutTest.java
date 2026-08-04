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
import java.util.Arrays;
import java.util.Collections;
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

    // AC-S1-1 (plan-99, relaxed - corrective round 1 / D7): anti-parallel edges must not share a corridor OUTSIDE a bounded
    // terminal stub at each endpoint - the walking skeleton
    @Test
    public void testAntiParallelEdgesShareNoCorridorOutsideTerminalStubs() throws Exception
    {
        LayoutGraph graph = buildAntiParallelGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        List<Point> waypoints1 = result.getEdgeWaypoints(LayoutEdgeId.of("e1"));
        List<Point> waypoints2 = result.getEdgeWaypoints(LayoutEdgeId.of("e2"));
        assertFalse("anti-parallel e1 (a->b) and e2 (b->a) must not share an axis-parallel corridor outside their terminal stubs: "
                    + waypoints1 + " vs " + waypoints2, shareAxisParallelCorridorOutsideTerminalStubs(waypoints1, waypoints2));
    }

    // S1-AC5 / AC-S1-2 (plan-99, relaxed - corrective round 1 / D7): parallel edges must not share a corridor OUTSIDE a
    // bounded terminal stub at each endpoint either - replaces the vacuous size()>=2-only assertion
    @Test
    public void testMultiEdgeBetweenSamePairBothPresentWithWaypoints() throws Exception
    {
        LayoutGraph graph = buildParallelGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        List<Point> waypoints1 = result.getEdgeWaypoints(LayoutEdgeId.of("e1"));
        List<Point> waypoints2 = result.getEdgeWaypoints(LayoutEdgeId.of("e2"));
        assertTrue(waypoints1.size() >= 2);
        assertTrue(waypoints2.size() >= 2);
        assertFalse("parallel e1 (a->b) and e2 (a->b) must not share an axis-parallel corridor outside their terminal stubs: "
                    + waypoints1 + " vs " + waypoints2, shareAxisParallelCorridorOutsideTerminalStubs(waypoints1, waypoints2));
    }

    // Vacuousness guard for the relaxed AC-S1-1/AC-S1-2 assertion (plan-99 D7): with edgeSeparation forced to 0.0, every
    // bundle's lane offset collapses back to exactly 0.0 (offset(k) scales with edgeSeparation regardless of lane count),
    // reproducing the pre-fix coincident-corridor defect verbatim. shareAxisParallelCorridorOutsideTerminalStubs must still
    // flag that as a corridor collision - proving the terminal-stub exclusion narrows the checked region without ever being
    // able to swallow a genuine, full-length coincidence. This is the executable form of "re-confirm red-first" for the
    // relaxed criterion.
    @Test
    public void testRelaxedCorridorDisjointnessAssertionStillFailsOnNoOffsetBaseline() throws Exception
    {
        LayoutOptions noOffset = LayoutOptions.builder()
                                              .edgeSeparation(0.0)
                                              .build();

        LayoutResult antiParallelResult = GraphLayoutUtils.newLayeredLayout(noOffset)
                                                          .apply(buildAntiParallelGraph());
        List<Point> antiParallelWaypoints1 = antiParallelResult.getEdgeWaypoints(LayoutEdgeId.of("e1"));
        List<Point> antiParallelWaypoints2 = antiParallelResult.getEdgeWaypoints(LayoutEdgeId.of("e2"));
        // e2 (b->a) is read back in ITS OWN original direction (reversal is invisible to callers, see
        // testCycleTerminatesAndWaypointsReadFromSourceToTarget), so its public waypoint order is the reverse of e1's - still
        // the same geometric corridor, just walked from the opposite end
        assertGeometricallyCoincident("anti-parallel e1/e2 with edgeSeparation=0.0", antiParallelWaypoints1, antiParallelWaypoints2);
        assertTrue("relaxed assertion must still flag a fully coincident (no-offset) anti-parallel pair as sharing a corridor",
                   shareAxisParallelCorridorOutsideTerminalStubs(antiParallelWaypoints1, antiParallelWaypoints2));

        LayoutResult parallelResult = GraphLayoutUtils.newLayeredLayout(noOffset)
                                                      .apply(buildParallelGraph());
        List<Point> parallelWaypoints1 = parallelResult.getEdgeWaypoints(LayoutEdgeId.of("e1"));
        List<Point> parallelWaypoints2 = parallelResult.getEdgeWaypoints(LayoutEdgeId.of("e2"));
        assertGeometricallyCoincident("parallel e1/e2 with edgeSeparation=0.0", parallelWaypoints1, parallelWaypoints2);
        assertTrue("relaxed assertion must still flag a fully coincident (no-offset) parallel pair as sharing a corridor",
                   shareAxisParallelCorridorOutsideTerminalStubs(parallelWaypoints1, parallelWaypoints2));
    }

    /**
     * Sanity helper for {@link #testRelaxedCorridorDisjointnessAssertionStillFailsOnNoOffsetBaseline()}: two polylines are
     * genuinely coincident if they are equal either forwards or reversed - an anti-parallel pair's public waypoint order
     * differs (each edge is read back in its own original direction) even though the underlying corridor is identical.
     */
    private static void assertGeometricallyCoincident(String label, List<Point> waypoints1, List<Point> waypoints2)
    {
        List<Point> reversedWaypoints2 = new ArrayList<>(waypoints2);
        Collections.reverse(reversedWaypoints2);
        assertTrue("sanity: " + label + " must be genuinely coincident (forwards or reversed): " + waypoints1 + " vs " + waypoints2,
                   waypoints1.equals(waypoints2) || waypoints1.equals(reversedWaypoints2));
    }

    // AC-S1-9 (plan-99, new - corrective round 1): the endpoint approach is perpendicular to the border - for every
    // genuinely bundled edge (lane offset != 0) the last two waypoints share u and differ in v, and symmetrically the first
    // two. This is what keeps ArrowheadBuilder's axis - derived from exactly the last two waypoints - pointing INTO the
    // node instead of lying flat along the border. Scoped like AC-S1-6 (testBundleWaypointsAxisAligned): the detour
    // mechanism guarantees this unconditionally for any offset != 0 edge, regardless of the baseline's own diagonality,
    // because the first/second and last/second-last points are each derived purely from their OWN border point, never from
    // the far end. A genuinely unbundled (offset == 0) edge instead reproduces its pre-change baseline verbatim (AC-S1-5),
    // which is perpendicular only where the baseline itself already is - true of every graph used here except
    // buildWideSampleGraph's naturally diagonal edges, a pre-existing engine property unrelated to this mechanism, exactly
    // as AC-S1-6 already establishes.
    @Test
    public void testEndpointApproachIsPerpendicularToBorder() throws Exception
    {
        // bundled: offset != 0, the detour mechanism enforces this unconditionally
        LayoutGraph antiParallelGraph = buildAntiParallelGraph();
        LayoutResult antiParallelResult = GraphLayoutUtils.newLayeredLayout()
                                                          .apply(antiParallelGraph);
        assertEndpointApproachPerpendicularForAllEdges(antiParallelGraph, antiParallelResult);

        LayoutGraph parallelGraph = buildParallelGraph();
        LayoutResult parallelResult = GraphLayoutUtils.newLayeredLayout()
                                                      .apply(parallelGraph);
        assertEndpointApproachPerpendicularForAllEdges(parallelGraph, parallelResult);

        LayoutGraph bundleGraph = buildWideSampleGraphWithBundle();
        LayoutResult bundleResult = GraphLayoutUtils.newLayeredLayout()
                                                    .apply(bundleGraph);
        for (LayoutEdgeId bundledEdgeId : Arrays.asList(LayoutEdgeId.of("n1-n4"), LayoutEdgeId.of("n1-n4-dup")))
        {
            assertEndpointApproachPerpendicular(bundledEdgeId.toString(), bundleResult.getEdgeWaypoints(bundledEdgeId));
        }

        // genuinely unbundled (offset == 0, baseline reproduced verbatim, AC-S1-5): perpendicular here because the baseline
        // itself is u-aligned (single node per layer on both sides), not because of the detour mechanism
        LayoutGraph chainGraph = buildChainGraph();
        LayoutResult chainResult = GraphLayoutUtils.newLayeredLayout()
                                                   .apply(chainGraph);
        assertEndpointApproachPerpendicularForAllEdges(chainGraph, chainResult);
    }

    // AC-S1-3 (plan-99): endpoints are pinned - identical to the edgeSeparation=0.0 baseline regardless of separation
    @Test
    public void testBundleEndpointsPinnedRegardlessOfEdgeSeparation() throws Exception
    {
        assertEndpointsPinnedAcrossSeparation(buildAntiParallelGraph());
        assertEndpointsPinnedAcrossSeparation(buildParallelGraph());
    }

    // AC-S1-4 (plan-99): no waypoint of a bundled edge may lie strictly inside any node's rectangle
    @Test
    public void testBundleWaypointsDoNotIntrudeIntoAnyNodeRectangle() throws Exception
    {
        LayoutGraph graph = buildWideSampleGraphWithBundle();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        for (LayoutEdge edge : graph.getEdges())
        {
            List<Point> waypoints = result.getEdgeWaypoints(edge.getId());
            for (Point point : waypoints)
            {
                for (LayoutNode node : graph.getNodes())
                {
                    Rectangle rect = result.getNodeBounds(node.getId());
                    assertFalse("waypoint " + point + " of edge " + edge.getId() + " lies strictly inside node " + node.getId() + "'s rectangle " + rect,
                                liesStrictlyInside(rect, point));
                }
            }
        }
    }

    // AC-S1-5 (plan-99): a bundle of one is byte-identical to the pre-change baseline, captured before any production edit
    @Test
    public void testEdgeSeparationBundleOfOneReproducesPreChangeBaseline() throws Exception
    {
        LayoutGraph graph = buildWideSampleGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        assertWaypointsEqual(Arrays.asList(Point.of(150.0, 35.0), Point.of(35.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n1-n4")));
        assertWaypointsEqual(Arrays.asList(Point.of(150.0, 35.0), Point.of(265.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n1-n6")));
        assertWaypointsEqual(Arrays.asList(Point.of(35.0, 35.0), Point.of(35.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n2-n4")));
        assertWaypointsEqual(Arrays.asList(Point.of(35.0, 35.0), Point.of(150.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n2-n5")));
        assertWaypointsEqual(Arrays.asList(Point.of(265.0, 35.0), Point.of(150.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n3-n5")));
        assertWaypointsEqual(Arrays.asList(Point.of(265.0, 35.0), Point.of(265.0, 95.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n3-n6")));
        assertWaypointsEqual(Arrays.asList(Point.of(35.0, 130.0), Point.of(92.5, 190.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n4-n7")));
        assertWaypointsEqual(Arrays.asList(Point.of(150.0, 130.0), Point.of(92.5, 190.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n5-n7")));
        assertWaypointsEqual(Arrays.asList(Point.of(150.0, 130.0), Point.of(207.5, 190.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n5-n8")));
        assertWaypointsEqual(Arrays.asList(Point.of(265.0, 130.0), Point.of(207.5, 190.0)), result.getEdgeWaypoints(LayoutEdgeId.of("n6-n8")));
    }

    // AC-S1-6 (plan-99): every segment produced by the corridor-separation mechanism itself is axis-aligned. Scoped to the
    // bundled edges (and to graphs where nodes are naturally u-aligned) rather than to every edge of an arbitrary graph:
    // XCoordinateAssignmentPhase does not guarantee a simple edge's source/target centres line up (e.g. plain n2-n4 in
    // buildWideSampleGraphWithBundle is already diagonal on unmodified code, for reasons unrelated to this defect), so
    // asserting it there would be testing a pre-existing, unrelated engine property rather than this mechanism's own output.
    @Test
    public void testBundleWaypointsAxisAligned() throws Exception
    {
        LayoutGraph bundleGraph = buildWideSampleGraphWithBundle();
        LayoutResult bundleResult = GraphLayoutUtils.newLayeredLayout()
                                                    .apply(bundleGraph);
        for (LayoutEdgeId bundledEdgeId : Arrays.asList(LayoutEdgeId.of("n1-n4"), LayoutEdgeId.of("n1-n4-dup")))
        {
            assertPolylineAxisAligned(bundledEdgeId.toString(), bundleResult.getEdgeWaypoints(bundledEdgeId));
        }

        LayoutGraph antiParallelGraph = buildAntiParallelGraph();
        LayoutResult antiParallelResult = GraphLayoutUtils.newLayeredLayout()
                                                          .apply(antiParallelGraph);
        for (LayoutEdge edge : antiParallelGraph.getEdges())
        {
            assertPolylineAxisAligned(edge.getId()
                                          .toString(),
                                      antiParallelResult.getEdgeWaypoints(edge.getId()));
        }

        LayoutGraph parallelGraph = buildParallelGraph();
        LayoutResult parallelResult = GraphLayoutUtils.newLayeredLayout()
                                                      .apply(parallelGraph);
        for (LayoutEdge edge : parallelGraph.getEdges())
        {
            assertPolylineAxisAligned(edge.getId()
                                          .toString(),
                                      parallelResult.getEdgeWaypoints(edge.getId()));
        }
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

    // AC-2-1 (plan-100, walking skeleton): a node with two self-loops yields two polylines that are not equal and whose
    // detour rectangles do not overlap. Byte-identical today (F5) - must be demonstrated red before green.
    @Test
    public void testMultiSelfLoopWaypointsAreDistinctAndNonOverlapping() throws Exception
    {
        LayoutGraph graph = buildMultiSelfLoopGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        List<Point> waypoints1 = result.getEdgeWaypoints(LayoutEdgeId.of("loop1"));
        List<Point> waypoints2 = result.getEdgeWaypoints(LayoutEdgeId.of("loop2"));

        assertFalse("two self-loops on the same node must not produce byte-identical waypoints: " + waypoints1 + " vs " + waypoints2,
                    waypoints1.equals(waypoints2));

        Rectangle rect1 = boundingRectangle(waypoints1);
        Rectangle rect2 = boundingRectangle(waypoints2);
        assertFalse("self-loop detour rectangles must not overlap: " + rect1 + " vs " + rect2, rect1.overlaps(rect2));
    }

    // AC-2-2 (plan-100): for a node with exactly one self-loop, the emitted waypoints equal the pre-change baseline exactly.
    // Baseline captured BEFORE any production edit by manual derivation from the pre-change algorithm and confirmed green
    // pre-edit; the literal values below are that captured baseline, asserted unchanged after the multi-loop change.
    @Test
    public void testSingleSelfLoopWaypointsByteIdenticalToPreChangeBaseline() throws Exception
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

        assertWaypointsEqual(Arrays.asList(Point.of(60.0, 7.5), Point.of(100.0, 7.5), Point.of(100.0, 22.5), Point.of(60.0, 22.5)),
                             result.getEdgeWaypoints(loopId));
    }

    // AC-2-3 (plan-100): with N loops on a node, the outermost loop still clears the nearest neighbouring node's rectangle
    // by at least selfLoopReserve, and no waypoint of any loop lies strictly inside any node's rectangle.
    @Test
    public void testMultiSelfLoopOutermostClearsNeighbourAndNoIntrusion() throws Exception
    {
        LayoutGraph graph = buildMultiSelfLoopGraph();

        LayoutResult result = GraphLayoutUtils.newLayeredLayout()
                                              .apply(graph);

        // loops are ordered by LayoutEdgeId natural order ("loop1" < "loop2"), so loop2 is the outermost
        List<Point> outermostWaypoints = result.getEdgeWaypoints(LayoutEdgeId.of("loop2"));
        double outermostMaxX = outermostWaypoints.stream()
                                                 .mapToDouble(Point::getX)
                                                 .max()
                                                 .getAsDouble();

        Rectangle neighbourRect = result.getNodeBounds(LayoutNodeId.of("y"));
        double gap = neighbourRect.getX() - outermostMaxX;
        assertTrue("outermost self-loop must clear the nearest neighbouring node by at least selfLoopReserve, was " + gap,
                   gap >= LayoutOptions.defaults()
                                       .getSelfLoopReserve()
                          - EPS);

        for (LayoutEdgeId loopId : Arrays.asList(LayoutEdgeId.of("loop1"), LayoutEdgeId.of("loop2")))
        {
            List<Point> waypoints = result.getEdgeWaypoints(loopId);
            for (Point point : waypoints)
            {
                for (LayoutNode node : graph.getNodes())
                {
                    Rectangle rect = result.getNodeBounds(node.getId());
                    assertFalse("waypoint " + point + " of self-loop " + loopId + " lies strictly inside node " + node.getId() + "'s rectangle " + rect,
                                liesStrictlyInside(rect, point));
                }
            }
        }
    }

    /**
     * Two self-loops "loop1"/"loop2" on node x, plus an unconnected neighbour node y in the same layer - plan-100 AC-2-1/
     * AC-2-3/AC-2-6.
     */
    private static LayoutGraph buildMultiSelfLoopGraph()
    {
        LayoutNodeId x = LayoutNodeId.of("x");
        LayoutNodeId y = LayoutNodeId.of("y");
        return GraphLayoutUtils.newLayoutGraph()
                               .addNode(x, Size.of(60, 40))
                               .addNode(y, Size.of(60, 40))
                               .addEdge(LayoutEdgeId.of("loop1"), x, x)
                               .addEdge(LayoutEdgeId.of("loop2"), x, x)
                               .build();
    }

    private static Rectangle boundingRectangle(List<Point> waypoints)
    {
        double minX = waypoints.stream()
                               .mapToDouble(Point::getX)
                               .min()
                               .getAsDouble();
        double minY = waypoints.stream()
                               .mapToDouble(Point::getY)
                               .min()
                               .getAsDouble();
        double maxX = waypoints.stream()
                               .mapToDouble(Point::getX)
                               .max()
                               .getAsDouble();
        double maxY = waypoints.stream()
                               .mapToDouble(Point::getY)
                               .max()
                               .getAsDouble();
        return Rectangle.of(minX, minY, maxX - minX, maxY - minY);
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

    // S1-AC10 / AC-S1-7 (plan-99): must still hold with a bundle present in the graph. AC-2-6 (plan-100): must also still
    // hold with multi-loop nodes present.
    @Test
    public void testDeterminismAcrossIndependentlyBuiltGraphs() throws Exception
    {
        LayoutGraph graph1 = buildWideSampleGraphWithBundle();
        LayoutGraph graph2 = buildWideSampleGraphWithBundle();
        assertTrue("test setup: independently built graphs must not share identity", graph1 != graph2);

        LayoutResult result1 = GraphLayoutUtils.newLayeredLayout()
                                               .apply(graph1);
        LayoutResult result2 = GraphLayoutUtils.newLayeredLayout()
                                               .apply(graph2);

        String rendering1 = canonicalRender(graph1, result1);
        String rendering2 = canonicalRender(graph2, result2);
        assertEquals(rendering1, rendering2);

        LayoutGraph multiLoopGraph1 = buildMultiSelfLoopGraph();
        LayoutGraph multiLoopGraph2 = buildMultiSelfLoopGraph();
        assertTrue("test setup: independently built multi-loop graphs must not share identity", multiLoopGraph1 != multiLoopGraph2);

        LayoutResult multiLoopResult1 = GraphLayoutUtils.newLayeredLayout()
                                                        .apply(multiLoopGraph1);
        LayoutResult multiLoopResult2 = GraphLayoutUtils.newLayeredLayout()
                                                        .apply(multiLoopGraph2);

        assertEquals(canonicalRender(multiLoopGraph1, multiLoopResult1), canonicalRender(multiLoopGraph2, multiLoopResult2));
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

    /**
     * Anti-parallel bundle of size 2: e1 (a->b) and e2 (b->a). After {@link org.omnaest.utils.graph.layout.internal.CycleRemovalPhase}
     * both resolve to layoutFrom=a, layoutTo=b, forming one bundle - plan-99 AC-S1-1.
     */
    private static LayoutGraph buildAntiParallelGraph()
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        return GraphLayoutUtils.newLayoutGraph()
                               .addNode(a, Size.of(80, 40))
                               .addNode(b, Size.of(80, 40))
                               .addEdge(LayoutEdgeId.of("e1"), a, b)
                               .addEdge(LayoutEdgeId.of("e2"), b, a)
                               .build();
    }

    /**
     * Parallel bundle of size 2: e1 (a->b) and e2 (a->b) - plan-99 AC-S1-2.
     */
    private static LayoutGraph buildParallelGraph()
    {
        LayoutNodeId a = LayoutNodeId.of("a");
        LayoutNodeId b = LayoutNodeId.of("b");
        return GraphLayoutUtils.newLayoutGraph()
                               .addNode(a, Size.of(80, 40))
                               .addNode(b, Size.of(80, 40))
                               .addEdge(LayoutEdgeId.of("e1"), a, b)
                               .addEdge(LayoutEdgeId.of("e2"), a, b)
                               .build();
    }

    /**
     * Same as {@link #buildWideSampleGraph()} plus one extra parallel edge n1->n4, forming a bundle of size 2 alongside
     * several nodes per layer - plan-99 AC-S1-4/AC-S1-7.
     */
    private static LayoutGraph buildWideSampleGraphWithBundle()
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
        builder.addEdge(LayoutEdgeId.of("n1-n4-dup"), LayoutNodeId.of("n1"), LayoutNodeId.of("n4"));
        return builder.build();
    }

    private static void assertEndpointsPinnedAcrossSeparation(LayoutGraph graph)
    {
        LayoutResult separated = GraphLayoutUtils.newLayeredLayout()
                                                 .apply(graph);
        LayoutResult zeroSeparation = GraphLayoutUtils.newLayeredLayout(LayoutOptions.builder()
                                                                                     .edgeSeparation(0.0)
                                                                                     .build())
                                                      .apply(graph);

        for (LayoutEdgeId id : Arrays.asList(LayoutEdgeId.of("e1"), LayoutEdgeId.of("e2")))
        {
            List<Point> withSeparation = separated.getEdgeWaypoints(id);
            List<Point> withoutSeparation = zeroSeparation.getEdgeWaypoints(id);

            Point firstExpected = withoutSeparation.get(0);
            Point firstActual = withSeparation.get(0);
            assertEquals("first waypoint x for " + id, firstExpected.getX(), firstActual.getX(), EPS);
            assertEquals("first waypoint y for " + id, firstExpected.getY(), firstActual.getY(), EPS);

            Point lastExpected = withoutSeparation.get(withoutSeparation.size() - 1);
            Point lastActual = withSeparation.get(withSeparation.size() - 1);
            assertEquals("last waypoint x for " + id, lastExpected.getX(), lastActual.getX(), EPS);
            assertEquals("last waypoint y for " + id, lastExpected.getY(), lastActual.getY(), EPS);
        }
    }

    private static boolean liesOnBorder(Rectangle rect, Point point)
    {
        boolean onVerticalEdge = (approxEquals(point.getX(), rect.getX()) || approxEquals(point.getX(), rect.getRight()))
                                 && point.getY() >= rect.getY() - EPS && point.getY() <= rect.getBottom() + EPS;
        boolean onHorizontalEdge = (approxEquals(point.getY(), rect.getY()) || approxEquals(point.getY(), rect.getBottom()))
                                   && point.getX() >= rect.getX() - EPS && point.getX() <= rect.getRight() + EPS;
        return onVerticalEdge || onHorizontalEdge;
    }

    private static boolean liesStrictlyInside(Rectangle rect, Point point)
    {
        return point.getX() > rect.getX() + EPS && point.getX() < rect.getRight() - EPS && point.getY() > rect.getY() + EPS
               && point.getY() < rect.getBottom() - EPS;
    }

    /**
     * True if the two polylines share an axis-parallel VERTICAL corridor (same x, overlapping y extent) OUTSIDE a bounded
     * terminal stub at each endpoint - plan-99 AC-S1-1/AC-S1-2's relaxed corridor-disjointness property (D7). The terminal
     * 30% of each polyline's own v-span at each end is clipped away before comparing: generous relative to the engine's
     * actual inset (a small fixed fraction of the layer-separation gap - see EdgeWaypointBuilderPhase), so it can only ever
     * shrink the checked region, never hide a genuine collision - a fully coincident (un-separated) pair still coincides
     * throughout its own untrimmed middle, which is exactly what
     * {@link #testRelaxedCorridorDisjointnessAssertionStillFailsOnNoOffsetBaseline()} proves executably.
     */
    private static boolean shareAxisParallelCorridorOutsideTerminalStubs(List<Point> waypoints1, List<Point> waypoints2)
    {
        for (double[] segment1 : verticalSegmentsClippedOfTerminalStubs(waypoints1))
        {
            for (double[] segment2 : verticalSegmentsClippedOfTerminalStubs(waypoints2))
            {
                if (approxEquals(segment1[0], segment2[0]) && overlapLength(segment1[1], segment1[2], segment2[1], segment2[2]) > EPS)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static final double TERMINAL_STUB_FRACTION = 0.30;

    private static List<double[]> verticalSegmentsClippedOfTerminalStubs(List<Point> waypoints)
    {
        double spanStart = waypoints.get(0)
                                    .getY();
        double spanEnd = waypoints.get(waypoints.size() - 1)
                                  .getY();
        double stub = Math.abs(spanEnd - spanStart) * TERMINAL_STUB_FRACTION;
        double loV = Math.min(spanStart, spanEnd) + stub;
        double hiV = Math.max(spanStart, spanEnd) - stub;

        List<double[]> clipped = new ArrayList<>();
        for (double[] segment : verticalSegments(waypoints))
        {
            double clippedMin = Math.max(segment[1], loV);
            double clippedMax = Math.min(segment[2], hiV);
            if (clippedMax - clippedMin > EPS)
            {
                clipped.add(new double[] {segment[0], clippedMin, clippedMax});
            }
        }
        return clipped;
    }

    private static void assertEndpointApproachPerpendicularForAllEdges(LayoutGraph graph, LayoutResult result)
    {
        for (LayoutEdge edge : graph.getEdges())
        {
            assertEndpointApproachPerpendicular(edge.getId()
                                                    .toString(),
                                                result.getEdgeWaypoints(edge.getId()));
        }
    }

    private static void assertEndpointApproachPerpendicular(String label, List<Point> waypoints)
    {
        assertTrue(label + ": needs at least 2 waypoints to assert endpoint approach", waypoints.size() >= 2);

        Point first = waypoints.get(0);
        Point second = waypoints.get(1);
        assertEquals(label + ": first two waypoints must share u", first.getX(), second.getX(), EPS);
        assertTrue(label + ": first two waypoints must differ in v", Math.abs(first.getY() - second.getY()) > EPS);

        Point last = waypoints.get(waypoints.size() - 1);
        Point secondLast = waypoints.get(waypoints.size() - 2);
        assertEquals(label + ": last two waypoints must share u", last.getX(), secondLast.getX(), EPS);
        assertTrue(label + ": last two waypoints must differ in v", Math.abs(last.getY() - secondLast.getY()) > EPS);
    }

    private static List<double[]> verticalSegments(List<Point> waypoints)
    {
        List<double[]> segments = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++)
        {
            Point p1 = waypoints.get(i);
            Point p2 = waypoints.get(i + 1);
            if (approxEquals(p1.getX(), p2.getX()))
            {
                segments.add(new double[] {p1.getX(), Math.min(p1.getY(), p2.getY()), Math.max(p1.getY(), p2.getY())});
            }
        }
        return segments;
    }

    private static double overlapLength(double aMin, double aMax, double bMin, double bMax)
    {
        return Math.min(aMax, bMax) - Math.max(aMin, bMin);
    }

    private static void assertWaypointsEqual(List<Point> expected, List<Point> actual)
    {
        assertEquals("waypoint count", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            assertEquals("waypoint " + i + " x", expected.get(i)
                                                         .getX(),
                         actual.get(i)
                               .getX(),
                         EPS);
            assertEquals("waypoint " + i + " y", expected.get(i)
                                                         .getY(),
                         actual.get(i)
                               .getY(),
                         EPS);
        }
    }

    private static void assertPolylineAxisAligned(String label, List<Point> waypoints)
    {
        for (int i = 0; i < waypoints.size() - 1; i++)
        {
            Point p1 = waypoints.get(i);
            Point p2 = waypoints.get(i + 1);
            boolean axisAligned = approxEquals(p1.getX(), p2.getX()) || approxEquals(p1.getY(), p2.getY());
            assertTrue(label + " segment " + i + " is not axis-aligned: " + p1 + " -> " + p2, axisAligned);
        }
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
