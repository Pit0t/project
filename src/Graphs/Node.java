package Graphs;

public class Node
{
    public int data;
    public int index;
    public int row;
    public int col;
    public Node pathZero;
    public Node pathOne;


    public Node()
    {
        data = 0;
        index = 0;
        row = 0;
        col = 0;
        pathZero = null;
        pathOne = null;
    }

    public Node(int data, int row, int col)
    {
        this.data = data;
        this.row = row;
        this.col = col;
        pathZero = null;
        pathOne = null;
    }

    public Node(int d, int i)
    {
        this.data = d;
        this.index = i;
        pathZero = null;
        pathOne = null;
    }

    public Node(int d,int index, Node pathZero, Node pathOne)
    {
        this.data = d;
        this.index = index;
        this.pathZero = pathZero;
        this.pathOne = pathOne;
    }
}
