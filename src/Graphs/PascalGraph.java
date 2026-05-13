package Graphs;

public class PascalGraph implements NumberGraph {

    @Override
    public Node StartPoint() {
        return new Node(1,0,0);
    }

    @Override
    public Node NextPoint(int chooser, Node node) {
        int currRow = node.row;
        int currCol = node.col;
        int data = node.data;
        int nextRow = node.row + 1;

        if (chooser == 0)
        {
            return new Node((int)(((long)data * nextRow) / (nextRow - currCol)) % 1000000007, nextRow, currCol);
        }
        else
            return new Node((int)(((long)data * nextRow) / (currCol + 1)) % 1000000007, nextRow, currCol+1);

    }
}
