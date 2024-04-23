package assignment2;
import java.util.ArrayList;

public class BodyNode extends NodeATS {
    ArrayList<VarDeclNode> declVariableList;
    BlockNode child;
    
    public BodyNode(){
        typeOfNode = "Body";
    }
}
