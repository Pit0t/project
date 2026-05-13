package Graphs;

// A directed edge in the Pascal DAG.
// The weight comes from the Fibonacci value at the time this edge was created,
// so the cost of traversing the graph changes dynamically.
public class Edge {

    public final Node source;
    public final Node target;
    public final long weight;

    public Edge(Node source, Node target, long weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "(" + source.row + "," + source.col + ")" + " -> (" + target.row + "," + target.col + ")" + " w=" + weight;
    }
}