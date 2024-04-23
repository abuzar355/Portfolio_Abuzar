package assignment2;

public class NodeBinop extends NodeATS {
    char op;
    NodeATS left;
    NodeATS right;
    String type; //resulting type

    public NodeBinop(){
        typeOfNode = "Binop";
    }
}
