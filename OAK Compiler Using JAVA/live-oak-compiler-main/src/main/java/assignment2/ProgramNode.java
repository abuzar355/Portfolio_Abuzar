package assignment2;

import java.util.ArrayList;

public class ProgramNode extends NodeATS {
    ArrayList<NodeMethodDecl> children;

    public ProgramNode(){
        typeOfNode = "Program";
    }
}