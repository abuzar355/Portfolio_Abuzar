package assignment2;

import edu.utexas.cs.sam.io.SamTokenizer;
import edu.utexas.cs.sam.io.Tokenizer.TokenType;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.ParseException;
import java.util.regex.Pattern;


public class LiveOak3Compiler {

	public static HashMap<String, VarDeclNode> variable_map = new HashMap<String, VarDeclNode>();
	public static HashMap<String, NodeMethodDecl> method_map = new HashMap<String, NodeMethodDecl>();
	public static int var_count = 0;




	public static void main(String[] args) throws Error, Exception{
		String in_file;
		String out_file;
		if (args.length > 1){
			in_file = args[0];
			out_file = args[1];
		}
		else{
			in_file = "C:\\Users\\azar\\Downloads\\lo3-to-sam-tester-public (2)\\lo3-to-sam-tester-public\\src\\test\\resources\\LO-3\\ValidPrograms\\test_16.lo";
			//in_file = "/Users/karandesai/SIMPL/hw2/lo2-to-sam-tester-public/src/test/resources/LO-2/InvalidPrograms/test_20.lo";
			out_file = "h2.sam";
		}

		String pgm = compiler(in_file);
		writeSamfile(pgm, out_file);
	}

	static String compiler(String orgFilePath) throws Error, Exception
	{
		String formattedFileName = orgFilePath.replace(File.separator, "/");
		try{
			//returns SaM code for program in file
			SamTokenizer samTok = new SamTokenizer(orgFilePath, SamTokenizer.TokenizerOptions.PROCESS_STRINGS);
			ProgramNode program = getProgram(samTok);
			if (!validateTree(program)){
				throw new Error("Failed to compile " + formattedFileName);
			}
			String pgm = processTree(program);
			// System.out.println(pgm);
			return pgm;
		}
		catch (Error e){

			System.err.println("Failed to compile " + formattedFileName);
			throw new Error("Failed to compile " + formattedFileName);
		}
		catch (Exception e){
			System.err.println("Failed to compile " + formattedFileName);
			throw new Error("Failed to compile " + formattedFileName);
		}

	}




	static ProgramNode getProgram(SamTokenizer samTok) throws ParseException{
		try{
			ProgramNode prgNode = new ProgramNode();
			ArrayList<NodeMethodDecl> methods = new ArrayList<NodeMethodDecl>();
			prgNode.children = methods;
			while (samTok.peekAtKind() != TokenType.EOF){
				NodeMethodDecl declMethod = getMethod(samTok);
				if (declMethod != null){
					declMethod.parent = prgNode;
					methods.add(declMethod);
				}
				else{
					return null;
				}
			}
			return prgNode;
		}
		catch(ParseException e){
			throw new ParseException("Parse failure", 0);
		}
	}

	static NodeMethodDecl getMethod(SamTokenizer samTok) throws ParseException{
		String type = samTok.getWord();
		TokenType tt = samTok.peekAtKind();
		if (tt != TokenType.WORD){
			throw new ParseException("bad method definition", 0);
		}
		String name = samTok.getWord();
		char c = samTok.getOp(); //eat (
		ArrayList<FormalNode> formals = new ArrayList<FormalNode>();
		while (!samTok.test(')')){
			if (samTok.test(',')){
				samTok.skipToken();
			}
			FormalNode formal = getFormal(samTok);
			if (formal != null){
				formals.add(formal);
				variable_map.put(name + "." + formal.identity, new VarDeclNode(formal.identity, formal.type, name, variable_map.size()+1));
			}
		}
		if (name.equals("main") && formals.size() > 0){
			throw new ParseException("Main should not have formals", 0);
		}
		c = samTok.getOp(); // eat )
		c = samTok.getOp(); // eat { from method
		BodyNode body = getBody(samTok);
		c = samTok.getOp(); // ear } from method
		NodeMethodDecl method = new NodeMethodDecl(type, name, formals, body);

		//set scope for declared variables
		for (int i = 0; i < body.declVariableList.size() ; i++){
			body.declVariableList.get(i).scope = method.name;
			variable_map.put(method.name + "." + body.declVariableList.get(i).identifier, body.declVariableList.get(i));
		}
		var_count = 0;
		//set scope for initialized variables
		for (int i = 0; i < body.child.stmts.size() ; i++){
			NodeATS node = body.child.stmts.get(i);
			if (node.typeOfNode.equals("Stmt")){
				StmtNode stmt = (StmtNode) node;
				if (stmt.var != null){
					stmt.var.scope = method.name;
				}
			}
		}

		method.body.parent = method;
		method_map.put(name, method);
		return method;
	}

	static FormalNode getFormal(SamTokenizer samTok){
		String type = samTok.getWord();
		String identifier = samTok.getWord();
		return new FormalNode(type, identifier);
	}

	static BodyNode getBody(SamTokenizer f) throws ParseException{
		BodyNode body = new BodyNode();
		ArrayList<VarDeclNode> vars = new ArrayList<VarDeclNode>();
		while(!f.test('{')){
			if (f.test('}')){ // missing block
				throw new ParseException("Missing statement block", 0);
			}
			ArrayList<VarDeclNode> new_vars = getVarDecl(f);
			for (int j = 0 ; j < new_vars.size() ; j++){
				vars.add(new_vars.get(j));
			}
		}
		body.declVariableList = vars;
		body.child = getBlock(f);
		body.child.parent = body;

		// add in conditional parents
		setParents(body, body.child);
		return body;
	}

	static ArrayList<VarDeclNode> getVarDecl(SamTokenizer samTok){
		try{
			ArrayList<VarDeclNode> varDeclList = new ArrayList<VarDeclNode>();
			String type = getType(samTok);
			char op;
			while (!samTok.test(';')){
				if (samTok.test(',')){
					op = samTok.getOp(); // eat ,
				}
				String identifier = getIdentifier(samTok);
				VarDeclNode var = new VarDeclNode(identifier, type, null, variable_map.size()+var_count+1);
				varDeclList.add(var);
				var_count++;
			}
			op = samTok.getOp(); // eat ;
			return varDeclList;
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return null;
		}
	}

	static BlockNode getBlock(SamTokenizer samTok) throws ParseException{
		BlockNode block = new BlockNode();
		ArrayList<NodeATS> stmtList = new ArrayList<NodeATS>();
		char op = samTok.getOp(); // eat {
		while (!samTok.test('}')){
			int line = samTok.lineNo();
			NodeATS stmt = getStmt(samTok);
			if (stmt != null){
				if (stmt.typeOfNode.equals("Stmt")){
					StmtNode st = (StmtNode) stmt;
					st.id = line;
					st.parent = block;
					stmtList.add(st);
				}
				else if (stmt.typeOfNode.equals("Conditional")){
					NodeConditionalStmt cond = (NodeConditionalStmt) stmt;
					cond.parent = block;
					stmtList.add(cond);
				}
				else{
					NodeLoopStmt loop = (NodeLoopStmt) stmt;
					loop.parent = block;
					//TODO changed something here
					loop.block.parent = loop;
					stmtList.add(loop);
				}
			}
		}
		block.stmts = stmtList;
		op = samTok.getOp(); // eat }
		return block;
	}

	static NodeATS getStmt(SamTokenizer samTok) throws ParseException{
		if (samTok.test(';')){
			samTok.getOp(); // eat ;
			return null;
		}
		else if (samTok.test("if")){
			NodeConditionalStmt cond = new NodeConditionalStmt();
			int id = samTok.lineNo();
			samTok.getWord(); // eat 'if'
			samTok.getOp(); // eat '('
			cond.condition = getExpr(samTok);
			cond.condition.parent = cond;
			samTok.getOp(); // eat ')'
			cond.if_block = getBlock(samTok);
			cond.if_block.parent = cond;
			samTok.getWord(); // eat 'else'
			cond.else_block = getBlock(samTok);
			cond.else_block.parent = cond;
			cond.id = id;
			return cond;
		}
		else if (samTok.test("while")){
			NodeLoopStmt loop = new NodeLoopStmt();
			int id = samTok.lineNo();
			samTok.getWord(); // eat 'while'
			samTok.getOp(); // eat '('
			loop.condition = getExpr(samTok);
			loop.condition.parent = loop;
			samTok.getOp(); // eat ')'
			loop.block = getBlock(samTok);
			loop.id = id;
			loop.block.loop = loop;
			return loop;
		}
		else if (samTok.test("break")){
			StmtNode stmt = new StmtNode(null, null);
			stmt.value = samTok.getWord();
			char c = samTok.getOp(); // eat ;
			return stmt;
		}
		else if (samTok.test("return")){
			samTok.getWord(); // eat 'return'
			StmtNode stmt = new StmtNode(null, null);
			ExprNode expr = new ExprNode();
			expr = getExpr(samTok);
			stmt.expr = expr;
			expr.parent = stmt;
			stmt.value = "return";
			char c = samTok.getOp(); // eat ;
			return stmt;
		}
		else{
			VarNode var = null;
			StmtNode stmt = null;
			var = getVar(samTok);
			char c = samTok.getOp(); // eat =
			ExprNode expr = new ExprNode();
			expr = getExpr(samTok);
			stmt = new StmtNode(var, expr);
			expr.parent = stmt;
			var.parent = stmt;
			c = samTok.getOp(); // eat ;
			if (c != ';'){
				throw new Error("invalid expression");
			}
			return stmt;
		}
	}

	static ExprNode getExpr(SamTokenizer samTok) throws ParseException{
		try{
			ExprNode expr = new ExprNode();
			if (samTok.peekAtKind() == TokenType.WORD){ // I think this is var
				String word = samTok.getWord();
				if (method_map.containsKey(word) || samTok.test('(')){
					MethodNode method = getMethod(word, samTok);
					expr.childNode = method;
					method.parent = expr;
				}
				else if (word.equals("true") || word.equals("false")){
					expr.childNode = new BoolNode(word);
				}
				else{
					VarNode var = new VarNode(word);
					expr.childNode = var;
				}
				expr.childNode.parent = expr;
				return expr;
			}
			else if (samTok.peekAtKind() == TokenType.STRING){
				expr.childNode = getLiteral(samTok);
				expr.childNode.parent = expr;
				//expr.parent = expr;
				return expr;
			}
			else if (samTok.peekAtKind() == TokenType.INTEGER){
				expr.childNode = getNum(samTok);
				expr.childNode.parent = expr;
				return expr;
			}
			else if (samTok.peekAtKind() == TokenType.OPERATOR){
				char c = samTok.getOp(); // eat the (
				if (c != '('){
					throw new ParseException("Invalid experession", 0);
				}
				TokenType tt = samTok.peekAtKind(); // peek one more time to check for more parens or unary op
				if (tt == TokenType.OPERATOR){
					if (samTok.test('!') || samTok.test('~')){ // unary op
						UnopNode unop = new UnopNode();
						unop.op = samTok.getOp();
						unop.child = getExpr(samTok);
						unop.child.parent = unop;
						expr.childNode = unop;
						unop.parent = expr;
						samTok.getOp(); //eat )
						return expr;
					}
					else {
						ExprNode child = getExpr(samTok);
						expr.childNode = child;
						expr.childNode.parent = expr;
						c = samTok.getOp(); // check next token
						if (c == ')'){
							return expr;
						}
						else if (c == '?'){ // ternary choice op
							TeropNode terop = new TeropNode();
							terop.id = samTok.lineNo();
							expr.childNode = terop;
							terop.parent = expr;
							terop.condition = child;
							terop.t = getExpr(samTok);
							terop.t.parent = terop;
							c = samTok.getOp(); // eat :
							terop.f = getExpr(samTok);
							terop.f.parent = terop;
							c = samTok.getOp(); // eat )
							return expr;
						}
						else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '|'
						|| c == '&' || c == '=' || c == '<' || c == '>'){ // binary op
							NodeBinop binop = new NodeBinop();
							expr.childNode = binop;
							binop.parent = expr;
							binop.op = c;
							binop.left = child;
							binop.left.parent = binop;
							binop.right = getExpr(samTok);
							binop.right.parent = binop;
							c = samTok.getOp(); // eat )
							return expr;
						}
						else{
							throw new ParseException("Invalid expression", samTok.lineNo());
						}
					}
				}
				else{
					ExprNode child = getExpr(samTok);
					expr.childNode = child;
					expr.childNode.parent = expr;
					c = samTok.getOp(); // check next token
					if (c == ')'){
						return expr;
					}
					else if (c == '?'){ // ternary choice op
						TeropNode terop = new TeropNode();
						terop.id = samTok.lineNo();
						expr.childNode = terop;
						terop.parent = expr;
						terop.condition = child;
						terop.t = getExpr(samTok);
						terop.t.parent = terop;
						c = samTok.getOp(); // eat :
						terop.f = getExpr(samTok);
						terop.f.parent = terop;
						c = samTok.getOp(); // eat )
						return expr;
					}
					else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '|'
							|| c == '&' || c == '=' || c == '<' || c == '>'){ // binary op
						NodeBinop binop = new NodeBinop();
						expr.childNode = binop;
						binop.parent = expr;
						binop.op = c;
						binop.left = child;
						binop.left.parent = binop;
						binop.right = getExpr(samTok);
						binop.right.parent = binop;
						c = samTok.getOp(); // eat )
						return expr;
					}
					else{
						throw new ParseException("Invalid expression", samTok.lineNo());
					}
				}
			}
			else {
				throw new ParseException("Invalid expression", samTok.lineNo());
			}
		}
		catch (ParseException e){
			throw new ParseException("Invalid expression", 0);
		}
	}

	static MethodNode getMethod(String name, SamTokenizer samTok) throws ParseException{
		char c = samTok.getOp(); // eat (
		ArrayList<NodeATS> actuals = new ArrayList<NodeATS>();
		MethodNode method = new MethodNode(null, null);
		while (!samTok.test(')')){
			if (samTok.test(',')){
				samTok.skipToken();
			}
			NodeATS actual = getExpr(samTok);
			actual.parent = method;
			actuals.add(actual);
		}
		method.name = name;
		method.actuals = actuals;


		c = samTok.getOp(); // eat )
		return method;
	}

	static String getType(SamTokenizer samTok){
		try{
			String type = samTok.getWord();
			if (!type.equals("int") && !type.equals("String") && !type.equals("bool")){
				throw new ParseException("unrecognized data type for variable", samTok.lineNo());
			}
			else{
				return type;
			}
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}

	static LiteralNode getLiteral(SamTokenizer samTok){
		try{
			if (samTok.peekAtKind() == TokenType.STRING){
				return new LiteralNode(getString(samTok));
			}
			else if (samTok.peekAtKind() == TokenType.WORD){
				return new LiteralNode(samTok.getWord());
			}
			else{
				throw new ParseException("Invalid literal", samTok.lineNo());
			}
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return null;
		}
	}

	static VarNode getVar(SamTokenizer samTok){
		try{
			String identifier = getIdentifier(samTok);
			return new VarNode(identifier);
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return null;
		}
	}

	static NumNode getNum(SamTokenizer samTok){
		try{
			int num = samTok.getInt();
			String str = String.valueOf(num);
			Pattern p = Pattern.compile("([0-9])+");
			if (!p.matcher(str).matches()) {
				throw new ParseException("Invalid String", samTok.lineNo());
			}
			else{
				return new NumNode(num);
			}
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return null;
		}
	}

	static String getString(SamTokenizer samTok){
		try{
			String str = samTok.getString();
			Pattern p = Pattern.compile("^\\p{ASCII}*$");
			if (!p.matcher(str).matches()) {
				throw new ParseException("Invalid String", samTok.lineNo());
			}
			else{
				return str;
			}
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}

	static String getIdentifier(SamTokenizer samTok){
		try{
			String str = samTok.getWord();
			if (str.equals("return")){
				return null;
			}
			else{
				return str;
			}
		}
		catch (Exception e){
			System.out.println("Fatal error: could not compile program");
			return "STOP\n";
		}
	}

	static String processTree(ProgramNode prgNode){
		String pgm = "";
		pgm += "ADDSP " + variable_map.size() + "\n";
		pgm += "JUMP main\n";
		for (int i = 0 ; i < prgNode.children.size() ; i++){
			NodeMethodDecl method = prgNode.children.get(i);
			pgm += processMethodDecl(method) + "\n";
		}
		pgm += "end:\n";
		pgm += "ADDSP -" + variable_map.size() + "\n";
		pgm += "PUSHABS 0\n";
		pgm += "STOP";
		return pgm;
	}

	static String processMethodDecl(NodeMethodDecl methodDecl){
		String result = "";
		result += methodDecl.name + ":\n";
		result += processBody(methodDecl.body);

		return result;
	}

	static String processBody(BodyNode bodyNode){
		String result = "";
		result += processBlock(bodyNode.child);

		return result;
	}

	static String processBlock(BlockNode blockNode){
		String result = "";
		for(int i = 0; i < blockNode.stmts.size() ; i++){
			result += processStmt(blockNode.stmts.get(i));
			//System.out.print(result);
		}

		return result;
	}

	static String processStmt(NodeATS nodeATS){
		try{
			String result = "";
			if (nodeATS.typeOfNode.equals("Stmt")){
				StmtNode stmt = (StmtNode) nodeATS;
				if (stmt.value == null){
					result += processExpr(stmt.expr);
					result += setVar(stmt.var);
				}
				else if (stmt.value.equals("return")){
					String method_name = getMethodName(stmt);
					if (!method_name.equals("main")){
						result += processExpr(stmt.expr);
						result += "SWAP\nJUMPIND\n";
					}
					else{
						result += processExpr(stmt.expr);
						result += "STOREABS 0\n";
						result += "JUMP end\n";
					}
				}
				else if (stmt.value.equals("break")){
					BlockNode block = (BlockNode)stmt.parent;
					result += "JUMP endloop" + getParentLoop(block) + "\n";
				}
				else{
					throw new ParseException("Unknown statement type", -1);
				}
			}
			else if (nodeATS.typeOfNode.equals("Loop")){
				NodeLoopStmt loop = (NodeLoopStmt) nodeATS;
				result += processLoop(loop);
			}
			else if (nodeATS.typeOfNode.equals("Conditional")){
				NodeConditionalStmt cond = (NodeConditionalStmt) nodeATS;
				result += processCond(cond);
			}
			else{
				throw new ParseException("Uknown statement type", -1);
			}
			return result;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	static String processCond(NodeConditionalStmt stmt){
		String result = "";
		result += processExpr(stmt.condition) + "PUSHIMM 1\nSUB\n";
		result += "JUMPC else" + stmt.id + "\n";
		result += "if" + stmt.id + ":\n";
		result += processBlock(stmt.if_block);
		result += "JUMP ifend" + stmt.id + "\n";
		result += "else" + stmt.id + ":\n";
		result += processBlock(stmt.else_block);
		result += "ifend" + stmt.id + ":\n";

		return result;
	}

	static String processLoop(NodeLoopStmt loop){
		String result = "";
		result += "loop" + loop.id + ":\n";
		result += processExpr(loop.condition) + "PUSHIMM 1\nSUB\n";
		result += "JUMPC endloop" + loop.id + "\n";
		result += processBlock(loop.block);
		result += "JUMP loop" + loop.id + "\n";
		result += "endloop" + loop.id + ":\n";

		return result;
	}

	static String processExpr(NodeATS node){
		if (node.typeOfNode == "Expr"){
			ExprNode expr = (ExprNode) node;
			return processExpr(expr.childNode);
		}
		else if (node.typeOfNode == "Unop"){
			UnopNode unop = (UnopNode) node;
			return processUnop(unop);
		}
		else if (node.typeOfNode == "Binop"){
			NodeBinop binop = (NodeBinop) node;
			return processBinop(binop);
		}
		else if (node.typeOfNode == "Var"){
			VarNode var = (VarNode) node;
			String scope = getMethodName(node);
			var.scope = scope;
			return readVar(var);
		}
		else if (node.typeOfNode == "VarDecl"){
			VarDeclNode var = (VarDeclNode) node;
			return readVar(var);
		}
		else if (node.typeOfNode == "Num"){
			NumNode num = (NumNode) node;
			return processNum(num);
		}
		else if (node.typeOfNode == "Bool"){
			BoolNode bool = (BoolNode) node;
			return processBool(bool);
		}
		else if (node.typeOfNode == "Method"){
			MethodNode method = (MethodNode) node;
			return processMethod(method);
		}
		else if (node.typeOfNode == "Terop"){
			TeropNode terop = (TeropNode) node;
			return processTerop(terop);
		}
		else{
			LiteralNode lit = (LiteralNode) node;
			return processLiteral(lit);
		}
	}
	static String processMethod(MethodNode method){
		String result = "";
		for (int i = 0 ; i < method.actuals.size(); i++){
			result += processExpr(method.actuals.get(i));
			String identifier = method_map.get(method.name).formals.get(i).identity;
			VarNode var = new VarNode(identifier);
			var.scope = method.name;
			result += setVar(var);
		}
		result += "JSR " + method.name + "\n";

		return result;
	}
	static String processUnop(UnopNode unopNode){
		try{
			String result = "";
			NodeATS n = stripExpr(unopNode.child);
			if (n.typeOfNode.equals("Num")){
				NodeATS op = getOperand(n);
				if (unopNode.op == '~'){
					result += processExpr(op);
					result += "PUSHIMM -1\nTIMES\n";
				}
				else{
					throw new Exception("unhandle unop");
				}
			}
			else if (n.typeOfNode.equals("Bool")){
				NodeATS op = getOperand(n);
				if (unopNode.op == '!'){
					result += processExpr(op);
					result += "NOT\n";
				}
			}
			else if (n.typeOfNode.equals("Literal")){
				LiteralNode lit = (LiteralNode) n;
				if (unopNode.op == '~'){
					result += processExpr(lit);
					result += str_rev(lit.id);
				}
			}
			else if (n.typeOfNode.equals("Binop")){
				NodeBinop b = (NodeBinop) n;
				result += processBinop(b);
				if (unopNode.op == '!'){
					result += "NOT\n";
				}
			}
			else if (n.typeOfNode.equals("Var")){
				VarNode v = (VarNode)n;
				String method = getMethodName(n);
				VarDeclNode var = variable_map.get(method+"."+v.identifier);
				if (var.type.equals("int")){
					if (unopNode.op == '!'){
						result += processExpr(var);
						result += "NOT\n";
					}
					else{
						throw new Exception("unhandle unop");
					}
				}
				else if(var.type.equalsIgnoreCase("String")){
					if (unopNode.op == '~'){
						result += processExpr(var);
						result += str_rev(var.id);
					}
				}

			}
			else{
				throw new Exception("unhandle unop");
			}
			return result;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	static String processTerop(TeropNode teropNode){
		try{
			String result = "";
			ExprNode cond = (ExprNode)teropNode.condition;
			while (true){
				if (cond.childNode.typeOfNode.equals("Expr")){
					cond.childNode.parent = cond.parent;
					cond = (ExprNode)cond.childNode;
				}
				else{
					break;
				}
			}
			result += processExpr(cond);
			result += "PUSHIMM 1\nSUB\nJUMPC f" + teropNode.id + "\n";
			result += "t" + teropNode.id + ":\n" + processExpr(teropNode.t);
			result += "JUMP teropend" + teropNode.id + "\n";
			result += "f" + teropNode.id + ":\n" + processExpr(teropNode.f);
			result += "teropend" + teropNode.id + ":\n";

			return result;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}

	}

	static String processBinop(NodeBinop nodeBinop){
		try{
			String result = "";
			NodeATS left = stripExpr(nodeBinop.left);
			NodeATS right = stripExpr(nodeBinop.right);
			if (left.typeOfNode.equals("Binop") && right.typeOfNode.equals("Binop")){
				result += processBinop((NodeBinop)left);
				result += processBinop((NodeBinop)right);
			}
			else if (left.typeOfNode.equals("Binop")){
				if (right.typeOfNode.equals("Unop")){
					result += processBinop((NodeBinop)left);
					result += processUnop((UnopNode)right);
				}
				else{
					result += processBinop((NodeBinop)left);
					result += processExpr(right);
				}
			}
			else if (right.typeOfNode.equals("Binop")){
				result += processExpr(left);
				result += processBinop((NodeBinop)right);
			}
			else if (left.typeOfNode.equals("Method") && right.typeOfNode.equals("Method")){
				result += processMethod((MethodNode)left);
				result += processMethod((MethodNode)right);
			}
			else if (left.typeOfNode.equals("Method")){
				result += processMethod((MethodNode)left);
				result += processExpr(right);
			}
			else if (right.typeOfNode.equals("Method")){
				result += processExpr(left);
				result += processMethod((MethodNode)right);
			}
			else if (left.typeOfNode.equals("Unop") && right.typeOfNode.equals("Unop")){
				result += processUnop((UnopNode) left);
				result += processUnop((UnopNode) right);
			}
			else if (left.typeOfNode.equals("Unop")){
				result += processUnop((UnopNode) left);
				result += processExpr(right);
			}
			else if (right.typeOfNode.equals("Unop")){
				result += processExpr(left);
				result += processUnop((UnopNode) right);
			}
			else{ // assumming we're just dealing with nums / literals
				result += processExpr(left) + processExpr(right);
			}
			NodeATS op1 = getOperand(left);
			NodeATS op2 = getOperand(right);
			result += processOp(nodeBinop.op, op1, op2);
			return result;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}

	}

	static String getType(NodeATS nodeATS){
		try{
			if (nodeATS.typeOfNode.equals("Num")){
				return "int";
			}
			else if (nodeATS.typeOfNode.equals("Literal")){
				return "String";
			}
			else if (nodeATS.typeOfNode.equals("Bool")){
				return "bool";
			}
			else if (nodeATS.typeOfNode.equals("Var")){
				VarNode var = (VarNode) nodeATS;
				if (var.scope == null){
					var.scope = getMethodName(var);
				}
				return variable_map.get(var.scope + "." + var.identifier).type;
			}
			else if (nodeATS.typeOfNode.equals("Method")){
				MethodNode method = (MethodNode) nodeATS;
				return method_map.get(method.name).type;
			}
			else if (nodeATS.typeOfNode.equals("Binop")){
				NodeBinop binop = (NodeBinop) nodeATS;
				return getBinopType(binop);
			}
			else if (nodeATS.typeOfNode.equals("Unop")){
				UnopNode unop = (UnopNode) nodeATS;
				return checkOp(unop.child);
			}
			else{
				throw new ParseException("Unknown operand type", -1);
			}
		}
		catch (Exception e){
			System.out.println(e.getMessage());
			return null;
		}
	}

	static void updateVar(VarNode varNode, ExprNode exprNode){
		NodeATS val = getOperand(exprNode);
		if (val.typeOfNode.equals("Literal")){
			LiteralNode lit = (LiteralNode) val;
			varNode.value = lit.literal;
		}
		else if (val.typeOfNode.equals("Num")){
			NumNode num = (NumNode) val;
			varNode.value = num.num;
		}
	}

	static NodeATS getOperand(NodeATS nodeATS){
		if (nodeATS.typeOfNode == "Expr"){
			ExprNode expr = (ExprNode) nodeATS;
			return getOperand(expr.childNode);
		}
		else if (nodeATS.typeOfNode == "Var"){
			VarNode var = (VarNode) nodeATS;
			return var;
		}
		else if (nodeATS.typeOfNode == "Num"){
			NumNode num = (NumNode) nodeATS;
			return num;
		}
		else if (nodeATS.typeOfNode == "Bool"){
			BoolNode bool = (BoolNode) nodeATS;
			return bool;
		}
		else if (nodeATS.typeOfNode == "Method"){
			MethodNode method = (MethodNode) nodeATS;
			return method;
		}
		else if (nodeATS.typeOfNode == "Binop"){
			NodeBinop binop = (NodeBinop) nodeATS;
			return binop;
		}
		else if (nodeATS.typeOfNode == "Unop"){
			UnopNode unop = (UnopNode) nodeATS;
			return unop;
		}
		else{
			LiteralNode lit = (LiteralNode) nodeATS;
			return lit;
		}
	}

	static String readVar(NodeATS nodeATS){
		if(nodeATS.typeOfNode.equals("Var")){
			VarNode v = (VarNode) nodeATS;
			if (v.scope == null){
				v.scope = getMethodName(v);
			}
			return "PUSHOFF " + (variable_map.get(v.scope + "." + v.identifier).location-1) + "\n";
		}
		else{
			VarDeclNode v = (VarDeclNode) nodeATS;
			return "PUSHOFF " + (variable_map.get(v.scope + "." + v.identifier).location-1) + "\n";
		}
	}

	static String setVar(VarNode varNode){
		if (varNode.scope == null){
			varNode.scope = getMethodName(varNode);
		}
		return "STOREOFF " + (variable_map.get(varNode.scope + "." + varNode.identifier).location-1) + "\n";
	}

	static String processNum(NumNode numNode){
		return "PUSHIMM " + numNode.num + "\n";
	}

	static String processLiteral(LiteralNode literalNode){
		return "PUSHIMMSTR \"" + literalNode.literal + "\"\n";
	}

	static String processBool(BoolNode bool){
		return "PUSHIMM " + bool.value + "\n";
	}

	static String processOp(char op, NodeATS nodeATS1, NodeATS nodeATS2){
		try{
			String result = "";
			String op1_type = checkOp(stripExpr(nodeATS1));
			String op2_type = checkOp(stripExpr(nodeATS2));
			if (op1_type.equals("int") && op2_type.equals("int")){
				if (op == '\0'){ // null op
					return "";
				}
				else if (op == '+'){
					return "ADD\n";
				}
				else if (op == '-'){
					return "SUB\n";
				}
				else if (op == '*'){
					return "TIMES\n";
				}
				else if (op == '/'){
					return "DIV\n";
				}
				else if (op == '%'){
					return "MOD\n";
				}
				else if (op == '<'){
					return "LESS\n";
				}
				else if (op == '>'){
					return "GREATER\n";
				}
				else if (op == '%'){
					return "MOD\n";
				}
				else if (op == '='){
					return "EQUAL\n";
				}
				else if (op == '|'){
					return "OR\n";
				}
				else if (op == '&'){
					return "AND\n";
				}
				else{
					throw new ParseException("Unrecognized op " + op, -1);
				}
			}
			else if (op1_type.equals("bool") && op2_type.equals("bool")){
				if (op == '='){
					return "EQUAL\n";
				}
				else if (op == '|'){
					return "OR\n";
				}
				else if (op == '&'){
					return "AND\n";
				}
				else{
					throw new ParseException("Unrecognized op " + op, -1);
				}
			}
			else if (op1_type.equals("bool") && op2_type.equals("String")){
				throw new Error("Failed to compile");
			}
			else{
				if (op == '*'){
					result += str_repeat(nodeATS1.id);
					return result;
				}
				else if (op == '+'){
					result += str_concat(nodeATS1.id);
					return result;
				}
				else if (op == '='){
					result += str_cmp(nodeATS1.id);
					result += "PUSHIMM 0\nEQUAL\n";
					return result;
				}
				else if (op == '>'){
					result += str_cmp(nodeATS1.id);
					result += "PUSHIMM -1\nEQUAL\n";
					return result;
				}
				else if (op == '<'){
					result += str_cmp(nodeATS1.id);
					result += "PUSHIMM 1\nEQUAL\n";
					return result;
				}
				else{
					throw new ParseException("unknown string operand", 0);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	static void writeSamfile(String program, String f){
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(program);
			bw.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	static boolean validateTree(ProgramNode programNode){
		ArrayList<NodeMethodDecl> methods = programNode.children;
		for (int i = 0 ; i < methods.size(); i++){
			if (!hasReturn(methods.get(i))){
				return false;
			}
			if (!validReturn(methods.get(i))){
				return false;
			}
			if (!validMethodCalls(methods.get(i))){
				return false;
			}
		}
		return true;
	}

	static boolean validMethodCalls(NodeMethodDecl nodeMethodDecl){
		BodyNode body = nodeMethodDecl.body;
		BlockNode block = body.child;
		for (int i = 0; i < block.stmts.size() ; i++){
			NodeATS node = block.stmts.get(i);
			MethodNode m = findMethodCall(node);
			if (m == null){
				continue;
			}
			if (m.actuals.size() != method_map.get(m.name).formals.size()){
				return false;
			}
			for (int j = 0 ; j < m.actuals.size() ; j++){
				String t1 = getType(stripExpr(m.actuals.get(j)));
				String t2 = method_map.get(m.name).formals.get(j).type;
				if (!t1.equalsIgnoreCase(t2)){
					return false;
				}
			}
		}
		return true;
	}

	static MethodNode findMethodCall(NodeATS nodeATS){
		if (nodeATS.typeOfNode.equals("Method")){
			MethodNode method = (MethodNode) nodeATS;
			return method;
		}
		else if(nodeATS.typeOfNode.equals("Stmt")){
			StmtNode stmt = (StmtNode) nodeATS;
			return findMethodCall(stmt.expr);
		}
		else if (nodeATS.typeOfNode.equals("Expr")){
			ExprNode expr = (ExprNode) nodeATS;
			return findMethodCall(expr.childNode);
		}
		else{
			return null;
		}
	}

	static boolean hasReturn(NodeMethodDecl nodeMethodDecl){
		BodyNode body = nodeMethodDecl.body;
		BlockNode block = body.child;
		for (int i = 0; i < block.stmts.size() ; i++){
			NodeATS node = block.stmts.get(i);
			if (node.typeOfNode.equals("Stmt")){
				StmtNode stmt = (StmtNode) node;
				if (stmt.value != null){
					if (stmt.value.equals("return")){
						return true;
					}
				}
			}
		}
		return false;
	}

	static boolean validReturn(NodeMethodDecl nodeMethodDecl){
		BodyNode body = nodeMethodDecl.body;
		BlockNode block = body.child;
		for (int i = 0; i < block.stmts.size() ; i++){
			NodeATS node = block.stmts.get(i);
			if (node.typeOfNode.equals("Stmt")){
				StmtNode stmt = (StmtNode) node;
				if (stmt.value != null){
					if (stmt.value.equals("return")){
						NodeATS op = stmt.expr.childNode;
						op = stripExpr(op);
						String type = "";
						if (op.typeOfNode.equals("Bool")){
							type = "bool";
						}
						else if (op.typeOfNode.equals("Num")){
							type = "int";
						}
						else if (op.typeOfNode.equals("Literal")){
							type = "String";
						}
						else if (op.typeOfNode.equals("Method")){
							MethodNode m = (MethodNode) op;
							type = method_map.get(m.name).type;
						}
						else if (op.typeOfNode.equals("Var")){
							VarNode var = (VarNode) op;
							String scope = getMethodName(var);
							type = variable_map.get(scope + "." + var.identifier).type;
						}
						else if (op.typeOfNode.equals("Binop")){
							NodeBinop binop = (NodeBinop) op;
							if (binop.op == '|' || binop.op == '&' || binop.op == '=' || binop.op == '>' || binop.op == '<'){
								type = "bool";
							}
							else if (numOp(binop.op)){
								type = getBinopType(binop);
							}
							else{
								type = "";
							}
						}
						else if (op.typeOfNode.equals("Terop")){
							TeropNode terop = (TeropNode) op;
							String op1 = checkOp(stripExpr(terop.t));
							String op2 = checkOp(stripExpr(terop.f));
							if (nodeMethodDecl.type.equals(op1) && nodeMethodDecl.type.equals(op2)){
								return true;
							}
						}
						else if (op.typeOfNode.equals("Unop")){
							UnopNode unop = (UnopNode) op;
							String op1 = checkOp(unop.child);
							if (nodeMethodDecl.type.equals(op1)){
								return true;
							}
						}
						if (nodeMethodDecl.type.equals(type)){
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	static String checkOp(NodeATS nodeATS){
		while(nodeATS.typeOfNode.equals("Expr")){
			ExprNode e = (ExprNode) nodeATS;
			nodeATS = e.childNode;
		}
		if (nodeATS.typeOfNode.equals("Bool")){
			return "bool";
		}
		else if (nodeATS.typeOfNode.equals("Num")){
			return "int";
		}
		else if (nodeATS.typeOfNode.equals("Literal")){
			return "String";
		}
		else if (nodeATS.typeOfNode.equals("Method")){
			MethodNode m = (MethodNode) nodeATS;
			return method_map.get(m.name).type;
		}
		else if (nodeATS.typeOfNode.equals("Unop")){
			UnopNode unop = (UnopNode) nodeATS;
			char op = unop.op;
			return checkOp(stripExpr(unop.child));
		}
		else if (nodeATS.typeOfNode.equals("Binop")){
			NodeBinop binop = (NodeBinop) nodeATS;
			return getBinopType(binop);
		}
		else if (nodeATS.typeOfNode.equals("Var")){
			VarNode var = (VarNode) nodeATS;
			String scope = getMethodName(var);
			return variable_map.get(scope + "." + var.identifier).type;
		}
		return "";
	}

	static boolean validName(String name){
		Pattern p = Pattern.compile("[a-zA-Z]([a-zA-Z0-9’_’])*");
		if (!p.matcher(name).matches()) {
			return false;
		}
		else{
			return true;
		}
	}

	static void setParents(BodyNode bodyNode, NodeATS nodeATS){
		if (nodeATS.typeOfNode.equals("Block")){
			BlockNode block = (BlockNode) nodeATS;
			block.parent = bodyNode;
			for (int i = 0; i < block.stmts.size() ; i++){
				NodeATS stmt = block.stmts.get(i);
				setParents(bodyNode, stmt);
			}
		}
		else if (nodeATS.typeOfNode.equals("Conditional")){
			NodeConditionalStmt cond = (NodeConditionalStmt) nodeATS;
			setParents(bodyNode, cond.if_block);
			setParents(bodyNode, cond.else_block);
		}
		else {
			return;
		}
	}

	static boolean numOp(char op){
		if (op == '+' || op == '-' || op == '*' || op == '/' || op == '%'){
			return true;
		}
		else{
			return false;
		}
	}

	static String getMethodName(NodeATS nodeATS){
		if (nodeATS.typeOfNode.equals("MethodDecl")){
			NodeMethodDecl method = (NodeMethodDecl) nodeATS;
			return method.name;
		}
		else{
			return getMethodName(nodeATS.parent);
		}
	}

	static NodeATS stripExpr(NodeATS nodeATS){
		if (!nodeATS.typeOfNode.equals("Expr")){
			return nodeATS;
		}
		else{
			ExprNode expr = (ExprNode)nodeATS;
			expr.childNode.parent = nodeATS.parent;
			return stripExpr(expr.childNode);
		}
	}

	static int getParentLoop(NodeATS nodeATS){
		if (nodeATS.typeOfNode.equals("Loop")){
			NodeLoopStmt loop = (NodeLoopStmt) nodeATS;
			return loop.id;
		}
		else{
			return getParentLoop(nodeATS.parent);
		}
	}

	static String getBinopType(NodeBinop nodeBinop){
		String left_type, right_type;
		NodeATS left = stripExpr(nodeBinop.left);
		NodeATS right = stripExpr(nodeBinop.right);
		//TODO I made this change since last test build
		if (nodeBinop.op == '|' || nodeBinop.op == '&' || nodeBinop.op == '=' || nodeBinop.op == '<' || nodeBinop.op == '>'){
			nodeBinop.type = "bool";
			return "bool";
		}
		else if (left.typeOfNode.equals("Binop") && right.typeOfNode.equals("Binop")){
			left_type = getBinopType((NodeBinop)left);
			right_type = getBinopType((NodeBinop)right);
		}
		else if (left.typeOfNode.equals("Binop")){
			left_type = getBinopType((NodeBinop)left);
			right_type = checkOp(right);
		}
		else if (right.typeOfNode.equals("Binop")){
			left_type = checkOp(left);
			right_type = getBinopType((NodeBinop)right);
		}
		else{
			left_type = checkOp(left);
			right_type = checkOp(right);
		}

		if (left_type.equals("String") || right_type.equals("String")){
			nodeBinop.type = "String";
			return "String";
		}
		else if (left_type.equals("bool") || right_type.equals("bool")){
			nodeBinop.type = "bool";
			return "bool";
		}
		else{
			nodeBinop.type = "int";
			return "int";
		}
	}




	// string operations

	static String str_cmp(int i){
		String result =  "strcmp"+ i + ":\n";

		result+="DUP\n";
		result+="STOREABS 3002\n"; // string 2 address
		result+="SWAP\n";
		result+="DUP\n";
		result+="STOREABS 3001\n"; // string 1 address
		result+="strcmplength1" + i + ":\n";
		result+="DUP\n";
		result+="PUSHIMM 1\n";
		result+="SUB\n";
		result+="strcmpiterate1" + i + ":\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="DUP\n";
        result+="PUSHIND\n";
        result+="PUSHIMMCH '\0'\n";
        result+="EQUAL\n";
        result+="PUSHIMM 1\n";
        result+="SUB\n";
        result+="JUMPC strcmpiterate1"+i+"\n";
		result+="SWAP\n";
		result+="SUB\n";
		result+="STOREABS 3003\n"; // length of string 2
		result+="strcmplength2" + i + ":\n";
		result+="DUP\n";
		result+="PUSHIMM 1\n";
		result+="SUB\n";
    	result+="strcmpiterate2" + i + ":\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="DUP\n";
        result+="PUSHIND\n";
        result+="PUSHIMMCH '\0'\n";
        result+="EQUAL\n";
        result+="PUSHIMM 1\n";
        result+="SUB\n";
        result+="JUMPC strcmpiterate2"+i+"\n";
		result+="SWAP\n";
		result+="SUB\n";
		result+="STOREABS 3004\n"; // length of string 1
		result+="PUSHABS 3004\n";
		result+="PUSHABS 3003\n";
		result+="EQUAL \n";
		result+="JUMPC strcmpiterate3"+i+"\n";

		result+="PUSHABS 3004\n";
		result+="PUSHABS 3003\n";
		result+="CMP\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="JUMPC first_length"+i+"\n";
		result+="PUSHABS 3004\n";
		result+="STOREABS 3006\n";
		result+="PUSHABS 3003\n";
		result+="STOREABS 3008\n"; // smaller string length
		result+="PUSHABS 3001\n";
		result+="STOREABS 3009\n"; // smaller string address

		result+="JUMP fill"+i+"\n";

		result+="first_length" + i + ":\n";
		result+="PUSHABS 3003\n";
		result+="STOREABS 3006\n";
		result+="PUSHABS 3004\n";
		result+="STOREABS 3008\n";// smaller string length
		result+="PUSHABS 3002\n";
		result+="STOREABS 3009\n"; // smaller string address

		result+="fill" + i + ":\n";
		result+="PUSHABS 3006\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="MALLOC\n";
		result+="STOREABS 3007\n"; // new string address
		result+="PUSHIMM -1\n";
		result+="STOREABS 3005\n"; // counter
		result+="strcmpiterate4" + i + ":\n";
        result+="PUSHABS 3005\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="STOREABS 3005\n";
        result+="PUSHABS 3005\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="PUSHABS 3008\n";
        result+="GREATER\n";
        result+="JUMPC pad"+i+"\n";
        result+="PUSHABS 3005\n";
        result+="PUSHABS 3009\n";
        result+="ADD\n";
        result+="PUSHIND\n";
        result+="PUSHABS 3007\n";
        result+="PUSHABS 3005\n";
        result+="ADD\n";
        result+="SWAP\n";
        result+="STOREIND\n";
        result+="JUMP strcmpiterate4"+i+"\n";
        result+="pad" + i + ":\n";
		result+="PUSHABS 3005\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="PUSHABS 3006\n";
		result+="GREATER\n";
		result+="JUMPC prep"+i+"\n";
		result+="PUSHIMMCH ' '\n";
		result+="PUSHABS 3007\n";
		result+="PUSHABS 3005\n";
		result+="ADD\n";
		result+="SWAP\n";
		result+="STOREIND\n";
		result+="PUSHABS 3005\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="STOREABS 3005\n";
		result+="JUMP pad"+i+"\n";

		result+="prep" + i + ":\n";
		result+="PUSHABS 3005\n";
		result+="PUSHABS 3007\n";
		result+="ADD\n";
		result+="PUSHIMMCH '\0'\n";
		result+="STOREIND\n";
		result+="PUSHABS 3008\n";
		result+="PUSHABS 3003\n";
		result+="EQUAL\n";
		result+="JUMPC set1"+i+"\n";
		result+="PUSHABS 3007\n";
		result+="STOREABS 3002\n";
		result+="JUMP start"+i+"\n";

		result+="set1" + i + ":\n";
		result+="PUSHABS 3007\n";
		result+="STOREABS 3001\n";
		result+="JUMP start"+i+"\n";

		result+="strcmpiterate3" + i + ":\n";
		result+="PUSHABS 3003\n";
		result+="STOREABS 3006\n";

		result+="start" + i + ":\n";
		result+="PUSHIMM -1\n";
		result+="STOREABS 3005\n"; // counter
		result+="PUSHIMM 1\n";
		result+="loop" + i + ":\n";
        result+="ADDSP -1\n";
        result+="PUSHABS 3005\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="STOREABS 3005\n";
        result+="PUSHABS 3005\n";
        result+="PUSHABS 3006\n";
        result+="EQUAL\n";
        result+="JUMPC finalpre"+i+"\n";
        result+="PUSHABS 3005\n";
        result+="PUSHABS 3001\n";
        result+="ADD\n";
        result+="PUSHIND\n";
        result+="PUSHABS 3005\n";
        result+="PUSHABS 3002\n";
        result+="ADD\n";
        result+="PUSHIND\n";
        result+="CMP\n";
        result+="DUP\n";
        result+="PUSHIMM 0\n";
        result+="EQUAL\n";
        result+="JUMPC loop"+i+"\n";
		result+="finalpre" + i + ":\n";
		result+="PUSHSP\n";
		result+="PUSHIMM 0\n";
		result+="GREATER\n";
		result+="JUMPC finalstrcmp"+i+"\n";
		result+="PUSHIMM 0\n";
		result+="finalstrcmp" + i + ":\n";

		return result;
	}

	static String str_concat(int i){
		String result = "strconcat"+ i + ":\n";
		result+="DUP\n";
		result+="STOREABS 3000\n"; // string 2 address
		result+="SWAP\n";
		result+="DUP\n";
		result+="STOREABS 3001\n"; // string 1 address
		result+="length1" + i + ":\n";
		result+="DUP\n";
		result+="PUSHIMM 1\n";
		result+="SUB\n";
		result+="strconcatiterate1" + i + ":\n";
        result+="PUSHIMM 1\n";
        result+="ADD \n";
        result+="DUP\n";
        result+="PUSHIND\n";
        result+="PUSHIMMCH '\0'\n";
		result+="EQUAL\n";
        result+="PUSHIMM 1\n";
        result+="SUB\n";
        result+="JUMPC strconcatiterate1"+i+"\n";
		result+="SWAP\n";
		result+="SUB\n";
		result+="SWAP\n";
		result+="length2" + i + ":\n";
		result+="DUP\n";
		result+="PUSHIMM 1\n";
		result+="SUB\n";
		result+="strconcatiterate2" + i + ":\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="DUP\n";
        result+="PUSHIND\n";
        result+="PUSHIMMCH '\0'\n";
        result+="EQUAL\n";
        result+="PUSHIMM 1\n";
        result+="SUB\n";
        result+="JUMPC strconcatiterate2"+i+"\n";
		result+="SWAP\n";
		result+="SUB\n";
		// allocate memory for both strings
		result+="ADD\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="MALLOC\n";
		result+="DUP\n";
		result+="STOREABS 3002\n"; // concat string address
		result+="DUP\n";
		result+="STOREABS 3003\n"; // current pointer
		result+="PUSHABS 3001\n";
		result+="strconcatiterate3" + i + ":\n";
		result+="PUSHIND\n";
		result+="DUP\n";
		result+="PUSHIMMCH '\0'\n";
		result+="EQUAL\n";
		result+="JUMPC end1"+i+"\n";
		result+="STOREIND\n";
		result+="PUSHABS 3003\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="DUP\n";
		result+="STOREABS 3003\n";
		result+="PUSHABS 3001\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="DUP\n";
		result+="STOREABS 3001\n";
		result+="JUMP strconcatiterate3"+i+"\n";
		result+="end1" + i + ":\n";
		result+="ADDSP -1\n";
		result+="PUSHABS 3000\n";
		result+="strconcatiterate4" + i + ":\n";
		result+="PUSHIND\n";
		result+="DUP\n";
		result+="PUSHIMMCH '\0'\n";
		result+="EQUAL\n";
		result+="JUMPC finalstrconcat"+i+"\n";
		result+="STOREIND\n";
		result+="PUSHABS 3003\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="DUP\n";
		result+="STOREABS 3003\n";
		result+="PUSHABS 3000\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="DUP\n";
		result+="STOREABS 3000\n";
		result+="JUMP strconcatiterate4"+i+"\n";
		result+="finalstrconcat" + i + ":\n";;
		result+="ADDSP -1\n";
		result+="PUSHIMMCH '\0'\n";
		result+="STOREIND\n";
		result+="PUSHABS 3002\n";

		return result;
	}

	static String str_repeat(int i){
		String result = "strrepeat"+ i + ":\n";
		result+="SWAP\n";
		result+="DUP\n";
		result+="STOREABS 3000\n"; // string address
		result+="PUSHIMM 0\n";
		result+="STOREABS 3004\n"; // counter for new string
		result+="PUSHIMM 1\n";
		result+="MALLOC\n";
		result+="STOREABS 3002\n"; // new string address default of 1 spot
		result+="length" + i + ":\n";
		result+="DUP\n";
		result+="PUSHIMM 1\n";
		result+="SUB\n";
		result+="strrepeatiterate" + i + ":\n";
        result+="PUSHIMM 1\n";
        result+="ADD\n";
        result+="DUP\n";
        result+="PUSHIND\n";
        result+="PUSHIMMCH '\0'\n";
        result+="EQUAL\n";
        result+="PUSHIMM 1\n";
        result+="SUB\n";
        result+="JUMPC strrepeatiterate"+i+"\n";
		result+="SWAP\n";
		result+="SUB\n";

		result+="alloc" + i + ":\n";;
		result+="DUP\n";
		result+="STOREABS 3001\n"; // string length
		result+="TIMES\n";
		result+="DUP\n";
		result+="STOREABS 3005\n"; // new string length
		result+="PUSHIMM 1\n";
		result+="LESS\n";
		result+="JUMPC finalstrrepeat"+i+"\n";
		result+="PUSHABS 3005\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="MALLOC\n";
		result+="STOREABS 3002\n"; // new string address


		result+="copy" + i + ":\n";
    	result+="restart" + i + ":\n";
        result+="PUSHIMM 0\n";
        result+="STOREABS 3003\n"; // counter for original string
        result+="strrepeatiterate2" + i + ":\n";
		result+="PUSHABS 3003\n";
		result+="PUSHABS 3001\n";
		result+="EQUAL\n";
		result+="JUMPC restart"+i+"\n"; // reset counter to 0
		result+="PUSHABS 3004\n";
		result+="PUSHABS 3005\n";
		result+="EQUAL\n";
		result+="JUMPC finalstrrepeat"+i+"\n"; // end loop
		result+="PUSHABS 3000\n";
		result+="PUSHABS 3003\n";
		result+="ADD\n";
		result+="PUSHIND\n";
		result+="PUSHABS 3004\n";
		result+="PUSHABS 3002\n";
		result+="ADD\n";
		result+="SWAP\n";
		result+="STOREIND\n";
		result+="PUSHABS 3003\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="STOREABS 3003\n";
		result+="PUSHABS 3004\n";
		result+="PUSHIMM 1\n";
		result+="ADD\n";
		result+="STOREABS 3004\n";
		result+="JUMP strrepeatiterate2"+i+"\n";
		result+="finalstrrepeat" + i + ":\n";
		result+="PUSHABS 3004\n";
		result+="PUSHABS 3002\n";
		result+="ADD\n";
		result+="PUSHIMMCH '\0'\n";
		result+="STOREIND\n";
		result+="PUSHABS 3002\n";
		return result;
	}

	static String str_rev(int i){
		String result = "strrev"+ i + ":\n";
		result += "DUP\n";
		result += "STOREABS 3001\n"; // starting memory address
		result += "DUP\n";
		result += "STOREABS 3000\n"; // current pointer
		result += "DUP\n";
		result += "strreviterate1" + i + ":\n";
    	result += "PUSHIND\n";
    	result += "DUP\n";
    	result += "PUSHIMMCH '\0'\n";
    	result += "EQUAL\n";
    	result += "JUMPC alloc"+i+"\n";
    	result += "PUSHABS 3000\n";
    	result += "PUSHIMM 1\n";
    	result += "ADD\n";
    	result += "DUP\n";
    	result += "STOREABS 3000\n";
    	result += "JUMP strreviterate1"+i+"\n";
		result += "alloc" + i + ":\n";
    	result += "ADDSP -1\n";
    	result += "PUSHABS 3000\n";
    	result += "PUSHABS 3001\n";
    	result += "SUB\n";
    	result += "DUP\n";
    	result += "STOREABS 3002\n"; // length
    	result += "PUSHIMM 1\n";
    	result += "ADD\n";
    	result += "MALLOC\n";
    	result += "DUP\n";
    	result += "STOREABS 3001\n"; // starting memory address
		result += "DUP\n";
		result += "STOREABS 3000\n"; // current pointer
		result += "PUSHABS 3002\n";
		result += "PUSHIMM 0\n";
		result += "EQUAL\n";
		result += "JUMPC finalstrrev"+i+"\n";
		result += "write" + i + ":\n";
		result += "PUSHIMM 1\n";
		result += "PUSHABS 3002\n";
		result += "EQUAL\n";
		result += "JUMPC push"+i+"\n";
		result += "PUSHABS 3002\n";
		result += "PUSHIMM 1\n";
		result += "SUB\n";
		result += "STOREABS 3002\n";
		result += "SWAP\n";
		result += "STOREIND\n";
		result += "PUSHABS 3000\n";
		result += "PUSHIMM 1\n";
		result += "ADD\n";
		result += "DUP\n";
		result += "STOREABS 3000\n";
		result += "JUMP write"+i+"\n";
		result += "push" + i + ":\n";
		result += "SWAP\n";
		result += "STOREIND\n";
		result += "PUSHABS 3000\n";
		result += "PUSHIMM 1\n";
		result += "ADD\n";
		result += "finalstrrev" + i + ":\n";
		result += "PUSHIMMCH '\0'\n";
		result += "STOREIND \n";
		result += "ADDSP -1\n";
		result += "PUSHABS 3001\n";

		return result;
	}
}
