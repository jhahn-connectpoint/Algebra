package org.example.math.graph;

import java.util.Objects;
import java.util.Set;

class SimpleGraph<V, E extends Graph.Edge<V>> implements Graph<V, E> {
    protected final Set<V> vertices;
    protected final Set<E> edges;

    public SimpleGraph(final Set<V> vertices, final Set<E> edges) {
        this.vertices = Objects.requireNonNull(vertices);
        this.edges = Objects.requireNonNull(edges);
    }

    @Override
    public Set<V> vertices() {
        return vertices;
    }

    @Override
    public Set<E> edges() {
        return edges;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof SimpleGraph<?, ?> that && vertices.equals(that.vertices) && edges.equals(that.edges);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(vertices, edges);
    }
}
