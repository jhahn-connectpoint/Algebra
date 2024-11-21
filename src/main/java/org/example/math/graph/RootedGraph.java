package org.example.math.graph;

import java.util.Objects;
import java.util.Set;

/**
 * A {@link Graph} together with a set of distinguished vertices called "roots".
 * <p>
 * Note that this class provides the methods {@link #vertices()} and {@link #edges()}, but does not implement the
 * {@link Graph} interface, because it cannot satisfy the specification of {@link Graph#equals(Object)}.
 *
 * @param graph the graph. Not {@code null}.
 * @param roots its roots. Not {@code null}.
 * @param <V>   the type of vertices
 * @param <E>   the type of edges
 */
public record RootedGraph<V, E extends Graph.Edge<V>>(Graph<V, E> graph, Set<V> roots) {
    public RootedGraph {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(roots);
        if (!graph.vertices().containsAll(roots)) {
            throw new IllegalArgumentException("Roots must be vertices of the graph");
        }
    }

    public Set<V> vertices() {
        return graph.vertices();
    }

    public Set<E> edges() {
        return graph.edges();
    }
}
