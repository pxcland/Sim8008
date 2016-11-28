/*	Sim8008.java
	Main Class
	
	Written by Patrick Cland
	CE2336 Term Project Fall 2016	*/
	
	
import java.util.*;
	
public class Sim8008
{
	public static void main(String[] args)
	{
		i8008 SYS = new i8008();
		AS8008 Assembler = new AS8008();		
		
		Scanner S = new Scanner(System.in);
		String choice = new String();
		
		/* If ran without any arguments, open up this interface */
		if(args.length == 0)
		{
			System.out.printf("\n== Sim8008 =========================\nIntel 8008 Simulator & Assembler\n====================================\n");
			do
			{	
				System.out.print("Enter E to run program, A to assemble, X to exit: ");
				choice = S.next().toUpperCase();
			}while(!choice.equals("E") && !choice.equals("A") && !choice.equals("X"));
			
			if(choice.equals("X"))
				return;
			else if(choice.equals("E"))
			{
				SYS.powerOn();
				System.out.print("Enter program file: ");
				SYS.loadProgramToRAM(S.next());
				SYS.execute();
			}
			else if(choice.equals("A"))
			{
				System.out.print("Enter source file: ");
				Assembler.setSrcFilePath(S.next());
				System.out.print("Enter destination file: ");
				Assembler.setDstFilePath(S.next());
				
				Assembler.openSrcFile();
				Assembler.openDstFile();
				
				Assembler.assemble();
				
				Assembler.closeSrcFile();
				Assembler.closeDstFile();
			}
		}
		
		/* Assemble files */
		else if(args.length == 3 && args[0].equals("-a"))
		{
			Assembler.setSrcFilePath(args[1]);
			Assembler.setDstFilePath(args[2]);
			
			Assembler.openSrcFile();
			Assembler.openDstFile();
			
			Assembler.assemble();
			
			Assembler.closeSrcFile();
			Assembler.closeDstFile();
		}
		
		/* Execute program */
		else if(args.length == 2 && args[0].equals("-e"))
		{
			SYS.powerOn();
			SYS.loadProgramToRAM(args[1]);
			SYS.execute();
		}
		
		/* Incorrect usage */
		else
		{
			System.out.println("\nUsage:\njava Sim8008 <no arguments> - Run program with interface.");
			System.out.println("java Sim8008 -a <source file> <destination file> - Assemble source file.");
			System.out.println("java Sim8008 -e <source file> - Execute source file\n\n");
		}
				
	}
}