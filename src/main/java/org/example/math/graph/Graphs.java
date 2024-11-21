package org.example.math.graph;

import java.util.Collections;
import java.util.SequencedSet;
import java.util.Set;

public class Graphs {
    private Graphs() {}

    public static <V, E extends Graph.Edge<V>> Graph<V, E> unmodifiableView(Graph<V, E> graph) {
        return unmodifiableGraph(graph.vertices(), graph.edges());
    }

    public static <V, E extends Graph.Edge<V>> Graph<V, E> unmodifiableGraph(Set<V> vertices, Set<E> edges) {
        return new SimpleGraph<>(Collections.unmodifiableSet(vertices), Collections.unmodifiableSet(edges));
    }

    public static <V, E extends Graph.Edge<V>> SequencedGraph<V, E> unmodifiableView(SequencedGraph<V, E> graph) {
        return unmodifiableGraph(graph.vertices(), graph.edges());
    }

    public static <V, E extends Graph.Edge<V>> SequencedGraph<V, E> unmodifiableGraph(SequencedSet<V> vertices,
                                                                                      SequencedSet<E> edges) {
        return new SimpleSequencedGraph<>(Collections.unmodifiableSequencedSet(vertices),
                Collections.unmodifiableSequencedSet(edges));
    }
}
