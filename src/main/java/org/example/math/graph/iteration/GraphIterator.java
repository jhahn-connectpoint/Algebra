package org.example.math.graph.iteration;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;

import org.example.math.graph.Graph;
import org.example.math.graph.Graphs;
import org.example.math.graph.RootedGraph;
import org.example.math.graph.SequencedGraph;

abstract sealed class GraphIterator<V, E extends Graph.Edge<V>> implements Iterator<V>
        permits BreadthFirstIterator, DepthFirstIterator {
    protected final Graph<V, E> graph;
    protected final int allowedDepth;

    protected final SequencedSet<V> visitedVertices;
    private final SequencedSet<V> forestRoots;
    private final SequencedSet<E> visitedEdges;
    private final RootedGraph<V, E> forestView;

    protected GraphIterator(Graph<V, E> graph, Set<V> roots, int allowedDepth) {
        this.graph = Objects.requireNonNull(graph);
        if (!graph.vertices().containsAll(roots)) {
            throw new IllegalArgumentException("Set of roots must be contained in the graph");
        }

        if (allowedDepth < 0) {
            throw new IllegalArgumentException("Allowed depth must be at least 0");
        }
        this.allowedDepth = allowedDepth;

        this.forestRoots = new LinkedHashSet<>();
        final int n = graph.vertices().size();
        this.visitedVertices = LinkedHashSet.newLinkedHashSet(n);
        this.visitedEdges = LinkedHashSet.newLinkedHashSet(Math.max(0, n - 1));

        this.forestView = new RootedGraph<>(
                Graphs.unmodifiableGraph(visitedVertices, visitedEdges),
                Collections.unmodifiableSequencedSet(forestRoots));
    }

    /**
     * Returns a {@link RootedGraph} {@link RootedGraph#graph() containing} a {@link SequencedGraph} that represents the
     * current progress of the iteration.
     * <ul>
     *     <li>Its {@link Graph#vertices() set of vertices} is the subset of the original graph's vertices that
     *     have already been visited by this iterator (in the order they have been visited).</li>
     *     <li>For each vertex it contains, the {@link Graph#edges() set of edges} contains at most one incoming
     *     edge to that vertex: The first edge coming from any previously visited vertex (in the order the edges have
     *     been visited). A vertex that was originally given as a root to the constructor of this iterator, may also
     *     have no incoming edges.
     *     <li>The {@link RootedGraph#roots() set of roots} is the subset of the root vertices originally given to the
     *     constructor containing exactly those vertices that were already visited by this iterator and do not have
     *     incoming edges from previously visited roots (in the order they have been visited).
     * </ul>
     * In particular the returned graph is a disjoint union of cycle-free graphs, i.e. trees.
     * <p>
     * After the iteration has finished, the returned graph's set of vertices will contain exactly those vertices
     * that are reachable from one of the root vertices originally given to the constructor.
     * <p>
     * The returned graph is an unmodifiable view of the iteration progress, i.e. the set of vertices, set of edges and
     * set of roots are all unmodifiable views. They cannot be updated other than by calling {@link #next()} which can
     * grow these sets by one element at most.
     *
     * @return An unmodifiable view of the iteration progress.
     */
    public RootedGraph<V, E> spanningForest() {
        return forestView;
    }

    @Override
    public V next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final Graph.Edge<V> e = nextEdge();
        markAsVisited(e);
        return e.end();
    }

    @SuppressWarnings("unchecked")
    private void markAsVisited(Graph.Edge<V> e) {
        final V v = e.end();
        visitedVertices.add(v);
        if (e instanceof GraphIterator.SyntheticEdge<V>) {
            forestRoots.add(v);
        } else {
            // unchecked cast is okay, because the only edges that can be returned by the two implementations of
            // nextEdge() are either of type SyntheticEdge or instances of E
            visitedEdges.add((E) e);
        }
    }

    protected abstract Graph.Edge<V> nextEdge();

    /**
     * @return the current depth of the iteration, i.e. the distance of the vertex last returned by {@link #next()} from
     * one of the roots.
     * @throws IllegalStateException if {@link #next()} has not been called yet.
     */
    public abstract int depth();

    /**
     * @return the maximal depth of the iteration so far.
     * @throws IllegalStateException if {@link #next()} has not been called yet.
     */
    public abstract int maxDepth();

    protected static class SyntheticEdge<V> extends Graph.Edge<V> {
        public SyntheticEdge(V end) {
            super(null, Objects.requireNonNull(end));
        }
    }

}
