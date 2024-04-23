package assignment2;

import java.util.ArrayList;

public class BlockNode extends NodeATS {
    ArrayList<NodeATS> stmts;
    NodeLoopStmt loop;
    
    public BlockNode(){
        typeOfNode = "Block";
    }
}
