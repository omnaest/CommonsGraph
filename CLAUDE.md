# CommonsGraph

Graph data structure library (`org.omnaest.utils.graph`). 54 classes covering graph construction, traversal, routing, serialization, and export formats.

## Build

```cmd
mvn clean install
mvn test -Dtest=MyTestClass#myMethod
```

## Architecture

Static facade `GraphUtils` creates a `GraphBuilder`. The `Graph` interface (extending `Streamable<Node>`) is the central type. Nodes are identified by `NodeIdentity` (string-keyed). All public types are interfaces in `domain`; implementations are in `internal` and never referenced by callers.

| Package | What lives here |
|---|---|
| `graph` | `GraphUtils` facade |
| `graph.domain` | `Graph`, `GraphBuilder`, `GraphDeserializer`, `GraphSerializer` interfaces |
| `graph.domain.attributes` | `Tag`, `Attribute` — node/edge metadata |
| `graph.domain.edge` | `Edge`, `Edges`, `TraversedEdge`, `TraversedEdges` |
| `graph.domain.node` | `Node`, `Nodes`, `NodeIdentity` |
| `graph.domain.traversal` | `Traversal`, `TraversalRoutes`, `Route`, `Routes`, `RouteAndTraversalControl` |
| `graph.domain.traversal.hierarchy` | `Hierarchy`, `HierarchicalNode` |
| `graph.internal` | `GraphImpl`, `GraphBuilderImpl`, `GraphResolver` |
| `graph.internal.data` | Index data structures |
| `graph.internal.router` | `GraphRouter`, routing strategies (BFS) |
| `graph.internal.serialization` | JSON serialization support |
| `graph.layout` | `GraphLayoutUtils` facade, `LayeredGraphLayout` — domain-free layered (Sugiyama-family) layout engine (plan-97 slice S1) |
| `graph.layout.domain` | `LayoutGraph`, `LayoutGraphBuilder`, `LayoutResult` interfaces; `Size`, `Point`, `Rectangle`, `LayoutNodeId`, `LayoutEdgeId`, `LayoutNode`, `LayoutEdge`, `LayoutOptions`, `LayoutDirection` value types |
| `graph.layout.internal` | Algorithm phases (cycle removal, longest-path layering, dummy-node insertion, barycentric crossing minimization, priority-method x/y assignment, self-loop routing) over a per-call mutable `LayoutModel`/`WorkNode`/`WorkEdge` working model |

## Key classes

- **`GraphUtils`** — entry point: `GraphUtils.builder()` and `GraphUtils.newCachedGraph(cache, consumer)` (JSON-backed caching)
- **`Graph`** — find nodes, stream nodes, get edges, traverse, route
- **`Node`** — holds `NodeIdentity`, `Tag`s, outgoing/incoming `Edges`
- **`GraphBuilder`** — add nodes and edges; edge identity via `EdgeIdentity`
- **`GraphRouter`** / **`BreadthFirstRoutingStrategy`** — find routes between nodes
- **`PlantUmlUtils`** / **`SIFUtils`** — export to PlantUML and SIF formats

## Code style

- No Lombok — all classes hand-written
- No logging in compile scope (CommonsLog is test-only)
- All domain types are interfaces; `internal` holds implementations
- Graph is backed by `ConcurrentHashMap` by default; can be swapped for a repository-backed store

## Dependencies (compile scope)

- `CommonsLangAndIO` — IO/stream utilities
- `CommonsJSONAndXML` — graph serialization to/from JSON

Test scope: `CommonsTest`, `CommonsLog`.
