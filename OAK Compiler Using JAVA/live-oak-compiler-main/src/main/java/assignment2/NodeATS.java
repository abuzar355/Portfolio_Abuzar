package assignment2;

import java.util.Random;

public class NodeATS {
    NodeATS parent;
    String typeOfNode;
    int id = assignId();

    public int assignId(){
        Random rand = new Random(); //instance of random class
        int upperbound = 2500;
        int int_random = rand.nextInt(upperbound); 

        return int_random;
    }
}
