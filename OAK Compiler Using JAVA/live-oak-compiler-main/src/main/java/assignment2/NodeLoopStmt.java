package assignment2;

public class NodeLoopStmt extends NodeATS {
    NodeATS condition;
    BlockNode block;
    
    public NodeLoopStmt(){
        typeOfNode = "Loop";
    }
}
