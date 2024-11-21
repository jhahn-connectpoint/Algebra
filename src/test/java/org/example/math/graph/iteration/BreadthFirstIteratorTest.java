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

class BreadthFirstIteratorTest {

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
        Collections.addAll(ROOTS, 0, 1, 2);


        //@formatter:off
        //                     Depth
        // 0          1  2     0
        // |             |
        // v             v
        // 3---+         4     1
        // |   |
        // v   v
        // 5   6-+-+           2
        // |   | | |
        // v   v v v
        // 7   8 9 10          3
        //@formatter:on
        TREE_EDGES = new LinkedHashSet<>();
        Collections.addAll(TREE_EDGES, new Edge<>(0, 3), new Edge<>(3, 5), new Edge<>(3, 6), new Edge<>(5, 7),
                new Edge<>(6, 8), new Edge<>(6, 9), new Edge<>(6, 10), new Edge<>(2, 4));
        FOREST = new ImmutableSequencedGraph<>(VERTICES, TREE_EDGES);

        //@formatter:off
        //                         Depth
        // 0              1  2     0
        // |                 |
        // v                 v
        // 3-----+           4     1
        // |     |
        // v     v
        // 5  +--6--+--+           2
        // |  |  |  |  |
        // v  |  v  v  v
        // 7<-+  8<-9  10          3
        //@formatter:on
        ACYCLIC_EDGES = new LinkedHashSet<>(TREE_EDGES);
        Collections.addAll(ACYCLIC_EDGES, new Edge<>(6, 7), new Edge<>(9, 8));
        ACYCLIC_GRAPH = new ImmutableSequencedGraph<>(VERTICES, ACYCLIC_EDGES);

        //@formatter:off
        //                          Depth
        // 0<------------+ 1  2     0
        // |             |    |
        // v             |    v
        // 3-----+       |    4     1
        // |     |       |
        // v     v       |
        // 5  +--6--+--+-+          2
        // |  |  |  |  |
        // v  |  v  v  v
        // 7<-+  8<-9  10           3
        //@formatter:on
        CYCLIC_EDGES = new LinkedHashSet<>(ACYCLIC_EDGES);
        Collections.addAll(CYCLIC_EDGES, new Edge<>(6, 0));
        CYCLIC_GRAPH = new ImmutableSequencedGraph<>(VERTICES, CYCLIC_EDGES);
    }

    public static Stream<Arguments> graphs() {
        return Stream.of(arguments(FOREST), arguments(ACYCLIC_GRAPH), arguments(CYCLIC_GRAPH));
    }

    @Nested
    class TestConstructor {
        @Test
        void givenNonRoot_thenThrowIAE() {
            assertThatIllegalArgumentException().isThrownBy(() -> new BreadthFirstIterator<>(FOREST, Set.of(4711)));
        }

        @Test
        void givenNegativeDepth_thenThrowIAE() {
            assertThatIllegalArgumentException().isThrownBy(() -> new BreadthFirstIterator<>(FOREST, ROOTS, -1));
        }
    }

    @Nested
    class BasicIterator {
        @Test
        void hasNextShallBeIdempotent() {
            var iterator = new BreadthFirstIterator<>(FOREST, ROOTS);
            while (iterator.hasNext()) {
                assertThat(iterator.hasNext()).isTrue();
                iterator.next();
            }
            assertThat(iterator.hasNext()).isFalse();
        }

        @Test
        void afterIterationNextShallThrowNSSE() {
            var iterator = new BreadthFirstIterator<>(FOREST, ROOTS);
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
            var iterator = new BreadthFirstIterator<>(Graph.of(), Set.of());
            assertThat(iterator.hasNext()).isFalse();
            assertThat(iterator.spanningForest().vertices()).isEmpty();
            assertThat(iterator.spanningForest().edges()).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("org.example.math.graph.iteration.BreadthFirstIteratorTest#graphs")
        void shallVisitAllVerticesInOrder(Graph<Integer, Edge<Integer>> graph) {
            var iterator = new BreadthFirstIterator<>(graph, ROOTS);

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        @ParameterizedTest
        @MethodSource("org.example.math.graph.iteration.BreadthFirstIteratorTest#graphs")
        void shallCountLevels(Graph<Integer, Edge<Integer>> graph) {
            var iterator = new BreadthFirstIterator<>(graph, ROOTS);

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
            assertThat(depths).containsExactly(0, 0, 0, 1, 1, 2, 2, 3, 3, 3, 3);
        }
    }

    @Nested
    class PartialIteration {

        @Test
        void earlyOut() {
            var iterator = new BreadthFirstIterator<>(FOREST, ROOTS);
            int count = 0;
            while (iterator.hasNext() && count < 5) {
                iterator.next();
                count++;
            }

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4);
        }

        @Test
        void iteratingFromNonRoot() {
            var iterator = new BreadthFirstIterator<>(FOREST, Set.of(3));

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(3, 5, 6, 7, 8, 9, 10);
        }

        @Test
        void levelRestrictedIteration() {
            var iterator = new BreadthFirstIterator<>(FOREST, ROOTS, 2);
            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6);
            assertThat(iterator.maxDepth()).isEqualTo(2);
        }

        @Test
        void shallDetectBackEdges() {
            var iterator = new BreadthFirstIterator<>(ACYCLIC_GRAPH, ROOTS);

            iterator.forEachRemaining(v -> {});

            assertThat(iterator.spanningForest().vertices()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }
}