package assignment2;

public class VarNode extends VarDeclNode{
    Object value;

    public VarNode(String identifier){
        typeOfNode = "Var";
        this.identifier = identifier;
    }
}
