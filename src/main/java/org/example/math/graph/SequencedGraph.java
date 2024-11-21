package org.example.math.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.stream.Collectors;

/**
 * A {@link Graph} with a well-defined iteration order for both vertices and edges.
 *
 * @param <V>
 * @param <E>
 */
public interface SequencedGraph<V, E extends Graph.Edge<V>> extends Graph<V, E> {
    @Override
    SequencedSet<V> vertices();

    @Override
    SequencedSet<E> edges();

    @Override
    default SequencedSet<E> edgesFrom(V vertex) {
        Objects.requireNonNull(vertex);
        return this.edges()
                   .stream()
                   .filter(e -> vertex.equals(e.start()))
                   .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    default SequencedSet<E> edgesTo(V vertex) {
        Objects.requireNonNull(vertex);
        return this.edges()
                   .stream()
                   .filter(e -> vertex.equals(e.end()))
                   .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
