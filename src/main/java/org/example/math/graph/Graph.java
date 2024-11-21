package org.example.math.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A digraph ("directed graph") consists of a set of {@link #vertices()} together with a set of {@link #edges()}. An
 * edge is an object that has a {@link Edge#start() "start" vertex} and {@link Edge#end() "end" vertex} (not necessarily
 * distinct) which can be thought of as an arrow pointing from start to end.
 *
 * @param <V>
 * @param <E>
 */
public interface Graph<V, E extends Graph.Edge<V>> {

    Set<V> vertices();

    Set<E> edges();

    /**
     * @param vertex a vertex of this graph.
     * @return the (unmodifiable) subset of this graph's {@link #edges()} that start at {@code vertex}.
     * @throws IllegalArgumentException if the vertex is not part of this graph.
     * @see #edgesTo(Object)
     */
    default Set<E> edgesFrom(V vertex) {
        return this.edges()
                   .stream()
                   .filter(e -> Objects.equals(vertex, e.start()))
                   .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @param vertex a vertex of this graph.
     * @return the (unmodifiable) subset of this graph's {@link #edges()} that end at {@code vertex}.
     * @throws IllegalArgumentException if the vertex is not part of this graph.
     * @see #edgesFrom(Object)
     */
    default Set<E> edgesTo(V vertex) {
        return this.edges()
                   .stream()
                   .filter(e -> Objects.equals(vertex, e.end()))
                   .collect(Collectors.toUnmodifiableSet());
    }

    default boolean isEmpty() {
        return this.edges().isEmpty() && this.vertices().isEmpty();
    }

    /**
     * Two Graphs are equal iff their {@link #vertices()} and {@link #edges()} are equal.
     */
    @Override
    boolean equals(Object o);

    /**
     * The hashCode of a graph is equal to {@code Objects.hash(vertices,edges)}
     */
    @Override
    int hashCode();

    //region factory methods
    @SafeVarargs
    static <V, E extends Edge<V>> Graph<V, E> of(E... edges) {
        return of(ImmutableSequencedSet.empty(), ImmutableSequencedSet.of(edges));
    }

    static <V, E extends Edge<V>> Graph<V, E> of(Set<E> edges) {
        return of(Set.of(), edges);
    }

    @SafeVarargs
    static <V, E extends Edge<V>> Graph<V, E> of(Set<V> vertices, E... edges) {
        return of(vertices, Set.of(edges));
    }

    @SafeVarargs
    static <V, E extends Edge<V>> Graph<V, E> of(SequencedSet<V> vertices, E... edges) {
        return of(ImmutableSequencedSet.copyOf(vertices), ImmutableSequencedSet.of(edges));
    }

    static <V, E extends Edge<V>> Graph<V, E> of(Set<V> vertices, Set<E> edges) {
        Set<V> allVertices = new HashSet<>(vertices);
        for (E e : edges) {
            allVertices.add(e.start());
            allVertices.add(e.end());
        }
        return new ImmutableGraph<>(allVertices, edges);
    }

    static <V, E extends Edge<V>> SequencedGraph<V, E> of(SequencedSet<V> vertices, SequencedSet<E> edges) {
        SequencedSet<V> allVertices = new LinkedHashSet<>(vertices);
        for (E e : edges) {
            allVertices.add(e.start());
            allVertices.add(e.end());
        }
        return new ImmutableSequencedGraph<>(allVertices, edges);
    }

    static <V, E extends Edge<V>> Graph<V, E> copyOf(Graph<V, E> graph) {
        if (graph instanceof ImmutableGraph || graph instanceof ImmutableSequencedGraph) {
            return graph;
        }
        return new ImmutableGraph<>(graph.vertices(), graph.edges());
    }

    static <V, E extends Edge<V>> SequencedGraph<V, E> copyOf(SequencedGraph<V, E> graph) {
        if (graph instanceof ImmutableSequencedGraph) {
            return graph;
        }
        return new ImmutableSequencedGraph<>(graph.vertices(), graph.edges());
    }
    //endregion

    /**
     * Base class for all edges in {@link Graph}s.
     *
     * @param <V> the type of vertices.
     */
    class Edge<V> {
        private final V start;
        private final V end;

        public Edge(V start, V end) {
            this.start = start;
            this.end = end;
        }

        public V start() {
            return start;
        }

        public V end() {
            return end;
        }

        /**
         * @return a string representation of the edge.
         * @implNote the default implementation returns {@code start().toString() + innerString() + end().toString()}.
         * Subclasses should override the {@link #innerString()} method if they want to provide more information about
         * the edge (like its "weight" or "colour") in its string representation.
         */
        @Override
        public String toString() {
            return start() + innerString() + end();
        }

        /**
         * The default implementation of {@link #toString()} returns
         * {@code start().toString() + innerString() + end().toString()}.
         * <p>
         * Subclasses should override this method if they want to provide more information about the edge (like its
         * "weight" or "colour") in its string representation.
         * <p>
         * This method is also used in {@link Path#toString()}.
         *
         * @implNote the default implementation just returns the constant "=&gt;".
         */
        protected String innerString() {
            return "=>";
        }

        /**
         * The default implementation considers two edges if they are identical. In particular this means that by
         * default it is allowed to have arbitrarily many different edges between the same two vertices.
         */
        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    class Path<V, E extends Edge<V>> {
        private final List<E> edges;
        private final V end;

        /**
         * Constructs a new {@link Path} of <i>non-zero</i> {@link #length()} from its edges.
         *
         * @param edges the path's edges. Must neither be nor contain {@code null}. Each one must be connected to the
         *              next.
         * @throws NullPointerException     if the given list is or contains {@code null}.
         * @throws IllegalArgumentException if the given edges are not connected.
         */
        public Path(List<E> edges) {
            this(edges, edges.getLast().end());
        }

        /**
         * Constructs a new Path of {@link #length()} <i>zero</i> whose {@link #start()} and {@link #end()} will be the
         * same as the given vertex.
         *
         * @param vertex the vertex.
         * @throws NullPointerException if the given vertex is {@code null}.
         */
        public Path(V vertex) {
            this(List.of(), vertex);
        }

        /**
         * Constructs a new path from its list of edges or - if the given list is empty - from the single given vertex.
         *
         * @param edges the list of edges. Must neither be nor contain {@code null}.Each one must be connected to the
         *              next.
         * @param end   the last vertex in the path which may be the only vertex if the path is of zero
         *              {@link #length()}. Must not be {@code null}.
         * @throws NullPointerException     if the given list is or contains {@code null} or if the given vertex is
         *                                  {@code null}.
         * @throws IllegalArgumentException if the given edges are not connected or if the given list is non-empty and
         *                                  the given vertex is not the equal to the {@link Edge#end() end vertex} of
         *                                  the last edge.
         */
        protected Path(List<E> edges, V end) {
            this.edges = List.copyOf(edges);
            this.end = Objects.requireNonNull(end);
            validate(this.edges, this.end);
        }

        private static <V, E extends Edge<V>> void validate(List<E> edges, V end) {
            if (edges.isEmpty()) {
                return;
            }
            final Iterator<E> iter = edges.iterator();
            E lastEdge = iter.next();
            while (iter.hasNext()) {
                E edge = iter.next();
                if (!Objects.equals(lastEdge.end(), edge.start())) {
                    throw new IllegalArgumentException(lastEdge + " is not connected to " + edge);
                }
                lastEdge = edge;
            }
            if (!lastEdge.end().equals(end)) {
                throw new IllegalArgumentException(
                        "The given end vertex " + end + " must be equal to the end vertex of the last given edge "
                        + lastEdge);
            }
        }

        public V start() {
            return edges.isEmpty() ? end : edges.getFirst().start();
        }

        public V end() {
            return end;
        }

        public List<E> edges() {
            return edges;
        }

        public List<V> vertices() {
            return Stream.concat(edges.stream().map(Edge::start), Stream.of(end)).toList();
        }

        public int length() {
            return edges.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Path[");
            for (E e : edges) {
                sb.append(e.start()).append(e.innerString());
            }
            sb.append(end).append(']');
            return sb.toString();
        }

        /**
         * Two paths are equal iff their start and end vertices are equal and all edges in between are equal.
         */
        @Override
        public final boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof Path<?, ?> that && this.edges.equals(that.edges) && this.end.equals(that.end);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(edges, end);
        }
    }
}
