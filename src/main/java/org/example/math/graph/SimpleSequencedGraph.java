package org.example.math.graph;

import java.util.SequencedSet;

class SimpleSequencedGraph<V, E extends Graph.Edge<V>> extends SimpleGraph<V, E>
        implements SequencedGraph<V, E> {
    public SimpleSequencedGraph(SequencedSet<V> vertices, SequencedSet<E> edges) {
        super(vertices, edges);
    }

    @Override
    public SequencedSet<V> vertices() {
        return (SequencedSet<V>) super.vertices();
    }

    @Override
    public SequencedSet<E> edges() {
        return (SequencedSet<E>) super.edges();
    }
}
