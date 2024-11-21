package org.example.math.graph;

import java.util.Set;

public final class ImmutableGraph<V, E extends Graph.Edge<V>> extends SimpleGraph<V, E> {

    public ImmutableGraph(Set<V> vertices, Set<E> edges) {
        super(Set.copyOf(vertices), Set.copyOf(edges));

        for (E edge : this.edges) {
            if (!this.vertices.contains(edge.start()) || !this.vertices.contains(edge.end())) {
                throw new IllegalArgumentException(
                        "Edge " + edge + " does not connect two vertices in " + Set.copyOf(vertices));
            }
        }
    }
}
