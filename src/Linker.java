import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * The linker processes the input twice (that is why it is called two-pass). 
 * Pass one determines the base address for each module and produces a symbol table containing the absolute address for each deﬁned symbol.
 * We assume the ﬁrst module has base address zero; the base address of module M+1 is equal to the base address of module M plus the length of module M. 
 * The absolute address for symbol S deﬁned in module M is the base address of M plus the relative address of S within M.
 * Pass two uses the base addresses and the symbol table computed in pass one to generate the actual output by relocating relative addresses and resolving external references.
 * @author Pu Zhao
 * Spring 2018, Feb. 4th 2018
 */
public class Linker {
	private int machineMemorySize = 200;

	private ArrayList<Module> modules = new ArrayList<Module>();
	private ArrayList<Symbol> definedSymbolTable = new ArrayList<Symbol>();
	private ArrayList<errorInt> memoryMap = new ArrayList<errorInt>();

	public static void main(String[] args) throws IOException {

		String filePath;
		// int inputFile = 6;
//	 for (int inputFile=1; inputFile<10; inputFile++){
//		 filePath = "inputs/input-" + inputFile + ".txt"; 
		if (args.length > 0)
			filePath = args[0];
		else
			throw new IllegalArgumentException("Need inputs.");
		
		new Linker(filePath);
//	 }

	}

	/**
	 * The symbol variables used in the program.
	 */
	private class Symbol {
		private String symbol;
		private Integer location;
		//private Integer oriLoc;

		private Integer moduleNumber;
		private boolean usedSomewhere;
		private boolean multiDefined;

		public Symbol(String symbol, Integer location) {
			this.symbol = symbol;
			this.location = location;
			this.usedSomewhere = false;
			this.multiDefined = false;
		}

		public Symbol(String symbol) {
			this.symbol = symbol;
			this.location = null;
			this.usedSomewhere = false;
			this.multiDefined = false;
		}

		public String toString() {
			return symbol.toString();
		} 
	}

	/**
	 * The program text consists of a count NT followed by NT pairs (type, word), 
	 * where word is a 4-digit instruction described above and type is a single character indicating 
	 * if the address in the word is Immediate, Absolute, Relative, or External. NT is thus the length of the module.
	 * such as "R 8001 E 1000 E 1000 E 3000 R 1002 A 1010"
	 */
	private class TextInstruction {
		private Character classification;
		private Integer opcode;
		private Integer address;

		public TextInstruction(Character classification, Integer opcode,
				Integer address) {
			this.classification = classification;
			this.opcode = opcode;
			this.address = address;
		}

		public TextInstruction(Character classification) {
			this(classification, null, null);
		}

		public String toString() {
			return Integer.toString((this.opcode * 1000) + this.address);
		}
	}

	/**
	 * Represents a single module in the input file, which is composed of a
	 * definition list, a use-list and an instruction list.
	 */
	
	private class Module {
		private int startLocation;
		private int endLocation;
		private int length;

		public List<Symbol> definitions;
		public List<Symbol> uses;
		public List<TextInstruction> textInstructions;
		public List<String> notUsedSymbols;
		public List<String> redefinedLocSymbols;

		public Module(int startLocation) {
			this.startLocation = startLocation;
			this.endLocation = startLocation;
			this.length = 0;

			this.definitions = new ArrayList<Symbol>();
			this.uses = new ArrayList<Symbol>();
			this.textInstructions = new ArrayList<TextInstruction>();

			this.notUsedSymbols = new ArrayList<String>();
			this.redefinedLocSymbols = new ArrayList<String>();
		}

		public void addDefinition(Symbol symbol) {
			this.definitions.add(symbol);
		}

		public void addUse(Symbol symbol) {
			this.uses.add(symbol);
		}

		public void addInstruction(TextInstruction instruction) {
			this.textInstructions.add(instruction);
			this.length++;
			this.endLocation = this.startLocation + this.length - 1;
		}

		public String toString() {
			
			return Integer.toString(startLocation);

		} 
	}

	/**error integer which is the value in the map that has error message
	 */
	private class errorInt {
		private int memoryValue;
		private String errorMsg;

		public errorInt(int memoryValue, String errorMsg) {
			this.memoryValue = memoryValue;
			this.errorMsg = errorMsg;
		}

		public String getErrorMsg() {
			return this.errorMsg;
		}


	}

	private Symbol currSym;
	private TextInstruction currInstr;
	private Module currModule;

	/**
	 * Read data from input and create the modules
	 * 
	 * @param inputFilePath
	 *            Specifies the path to the relevant input file.
	 * @throws FileNotFoundException
	 *             If the file-path that was provided did not lead to a text
	 *             file.
	 */
	public Linker(String inputFilePath) throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(inputFilePath));
		int moduleNumber = scanner.nextInt();
		//System.out.println(moduleNumber);
		for(int i=0;i<moduleNumber;i++){
			
			int startLocation = 0;
			
			if (!this.modules.isEmpty()) 
				startLocation = this.modules.get(this.modules.size() - 1).endLocation + 1;
			
			this.currModule = new Module(startLocation);
			this.modules.add(this.currModule);
			
			int definitionNumber = scanner.nextInt();
			
			for(int defNum=0; defNum<definitionNumber; defNum++){
				this.currSym = new Symbol(scanner.next());
				String symbol = this.currSym.symbol;
				int location = Integer.parseInt(scanner.next());
				this.currSym = new Symbol(symbol, location);
				this.currModule.addDefinition(this.currSym);			
			}
			
			int usesNumber = scanner.nextInt();
			
			for(int usesNum=0; usesNum<usesNumber; usesNum++)
				this.currModule.addUse(new Symbol(scanner.next()));

			
			int instrNumber = scanner.nextInt();
			
			for(int instrNum=0; instrNum<instrNumber; instrNum++){
				this.performInstruction(scanner.next(), "TYPE");				
				this.performInstruction(scanner.next(), "WORD");	
				
			}
			if (!this.modules.isEmpty())
				this.setAbsoluteAddress(this.modules.get(this.modules.size() - 1));
			}

		this.performSecondPass();
	}



	private void performInstruction(String element, String Type) {

		if (Type == "TYPE") {
			char classification = element.charAt(0);
			this.currInstr = new TextInstruction(classification);

		} else if (Type == "WORD") {
			char classification = this.currInstr.classification;
			int opcode = Character.getNumericValue(element.charAt(0));
			int address = Integer.parseInt(element.substring(1));
			this.currInstr = new TextInstruction(classification, opcode,
					address);
			this.currModule.addInstruction(this.currInstr);
		}

	}

	/**
	 * Set absolute address in the module and detect
	 * certain errors needed to be output
	 */
	private void setAbsoluteAddress(Module module) {

		for (Symbol currSymbol : module.definitions) { 
			boolean defined=false;
			
			for (Symbol symbolInTable : definedSymbolTable){
				if (currSymbol.symbol.equals(symbolInTable.symbol)){
					symbolInTable.multiDefined=true;
					defined=true;
				}				
			}
			
			if (!defined){
				if (currSymbol.location == null)
					currSymbol.location = 0;
				if (currSymbol.location > module.length){
					currSymbol.location = 0 + module.startLocation;
					module.redefinedLocSymbols.add(currSymbol.symbol);
				}
				else 
					currSymbol.location = currSymbol.location + module.startLocation;;

				currSymbol.moduleNumber = this.modules.size()-1;
				
				definedSymbolTable.add(currSymbol);
				
			}
		}

	}

	/**
	 * Perform second pass during which detect all errors that requires
	 * and to form a linker result as required
	 * 
	 */
	private void performSecondPass() {

		String symbolName;
		String errorMsg;
		Integer relativeAddress;
		Integer absoluteAddress;
		for (Module module : this.modules) {
			for (Symbol use : module.uses) 
				for (Symbol symbolInTable : definedSymbolTable)
					if (use.symbol.equals(symbolInTable.symbol)) 
						module.notUsedSymbols.add(use.symbol);
				
			
			for (TextInstruction instr : module.textInstructions) {
				boolean notDefined=true;
				errorMsg = null;
				relativeAddress = instr.address;
				absoluteAddress = instr.address;
	        	
	        	if (instr.classification == 'E') {
					if (module.uses.size() <= relativeAddress) {
						errorMsg = "Error: External address exceeds length of use list; treated as immediate.";

					} else 
					  { 
						symbolName = module.uses.get(relativeAddress).symbol;
						for (Symbol symbolInTable : definedSymbolTable)
							if (symbolName.equals(symbolInTable.symbol)) {
								absoluteAddress = symbolInTable.location;
								notDefined=false;
								symbolInTable.usedSomewhere = true;
							}
						if (notDefined == true) {
							errorMsg = "Error: " + symbolName
									+ " is not defined; zero used.";
							instr.address = 0;
						} else {
							module.notUsedSymbols.remove(symbolName);
						}
					  }
				}  // according to sample output when both "Error: Absolute address exceeds machine size; zero used." and 
					   // "Error: External address exceeds length of use list; treated as immediate." take place,
					   // only deal with the latter situation.
					  
				if (instr.classification == 'A') {
						if (absoluteAddress >= this.machineMemorySize) {
							errorMsg = "Error: Absolute address exceeds machine size; zero used.";
							absoluteAddress = 0;
							}
				}
	        	
	        	if (instr.classification == 'R') {
					absoluteAddress = relativeAddress + module.startLocation;

					if (relativeAddress > module.length) {
						errorMsg = "Error: Relative address exceeds module size; zero used.";
						absoluteAddress = 0;
					}

				} 

				this.memoryMap
						.add(new errorInt(instr.opcode * 1000 + absoluteAddress, errorMsg));
			}
		}

		this.displayOutput();
	}

	/**
	 * Display the results including: "Symbol Table",
	 * "Memory Map" and Warnings and Errors
	 */
	private void displayOutput() {

		System.out.println("Symbol Table");
		
		for (Symbol symbolInTable : definedSymbolTable){
			System.out.print(symbolInTable.symbol + "="
					+ symbolInTable.location);
			if(symbolInTable.multiDefined==true)
				System.out.print(" Error: This variable is multiply defined; first value used.");
			System.out.println();
		}


		System.out.println();


		System.out.println("Memory Map");

		int counter = 0;
		for (errorInt memoryEntry : this.memoryMap) {

			System.out.printf("%-3s %s", counter + ":", memoryEntry.memoryValue);

			if (memoryEntry.getErrorMsg() != null)
				System.out.print(" " + memoryEntry.getErrorMsg());

			System.out.println();
			counter++;
		}

		int moduleCounter = 0;
		for (Module currModule : this.modules) {

			for (String badSymbol : currModule.notUsedSymbols) {
				System.out
						.println("Warning: In module "
								+ moduleCounter
								+ " "
								+ badSymbol
								+ " appeared in the use list but was not actually used.");
			}

			moduleCounter++;
		}
		System.out.println();

		for (Symbol descSymbol : this.definedSymbolTable
				) {

			if (!descSymbol.usedSomewhere){
				//System.out.println();
				System.out.println("Warning: " + descSymbol.symbol
						+ " was defined in module "
						+ descSymbol.moduleNumber + " but never used.");
			}
				
		}
		System.out.println();
		
		int moduleCounter1 = 0;
		for (Module currModule : this.modules) {

			for (String badSymbol : currModule.redefinedLocSymbols) {
				System.out
						.println("Error: In module "
								+ moduleCounter1
								+ " the def of "
								+ badSymbol
								+ " exceeds the module size; zero (relative) used.");
			}

			moduleCounter1++;
		}
		
		

	}



}
