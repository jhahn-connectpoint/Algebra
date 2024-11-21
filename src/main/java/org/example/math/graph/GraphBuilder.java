package org.example.math.graph;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

import org.example.math.graph.Graph.Edge;

public class GraphBuilder<V> {
    private final SequencedSet<V> vertices = new LinkedHashSet<>();
    private final SequencedSet<Edge<V>> edges = new LinkedHashSet<>();

    public GraphBuilder<V> addVertex(V vertex) {
        vertices.add(vertex);
        return this;
    }

    public GraphBuilder<V> addVertices(V... vertices) {
        for (V v : vertices) {
            addVertex(v);
        }
        return this;
    }

    public GraphBuilder<V> addVertices(Collection<V> vertices) {
        this.vertices.addAll(vertices);
        return this;
    }

    public GraphBuilder<V> addEdge(Edge<V> edge) {
        vertices.add(edge.start());
        vertices.add(edge.end());
        edges.add(edge);
        return this;
    }

    public GraphBuilder<V> addEdges(Edge<V>... edges) {
        for (Edge<V> edge : edges) {
            addEdge(edge);
        }
        return this;
    }

    public GraphBuilder<V> addEdges(Collection<Edge<V>> edges) {
        edges.forEach(this::addEdge);
        return this;
    }

    public EdgeBuilder addEdgeFrom(V vertex) {
        vertices.add(vertex);
        return new EdgeBuilder(vertex);
    }

    public SequencedGraph<V, Edge<V>> build() {
        return new ImmutableSequencedGraph<>(vertices, edges);
    }

    public class EdgeBuilder {
        private final V vertex;

        private EdgeBuilder(V vertex) {
            this.vertex = vertex;
        }

        public GraphBuilder<V> to(V end) {
            edges.add(new Edge<>(vertex, end));
            return GraphBuilder.this;
        }
    }
}
