package assignment2;

import java.util.ArrayList;

public class MethodNode extends NodeATS {
    String name;
    ArrayList<NodeATS> actuals;

    public MethodNode(String name, ArrayList<NodeATS> actuals){
        typeOfNode = "Method";
        this.name = name;
        this.actuals = actuals;
    }
}
