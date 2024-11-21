package org.example.math.graph.iteration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.math.graph.Graph;
import org.example.math.graph.Graph.Edge;

/**
 * An iterator that visits the edges of a graph in a (pre-order) depth-first traversal.
 *
 * @param <V> the type of vertices
 * @param <E> the type of edges
 */
public final class DepthFirstIterator<V, E extends Edge<V>> extends GraphIterator<V, E> {

    // Invariants:
    // - stack is empty iff iteration is finished.
    // - before that: top of stack is a non-empty queue and the head of the queue is an edge to an unknown vertex.
    private final Deque<Queue<Edge<V>>> stack;
    private int currentDepth;
    private int maxDepth;

    /**
     * Constructs a new Iterator of the given directed graph starting with the given set of roots. The iteration depth
     * is unrestricted.
     *
     * @param graph a directed graph. Must not be {@code null}.
     * @param roots a set of vertices of the graph. Must neither be nor contain {@code null}.
     * @throws NullPointerException     if the graph of the set of roots or any of its elements is {@code null}.
     * @throws IllegalArgumentException if the given set is not contained in the graph's vertices.
     */
    public DepthFirstIterator(Graph<V, E> graph, Set<V> roots) {
        this(graph, roots, Integer.MAX_VALUE);
    }

    /**
     * Constructs a new Iterator of the given directed graph starting with the given set of roots that iterates only to
     * the given depth.
     *
     * @param graph        a directed graph. Must not be {@code null}.
     * @param roots        a set of vertices of the graph. Must neither be nor contain {@code null}.
     * @param allowedDepth a maximum depth to which the new iterator will iterate. Must be non-negative. The given roots
     *                     are considered of depth zero unless they are reachable from a previous root.
     * @throws NullPointerException     if the graph or the set of roots or any of its elements is {@code null}.
     * @throws IllegalArgumentException if the given set is not contained in the graph's vertices or
     *                                  {@code allowedDepth} is negative.
     */
    public DepthFirstIterator(Graph<V, E> graph, Set<V> roots, int allowedDepth) {
        super(graph, roots, allowedDepth);
        this.currentDepth = -1;
        this.maxDepth = 0;

        this.stack = new ArrayDeque<>();
        this.stack.add(roots.stream() // implicit NPE
                            .map(SyntheticEdge::new) // throws NPE if a root is null
                            .collect(Collectors.toCollection(ArrayDeque::new)));
        normalizeStack();
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    private void normalizeStack() {
        Queue<Edge<V>> queue;
        while (null != (queue = stack.peek())) {
            Edge<V> e;
            while (null != (e = queue.peek())) {
                if (visitedVertices.contains(e.end())) {
                    // edge to previous vertex
                    queue.remove();
                } else {
                    // edge to next vertex
                    return;
                }
            }
            stack.pop();
        }
    }

    @Override
    public V next() {
        final V v = super.next(); // calls nextEdge() => may lead stack in unnormalized state with empty queue on top

        currentDepth = stack.size() - 1; // which is important here: the empty queue still counts for the depth.
        maxDepth = Math.max(maxDepth, currentDepth);

        if (currentDepth < allowedDepth) {
            stack.push(new ArrayDeque<>(graph.edgesFrom(v)));
        }
        normalizeStack();

        return v;
    }

    @Override
    protected Edge<V> nextEdge() {
        return stack.getFirst().remove();
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
        if (currentDepth < 0) {
            throw new IllegalStateException();
        }
        return maxDepth;
    }
}
