package assignment2;

public class NodeConditionalStmt extends NodeATS {
    NodeATS condition;
    BlockNode if_block;
    BlockNode else_block;
    
    public NodeConditionalStmt(){
        typeOfNode = "Conditional";
    }
}
