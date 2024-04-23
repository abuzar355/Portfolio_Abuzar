package assignment2;

public class StmtNode extends NodeATS {
    VarNode var;
    ExprNode expr;
    String value; // to store break, return;

    public StmtNode(VarNode var, ExprNode expr){
        typeOfNode = "Stmt";
        this.var = var;
        this.expr = expr;
    }
}
