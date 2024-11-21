package org.example.math.graph.iteration;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.math.graph.Graph;
import org.example.math.graph.Graph.Edge;

/**
 * An iterator that visits the edges of a graph in a breadth-first traversal.
 *
 * @param <V> the type of vertices
 * @param <E> the type of edges
 */
public final class BreadthFirstIterator<V, E extends Edge<V>> extends GraphIterator<V, E> {
    private final MarkedQueue<Edge<V>> queue;
    private int currentDepth;

    /**
     * Constructs a new Iterator of the given directed graph starting with the given set of roots. The iteration depth
     * is unrestricted.
     *
     * @param graph a directed graph. Must not be {@code null}.
     * @param roots a set of vertices of the graph. Must neither be nor contain {@code null}.
     * @throws NullPointerException     if the graph of the set of roots or any of its elements is {@code null}.
     * @throws IllegalArgumentException if the given set is not contained in the graph's vertices.
     */
    public BreadthFirstIterator(Graph<V, E> graph, Set<V> roots) {
        this(graph, roots, Integer.MAX_VALUE);
    }

    /**
     * Constructs a new Iterator of the given directed graph starting with the given set of roots that iterates only to
     * the given depth.
     *
     * @param graph        a directed graph. Must not be {@code null}.
     * @param roots        a set of vertices of the graph. Must neither be nor contain {@code null}.
     * @param allowedDepth a maximum depth to which the new iterator will iterate. Must be non-negative. The given roots
     *                     are considered of depth zero.
     * @throws NullPointerException     if the graph or the set of roots or any of its elements is {@code null}.
     * @throws IllegalArgumentException if the given set is not contained in the graph's vertices or
     *                                  {@code allowedDepth} is negative.
     */
    public BreadthFirstIterator(Graph<V, E> graph, Set<V> roots, int allowedDepth) {
        super(graph, roots, allowedDepth);
        this.currentDepth = -1;

        this.queue = roots.stream() // implicit NPE
                          .map(SyntheticEdge::new) // throws NPE if a root is null
                          .collect(Collectors.toCollection(() -> new MarkedQueue<>(new ArrayDeque<>())));
        this.queue.markElement(0);

        normalizeQueue();
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    private void normalizeQueue() {
        Edge<V> e;
        while (null != (e = queue.peek())) {
            if (visitedVertices.contains(e.end())) {
                // found edge to previous vertex
                queue.remove();
            } else {
                // found edge to new vertex
                return;
            }
        }
    }

    @Override
    public V next() {
        boolean isNewLayer = queue.getMark() == 0;
        V v = super.next(); // calls nextEdge()
        if (isNewLayer) {
            currentDepth++;
            queue.markElement(queue.size());
        }
        if (currentDepth < allowedDepth) {
            // We may have visited a lot of other vertices since we enqueued the edge that led us to v.
            // The normalizing step in hasNext() would get rid of them, but only after they moved up to the head of the
            // queue. So we do an additional check here to not keep edges in memory that we already know we're not
            // going to use.
            graph.edgesFrom(v).stream().filter(e -> !visitedVertices.contains(e.end())).forEach(queue::add);
        }
        normalizeQueue();

        return v;
    }

    @Override
    protected Edge<V> nextEdge() {
        return queue.remove();
    }

    @Override
    public int depth() {
        if (currentDepth < 0) {
            throw new IllegalStateException();
        }
        return currentDepth;
    }

    @Override
    public int maxDepth() {
        return depth();
    }

    /**
     * A queue that keeps track of one the position of its elements.
     *
     * @param <E>
     */
    private static class MarkedQueue<E> extends AbstractQueue<E> {
        private final Queue<E> delegate;
        private int mark;

        private MarkedQueue(Queue<E> delegate) {
            this.delegate = delegate;
            this.mark = -1;
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<E> i = delegate.iterator();
            return new Iterator<>() {
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    cursor++;
                    return i.next();
                }

                @Override
                public void remove() {
                    i.remove(); // throws if unsupported
                    if (cursor < mark) {
                        mark--;
                    } else if (cursor == mark) {
                        mark = -1;
                    }
                    cursor--;
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean offer(E e) {
            return delegate.offer(e);
        }

        @Override
        public E poll() {
            final E e = delegate.poll();
            mark--;
            return e;
        }

        @Override
        public E peek() {
            return delegate.peek();
        }

        /**
         * @return the current position of the marked element or a negative number if the marked element has already
         * left the queue.
         */
        public int getMark() {
            return mark;
        }

        public void markElement(int i) {
            this.mark = i;
        }
    }
}
