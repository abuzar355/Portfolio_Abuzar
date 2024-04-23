package assignment2;

public class BoolNode extends NodeATS {
    int value;
    String type;

    public BoolNode(String bool){
        typeOfNode = "Bool";
        type = "bool";
        if (bool.equals("true")){
            this.value = 1;
        }
        else{
            this.value = 0;
        }
    }
}
