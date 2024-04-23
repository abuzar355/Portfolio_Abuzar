package assignment2;

import java.util.ArrayList;

public class NodeMethodDecl extends NodeATS {
    String type;
    String name;
    ArrayList<FormalNode> formals;
    BodyNode body;

    public NodeMethodDecl(String type, String name, ArrayList<FormalNode> formals, BodyNode body){
        typeOfNode = "MethodDecl";
        this.type = type;
        this.name = name;
        this.formals = formals;
        this.body = body;
    }
}
