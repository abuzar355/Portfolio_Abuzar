package assignment2;

public class FormalNode extends NodeATS {
    String type;
    String identity;

    public FormalNode(String type, String identifier){
        typeOfNode = "Formal";
        this.type = type;
        this.identity = identifier;
    }
}
