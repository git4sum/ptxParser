// import ANTLR runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

// import Java Map Libs
import java.util.HashMap;
import java.util.Map;

// import Java Stack Libs
import java.util.*;

// import Java console IO
import java.io.Console;
import java.io.IOException;

import java.io.*;

import java.lang.*;

class InsertRuleContext {
	ParserRuleContext ctx;
	public InsertRuleContext(ParserRuleContext ctx) {
		this.ctx = ctx;
	}
	public <T extends ParseTree> T addAnyChildAt(T t, Integer offset) {
		if ( ctx.children==null ) ctx.children = new ArrayList<>();
		ctx.children.add(offset, t);
		return t;
	}

	public RuleContext addChildAt(RuleContext ctx, Integer offset){
		return addAnyChildAt(ctx, offset);
	}
}
public class PTX2PTX {
	protected static ParserRuleContext tree;

	public static Integer getNum(String str) {
		return Integer.parseInt(str.replaceAll("[^0-9]", "")); 
	}
	public static String getIdent(String str){
		return str.replaceAll("[%^0-9]", "");	
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
	    	System.err.println("Input Filename...");
	    	System.exit(1);
	    }

	    parseInitPTX(args[0]);
	    insertFunc(".weak .func  (.param .b32 func_retval0) cudaMalloc3(\n.param .b64 cudaMalloc_param_0,\n	.param .b64 cudaMalloc_param_1\n)\n{\n.reg .b32 	%r<2>;\n	st.param.b32	[func_retval0+0], %r1;\n	ret;\n}");
	    insertInst("BB0_55:\nadd.s32 	%r86, %r1, 193;\nsetp.lt.s32	%rd72, %r86, %r32;", "cudaMalloc", 0);
	    modifyOpcode("setp .lt .s32 ", "cudaMalloc", 1);
	    printPTX(tree, args[0]);
	}

	public static void parseInitPTX(String fileName) {
	    String input = "", tmp;
		
	    try {
	    	BufferedReader rd = new BufferedReader(new FileReader(fileName));
	    	while ((tmp = rd.readLine()) != null) {
	    	    input += tmp;
	        	input += "\n";
	    	}
	    	rd.close();
	    } catch (IOException e) {
	        System.err.println(e);
	        System.exit(1);
	    }

	    PTXLexer lexer = new PTXLexer(CharStreams.fromString(input));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    PTXParser parser = new PTXParser(tokens);
	    tree = parser.program();
	}

	public static void insertFunc(String inputPTX){
	    PTXLexer lexer = new PTXLexer(CharStreams.fromString(inputPTX));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    PTXParser parser = new PTXParser(tokens);

	    ParserRuleContext directiveTree = findDirTree();
	
	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTX2Listener listener = new PTX2Listener(parser, directiveTree);

	    walker.walk(listener, parser.directive());
	}

	public static ParserRuleContext findDirTree() {
	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTXfListener listener = new PTXfListener();
	    walker.walk(listener, tree);

	    return listener.dirtree;
	}

	public static void insertInst(String inputPTX, String funcName, int offset) {
	    PTXLexer lexer = new PTXLexer(CharStreams.fromString(inputPTX));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    PTXParser parser = new PTXParser(tokens);
                                                                                                                    
	    ParserRuleContext subtree = findSubTree(funcName);
	    	
	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTX2Listener listener = new PTX2Listener(parser, subtree, offset); //offset starts from zero(0)|
	    walker.walk(listener, parser.instructionList());

	    HashMap<String, Integer> newmap = listener.newmap;
	    PTXfListener maplistener = new PTXfListener(funcName, newmap);
	    walker.walk(maplistener, tree);
	}

	public static ParserRuleContext findSubTree(String funcName) {
	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTXfListener listener = new PTXfListener(funcName);
	    walker.walk(listener, tree);

	    if(!listener.funcExist) {
	    	System.out.println("No such a function . . .");
	    	System.exit(1);
	    }

	    return listener.subtree;
	}

	public static void printPTX(ParserRuleContext inputTree, String outputName) throws IOException {
	    ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
	    PTX2PTXListener listener = new PTX2PTXListener();
	    walker.walk(listener, inputTree); // initiate walk of tree with listener

	    // Output file
	    FileOutputStream output = new FileOutputStream(new File("output_"+outputName));
	    System.out.println("PTX 2 PTX output file name:  output_"+outputName);
	    output.write(listener.out.peek().toString().getBytes());
	    output.flush();
	    output.close();
	}

	public static void modifyOpcode(String inputOpcode, String funcName, int offset){
	    PTXLexer lexer = new PTXLexer(CharStreams.fromString(inputOpcode));
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    PTXParser parser = new PTXParser(tokens);

	    ParserRuleContext modtree = findModTree(funcName, inputOpcode, offset);

	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTX2Listener listener = new PTX2Listener(parser, modtree);
		
	    walker.walk(listener, parser.opcode());
	}

	public static ParserRuleContext findModTree(String funcName, String inputOpcode, int offset) {
	    ParseTreeWalker walker = new ParseTreeWalker();
	    PTXfListener listener = new PTXfListener(funcName, inputOpcode, offset);
	    walker.walk(listener, tree);
	    return listener.modtree;
	}
}

class PTX2Listener extends PTXBaseListener {
	PTXParser parser;
	int offset;
	ParserRuleContext context;

	int mapIndex = 0;
	HashMap<String, Integer> newmap = new HashMap<String, Integer>();
	PTX2Listener(PTXParser parser, ParserRuleContext context, int offset) {
		this.parser = parser;
		this.context = context;
		this.offset = offset;
	}
	PTX2Listener(PTXParser parser, ParserRuleContext context) {
		this.parser = parser;
		this.context = context;
	}
	
	@Override public void enterOpcode(PTXParser.OpcodeContext ctx){
		if(context instanceof PTXParser.OpcodeContext == true){
			TerminalNode id = (TerminalNode) context.getChild(0).getChild(0);
			CommonToken token = (CommonToken) id.getSymbol();
			token.setText(ctx.getChild(0).getText());
			for(int i = 1; i < ctx.getChildCount(); i++) {
				ParseTree child = ctx.getChild(i);
				if(child instanceof PTXParser.IdentifierContext == true)
					this.context.addChild((PTXParser.IdentifierContext) child);
				else if(child instanceof PTXParser.OplistContext == true)
					this.context.addChild((PTXParser.OplistContext) child);
			}
		}
	}

	@Override public void exitOperand(PTXParser.OperandContext ctx) {
		if(ctx.getChildCount() == 1){
			if(ctx.getChild(0).getChild(0) instanceof PTXParser.IdentifierContext == true){
				String tempText = ctx.getChild(0).getChild(0).getChild(0).getText();
				if(tempText.charAt(0) == '%'){
					String tempIdent = PTX2PTX.getIdent(tempText);
					Integer tempNum = PTX2PTX.getNum(tempText);
					if(newmap.containsKey(tempIdent)){
						if(newmap.get(tempIdent) <  tempNum){
							newmap.put(tempIdent, tempNum);
						}
					}
					else{
						newmap.put(tempIdent, tempNum);
					}
				}
			}
		}
	}
	@Override public void exitInstructionList(PTXParser.InstructionListContext ctx){
		if(context instanceof PTXParser.InstructionListContext == true){ // case: insert instruction
			for (int i = 0; i < ctx.getChildCount(); i++) {
				ParseTree child = ctx.getChild(i);
				if (child instanceof PTXParser.InstructionContext == true) {
					if (this.context instanceof ParserRuleContext == true){
						InsertRuleContext tmp = new InsertRuleContext(this.context);
						tmp.addChildAt((PTXParser.InstructionContext) child, offset+i);
					}
				}
			}
		}
	}
	@Override public void exitDirective(PTXParser.DirectiveContext ctx){
		if(context instanceof PTXParser.DirectiveListContext == true){ // case: insert function
			this.context.addChild(ctx);
		}
	}
}

class PTXfListener extends PTXBaseListener {
	String funcName;
	HashMap<String, Integer> map;
	Integer offset;
	String inputOpcode;

	boolean modifyOp = false;
	boolean mapExist = false, mapFunc = false;
	Integer mapIndex = 0;
	ParserRuleContext subtree, dirtree, modtree;
  	boolean funcExist = false, funcIn = false;

  	PTXfListener(){}
	PTXfListener(String funcName){
		this.funcName = funcName;
	}
	PTXfListener(String funcName, HashMap<String, Integer> map){
		this.funcName = funcName;
		this.map = map;
		mapExist = true;
	}
	PTXfListener(String funcName, String inputOpcode, Integer offset){
		this.funcName = funcName;
		this.inputOpcode = inputOpcode;
		this.offset = offset;
		modifyOp = true;
	}
	@Override public void enterRegvecVal(PTXParser.RegvecValContext ctx){
		if(mapExist && mapFunc){
			if(ctx.getChildCount() > 1){ // ctx is register declaration
				String regIdent = PTX2PTX.getIdent(ctx.getChild(0).getText());
				Integer regNum = PTX2PTX.getNum(ctx.getChild(1).getText());

				if(map.containsKey(regIdent)){
					if(regNum <=  map.get(regIdent)){ // need to change
						TerminalNode id = (TerminalNode) ctx.getChild(1);
						CommonToken token = (CommonToken) id.getSymbol();
						token.setText("<"+String.valueOf(map.get(regIdent)+1)+">");
					}
				}
			}
		}
	}
	@Override public void exitDeclarationList(PTXParser.DeclarationListContext ctx){
		mapFunc = false;
	}
	@Override public void enterKernelDirective(PTXParser.KernelDirectiveContext ctx){
		if(ctx.getChild(1).getText().equals(funcName)){
			funcExist = true;
			funcIn = true;
			mapFunc = true;
		}
	}
	@Override public void enterFunctionDirective(PTXParser.FunctionDirectiveContext ctx){
		if(ctx.getChild(1).getText().equals(funcName) || ctx.getChild(4).getText().equals(funcName)){
			funcExist = true;
			funcIn = true;
			mapFunc = true;
		}
	}
	@Override public void enterInstructionList(PTXParser.InstructionListContext ctx){
    	if(funcIn){
  			funcIn = false;
  			subtree = ctx;
  			if(modifyOp){
  				if(ctx.getChild(offset).getChild(0) instanceof PTXParser.OpcodeContext == true){
  					ParserRuleContext child = (ParserRuleContext)ctx.getChild(offset).getChild(0);
  					for(int i = 0; i < child.getChildCount(); i++){
  						child.removeLastChild();
  					}
  					modtree = child; //opcode to modify
  				}
			}
  		}
  	}
  	@Override public void exitDirectiveList(PTXParser.DirectiveListContext ctx){
  		dirtree = ctx;
  	}
}

class PTX2PTXListener extends PTXBaseListener {
	Stack<StringBuilder> out = new Stack<StringBuilder>();
	PTX2PTXListener(){
		out.push(new StringBuilder(""));
	}

	@Override public void exitProgram(PTXParser.ProgramContext ctx){
		out.peek().append("\n");
	}
	@Override public void exitModDirective(PTXParser.ModDirectiveContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterDirective(PTXParser.DirectiveContext ctx){
		out.peek().append("\n");
	}
	@Override public void exitDirective(PTXParser.DirectiveContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterParamList(PTXParser.ParamListContext ctx){
		out.peek().append("\n");
	}
	@Override public void exitParamList(PTXParser.ParamListContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterDeclarationList(PTXParser.DeclarationListContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterDeclaration(PTXParser.DeclarationContext ctx){
		out.peek().append("\t");
	}
	@Override public void exitDeclaration(PTXParser.DeclarationContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterInstructionList(PTXParser.InstructionListContext ctx){
		PTXParser.InstructionContext temp = (PTXParser.InstructionContext)ctx.getChild(ctx.getChildCount()-1);
		ctx.removeLastChild();
		if(temp instanceof PTXParser.InstructionContext == true){
			ctx.addChild(temp);
		}
		out.peek().append("\n");
	}
	@Override public void enterInstruction(PTXParser.InstructionContext ctx){
		if(ctx.getChild(0) instanceof PTXParser.LabelNameContext==false){
			out.peek().append("\t");
		} else{
			out.peek().append("\n");
		}
	}
	@Override public void exitInstruction(PTXParser.InstructionContext ctx){
		out.peek().append("\n");
	}
	@Override public void enterOperandList(PTXParser.OperandListContext ctx){
		out.peek().append(" ");
	}
	@Override public void visitTerminal(TerminalNode node){
		out.peek().append(node.getText()+" ");
	}
}
