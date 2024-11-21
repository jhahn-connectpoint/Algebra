package org.example.math.graph.iteration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Stream;

import org.example.math.graph.Graph;
import org.example.math.graph.Graph.Edge;
import org.example.math.graph.ImmutableSequencedGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DepthFirstIteratorTest {

    private static final SequencedSet<Integer> VERTICES;
    private static final SequencedSet<Integer> ROOTS;
    private static final SequencedSet<Edge<Integer>> TREE_EDGES;
    private static final Graph<Integer, Edge<Integer>> FOREST;
    private static final SequencedSet<Edge<Integer>> ACYCLIC_EDGES;
    private static final Graph<Integer, Edge<Integer>> ACYCLIC_GRAPH;
    private static final SequencedSet<Edge<Integer>> CYCLIC_EDGES;
    private static final Graph<Integer, Edge<Integer>> CYCLIC_GRAPH;

    static {
        VERTICES = new LinkedHashSet<>();
        Collections.addAll(VERTICES, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ROOTS = new LinkedHashSet<>();
        Collections.addAll(ROOTS, 0, 8, 9);


        //@formatter:off
        //                     Depth
        // 0          8  9     0
        // |             |
        // v             v
        // 1---+        10     1
        // |   |
        // v   v
        // 2   4-+-+           2
        // |   | | |
        // v   v v v
        // 3   5 6 7           3
        //@formatter:on
        TREE_EDGES = new LinkedHashSet<>();
        Collections.addAll(TREE_EDGES, new Edge<>(0, 1), new Edge<>(1, 2), new Edge<>(2, 3), new Edge<>(1, 4),
                new Edge<>(4, 5), new Edge<>(4, 6), new Edge<>(4, 7), new Edge<>(9, 10));
        FOREST = new ImmutableSequencedGraph<>(VERTICES, TREE_EDGES);

        //@formatter:off
        //                         Depth
        // 0               8  9    0
        // |                  |
        // v                  v
        // 1-----+           10    1
        // |     |
        // v     v
        // 2  +--4--+--+           2
        // |  |  |  |  |
        // v  |  v  v  v
        // 3<-+  5<-6  7           3
        //@formatter:on
        ACYCLIC_EDGES = new LinkedHashSet<>(TREE_EDGES);
        Collections.addAll(ACYCLIC_EDGES, new Edge<>(4, 3), new Edge<>(6, 5));
        ACYCLIC_GRAPH = new ImmutableSequencedGraph<>(VERTICES, ACYCLIC_EDGES);

        //@formatter:off
        //                         Depth
        // 0<------------+ 8  9    0
        // |             |    |
        // v             |    v
        // 1-----+       |   10    1
        // |     |       |
        // v     v       |
        // 2  +--4--+--+-+         2
        // |  |  |  |  |
        // v  |  v  v  v
        // 3<-+  5<-6  7           3
        //@formatter:on
        CYCLIC_EDGES = new LinkedHashSet<>(ACYCLIC_EDGES);
        Collections.addAll(CYCLIC_EDGES, new Edge<>(4, 0));
        CYCLIC_GRAPH = new ImmutableSequencedGraph<>(VERTICES, CYCLIC_EDGES);
    }

    public static Stream<Arguments> graphs() {
        return Stream.of(arguments(FOREST), arguments(ACYCLIC_GRAPH), arguments(CYCLIC_GRAPH));
    }

    @Nested
    class TestConstructor {
        @Test
        void givenNonRoot_thenThrowIAE() {
            assertThatIllegalArgumentException().isThrownBy(() -> new DepthFirstIterator<>(FOREST, Set.of(4711)));
        }

        @Test
        void givenNegativeDepth_thenThrowIAE() {
            assertThatIllegalArgumentException().isThrownBy(() -> new DepthFirstIterator<>(FOREST, ROOTS, -1));
        }
    }

    @Nested
    class BasicIterator {
        @Test
        void hasNextShallBeIdempotent() {
            var iterator = new DepthFirstIterator<>(FOREST, ROOTS);
            while (iterator.hasNext()) {
                assertThat(iterator.hasNext()).isTrue();
                iterator.next();
            }
            assertThat(iterator.hasNext()).isFalse();
        }

        @Test
        void afterIterationNextShallThrowNSSE() {
            var iterator = new DepthFirstIterator<>(FOREST, ROOTS);
            while (iterator.hasNext()) {
                iterator.next();
            }
            assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
        }
    }

    @Nested
    class CompleteIteration {

        @Test
        void emptyGraph() {
            var iterator = new DepthFirstIterator<>(Graph.of(), Set.of());
            assertThat(iterator.hasNext()).isFalse();
            assertThat(iterator.spanningForest().vertices()).isEmpty();
            assertThat(iterator.spanningForest().edges()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("org.example.math.graph.iteration.DepthFirstIteratorTest#graphs")
        void shallVisitAllVerticesInPreOrder(Graph<Integer, Edge<Integer>> graph) {
            var iterator = new DepthFirstIterator<>(graph, ROOTS);

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        @ParameterizedTest
        @MethodSource("org.example.math.graph.iteration.DepthFirstIteratorTest#graphs")
        void shallCountLevels(Graph<Integer, Edge<Integer>> graph) {
            var iterator = new DepthFirstIterator<>(graph, ROOTS);

            // before iteration: throw
            assertThatIllegalStateException().isThrownBy(iterator::maxDepth);
            assertThatIllegalStateException().isThrownBy(iterator::depth);

            List<Integer> depths = new ArrayList<>();
            while (iterator.hasNext()) {
                iterator.next();
                depths.add(iterator.depth());
            }

            // after iteration: contain correct values
            assertThat(iterator.maxDepth()).isEqualTo(3);
            assertThat(depths).containsExactly(0, 1, 2, 3, 2, 3, 3, 3, 0, 0, 1);
        }

        @ParameterizedTest
        @MethodSource("org.example.math.graph.iteration.DepthFirstIteratorTest#graphs")
        void duplicateRootsShouldBeSkipped(Graph<Integer, Edge<Integer>> graph) {
            var moreRoots = new LinkedHashSet<Integer>();
            Collections.addAll(moreRoots, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            var iterator = new DepthFirstIterator<>(graph, moreRoots);

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().roots()).containsExactly(0, 8, 9);
            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Nested
    class PartialIteration {

        @Test
        void earlyOut() {
            var iterator = new DepthFirstIterator<>(FOREST, ROOTS);
            int count = 0;
            while (iterator.hasNext() && count < 5) {
                iterator.next();
                count++;
            }

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4);
        }

        @Test
        void iteratingFromNonRoot() {
            var iterator = new DepthFirstIterator<>(FOREST, Set.of(1));

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(1, 2, 3, 4, 5, 6, 7);
        }

        @Test
        void levelRestrictedIteration() {
            var iterator = new DepthFirstIterator<>(FOREST, ROOTS, 2);
            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 4, 8, 9, 10);
            assertThat(iterator.maxDepth()).isEqualTo(2);
        }

        @Test
        void shallDetectBackEdges() {
            var iterator = new DepthFirstIterator<>(ACYCLIC_GRAPH, ROOTS);

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }
}