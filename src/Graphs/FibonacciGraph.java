package Graphs;

public class FibonacciGraph implements NumberGraph {

    public Node[] fibArray;
    public FibonacciGraph(int size)
    {
        fibArray = new Node[size];
        fibArray[0] = new Node(0,0);
        fibArray[1] = new Node(1,1);
        for (int i = 2; i < size; i++)
        {
            int nextVal = (fibArray[i-1].data + fibArray[i-2].data) % 1000000007;
            fibArray[i] = new Node(nextVal,i);
        }
    }


    @Override
    public Node StartPoint() {
        return fibArray[0];
    }

    @Override
    public Node NextPoint(int chooser, Node node) {
        int currentIndex = node.index;
        // chooser can be only 0 or 1!!!
        if  (chooser == 0)
        {
            return fibArray[(currentIndex + 1) % fibArray.length];
        }
        else
        {
            return fibArray[(currentIndex+2) % fibArray.length];
        }

    }
}
