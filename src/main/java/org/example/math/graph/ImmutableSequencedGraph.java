package org.example.math.graph;

import java.util.SequencedSet;

public final class ImmutableSequencedGraph<V, E extends Graph.Edge<V>> extends SimpleSequencedGraph<V, E> {

    public ImmutableSequencedGraph(SequencedSet<V> vertices, SequencedSet<E> edges) {
        super(ImmutableSequencedSet.copyOf(vertices), ImmutableSequencedSet.copyOf(edges));
    }
}
