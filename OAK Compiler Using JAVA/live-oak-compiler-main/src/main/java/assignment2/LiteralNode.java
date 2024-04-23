package assignment2;

public class LiteralNode extends NodeATS {
    String literal;
    String type; 

    public LiteralNode(String literal){
        typeOfNode = "Literal";
        type = "String";
        this.literal = literal;
    }
}
