/*	AS8008.java
	Assembler for Intel 8008 assembly language
	
	Written by Patrick Cland
	CE2336 Term Project Fall 2016	*/
	
import java.io.*;
import java.util.*;

public class AS8008
{
	/* Path names */
	private String srcFilePath;
	private String dstFilePath;
	
	/* Actual handles for the files */
	private RandomAccessFile srcFile;
	private RandomAccessFile dstFile;
	
	public void setSrcFilePath(String path)
	{
		srcFilePath = path;
	}
	
	public void setDstFilePath(String path)
	{
		dstFilePath = path;
	}
	
	public void openSrcFile()
	{
		try
		{
			srcFile = new RandomAccessFile(srcFilePath,"r");
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Cannot open " + srcFilePath);
		}
	}
	
	public void openDstFile()
	{
		try
		{
			dstFile = new RandomAccessFile(dstFilePath,"rw");
			/* Delete if anything already exists */
			dstFile.setLength(0);
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Cannot open " + dstFilePath);
		}
		catch(IOException e)
		{
			System.out.println("File error while assembling");
		}
	}
	
	public void closeSrcFile()
	{
		try
		{
			srcFile.close();
		}
		catch(IOException e)
		{
			System.out.println("Cannot close " + srcFilePath);
		}
	}
	
	public void closeDstFile()
	{
		try
		{
			dstFile.close();
		}
		catch(IOException e)
		{
			System.out.println("Cannot close " + dstFilePath);
		}
	}
	
	public int parseImmediate(String imm)
	{
		String tmp = imm.substring(1);
		/* $ denotes hex, # denotes decimal, % denotes binary */
		/* 0xDEADDEAD is returned on erroneous input */
		switch(imm.charAt(0))
		{
			case '$':
				return Integer.parseInt(tmp,16);
			case '#':
				return Integer.parseInt(tmp,10);
			case '%':
				return Integer.parseInt(tmp,2);
			default:
				return 0xDEADDEAD;
		}
	}
	
	public void assemble()
	{
		/* Running counter for how large the file is, used to calculate label currentAddressesses */
		int currentAddress = 0;
		/* An instruction varies from 1-3 bytes, or 4 for a pseudo instruction */
		int instructionLength = 0;
		
		int currentLineNumber = 1;
		
		/* An opcode is 1-3 bytes, pseudo instruction is 4 */
		byte[] opcode = new byte[4];
		
		Map<String,Integer> labelAddress = new HashMap<String,Integer>();
		
		String lineBuffer = new String();
		
		String label = new String();
		String mnemonic = new String();
				
		boolean hasSyntaxError = false;
		/* The first pass calculates the addresses of all the labels */
		/* The second pass assembles the file */
		boolean onFirstPass = true;
		
		/* Following the standard of the official assembler for the 8008 by intel */
		/* A line of code takes the following form */
		/* LABEL INSTRUCTION IMMEDIATE COMMENT */
		
		try
		{
			/* Assert dstFile is at the beginning */
			dstFile.seek(0);
			
			while((lineBuffer = srcFile.readLine()) != null || onFirstPass)
			{
				if(lineBuffer == null && onFirstPass)
				{
					/* Reset to read again */
					srcFile.seek(0);
					onFirstPass = false;
					currentLineNumber = 1;
					currentAddress = 0;
					instructionLength = 0;
					continue;
				}
					
				/* Skip past empty lines */
				if(lineBuffer.trim().isEmpty())
				{
					currentLineNumber++;
					continue;
				}

				/* Assemble */
				/* Make everything uppercase for consistency */
				Scanner S = new Scanner(lineBuffer.toUpperCase());
				
				/* If the line starts with an / symbol, the whole line is a comment */
				if(lineBuffer.charAt(0) == ';')
				{
					currentLineNumber++;
					continue;
				}
				
				/* Labels are left justified. If there is a whitespace at position 0, assume no label */				
				/* If the current line has a label, then get it. If not, there is no label */
				if(!Character.isWhitespace(lineBuffer.charAt(0)))
				{
					label = S.next();
					if(onFirstPass)
					{
						if(labelAddress.containsKey(label))
						{
							hasSyntaxError = true;
							System.out.printf("Syntax error on line %d: Duplicate label %s\n", currentLineNumber, label);
							return;
						}
						labelAddress.put(label,currentAddress);
					}
				}
				
				
				/* Get the mnemonic */
				mnemonic = S.next();
				/* ---------------------------------------------- */
				/* MASSIVE SWITCH STATEMENT FOR THE MNEMONICS LOL */
				/* ---------------------------------------------- */
				/* Pseudo Instruction */
				/* PAM - LHI and LHL at once */
				if(mnemonic.equals("PAM"))
				{
					instructionLength = 4;
					currentAddress += 4;
					
					byte hi = 0, lo = 0;
					
					/* If on the first pass, just calculate the offsets, don't assemble anything */
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					String imm16 = S.next();
					/* Only supports a 16 bit hexadecimal number */
					if(imm16.charAt(0) != '$' && imm16.charAt(0) != '#' && imm16.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(imm16))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(imm16) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(imm16) & 0x000000FF);
						}
					}
					
					else
					{
						hi = (byte)((parseImmediate(imm16) & 0x0000FF00) >>> 8);
						lo = (byte)((parseImmediate(imm16) & 0x000000FF));
					}
						opcode[0] = 0b00101110;
						opcode[1] = (byte)hi;
						opcode[2] = 0b00110110;
						opcode[3] = (byte)lo;
				}
				
				/* Assembler directives */
				else if(mnemonic.equals("ORG"))
				{
					int newOrigin = 0;
					
					if(!S.hasNext())
					{
						hasSyntaxError = true;
					}
					else
					{
						currentAddress = parseImmediate(S.next());
					}
					
					/* Change the file pointer */
					dstFile.seek(currentAddress);
					currentLineNumber++;
					continue;
				}
				/* Define Byte */
				else if(mnemonic.equals("DB"))
				{
					instructionLength = 1;
					currentAddress++;
					
					if(!S.hasNext())
						hasSyntaxError = true;
					
					opcode[0] = (byte)(parseImmediate(S.next()) & 0x000000FF);
				}
				/* Define null terminated string */
				else if(mnemonic.equals("ASCIIZ"))
				{
					/* Max length is 255 characters */
					byte str[] = new byte[255];
					int length = 0;
					while(S.hasNext())
					{
						String tmp = S.next();
						for(int i = 0; i < tmp.length(); i++)
						{
							str[length + i] = (byte)tmp.charAt(i);
						}
						/* Add space that was skipped */
						length += tmp.length() + 1;
						str[length-1] = (byte)0x20;
					}
					/* Append null terminator */
					str[length-1] = 0;
					
					instructionLength = length;
					currentAddress += length;
					if(onFirstPass)
					{
						currentLineNumber++;
					}
					else
					{
						dstFile.write(str,0,instructionLength);
						currentLineNumber++;
						continue;
					}
					
				}
				/* Define NON null terminated string */
				else if(mnemonic.equals("ASCII"))
				{
					/* Max length is 255 characters */
					byte str[] = new byte[255];
					int length = 0;
					while(S.hasNext())
					{
						String tmp = S.next();
						for(int i = 0; i < tmp.length(); i++)
						{
							str[length + i] = (byte)tmp.charAt(i);
						}
						/* Add space that was skipped */
						length += tmp.length() + 1;
						str[length-1] = (byte)0x20;
					}
					/* Append null terminator */
					
					instructionLength = length-1;
					currentAddress += length-1;
					if(onFirstPass)
					{
						currentLineNumber++;
					}
					else
					{
						dstFile.write(str,0,instructionLength);
						currentLineNumber++;
						continue;
					}
					
				}

				/* HLT */
				else if(mnemonic.equals("HLT"))
				{
					instructionLength = 1;
					currentAddress++;
					
					opcode[0] = (byte)0b11111111;
				}
				
				/* Load instruction */
				else if(mnemonic.charAt(0) == 'L')
				{
					/* Lr1r2 */
					if((mnemonic.charAt(1) != 'M' && mnemonic.charAt(1) != 'I') && (mnemonic.charAt(2) != 'M' && mnemonic.charAt(2) != 'I'))
					{
						instructionLength = 1;
						currentAddress++;
						byte DDD = (byte)(mnemonic.charAt(1) - 'A');
						byte SSS = (byte)(mnemonic.charAt(2) - 'A');
						
						if(mnemonic.charAt(1) == 'H')
							DDD = (byte)0b101;
						else if(mnemonic.charAt(1) == 'L')
							DDD = (byte)0b110;
						if(mnemonic.charAt(2) == 'H')
							SSS = (byte)0b101;
						else if(mnemonic.charAt(2) == 'L')
							SSS = (byte)0b110;
						
						if(DDD > 0b110 || SSS > 0b110)
							hasSyntaxError = true;
						else
							opcode[0] = (byte)(0b11000000 | (DDD << 3) | (SSS));
					}
					/* LrM */
					else if((mnemonic.charAt(1) != 'M' && mnemonic.charAt(1) != 'I') && (mnemonic.charAt(2) == 'M'))
					{
						instructionLength = 1;
						currentAddress++;
						byte DDD = (byte)(mnemonic.charAt(1) - 'A');
						if(mnemonic.charAt(1) == 'H')
							DDD = (byte)0b101;
						else if(mnemonic.charAt(1) == 'L')
							DDD = (byte)0b110;
						
						if(DDD > 0b110)
							hasSyntaxError = true;
						else
							opcode[0] = (byte)(0b11000111 | (DDD << 3));
					}
					/* LMr */
					else if((mnemonic.charAt(1) == 'M') && (mnemonic.charAt(2) != 'I'))
					{
						instructionLength = 1;
						currentAddress++;
						byte SSS = (byte)(mnemonic.charAt(2) - 'A');
						if(mnemonic.charAt(2) == 'H')
							SSS = (byte)0b101;
						else if(mnemonic.charAt(2) == 'L')
							SSS = (byte)0b110;
						
						if(SSS > 0b110)
							hasSyntaxError = true;
						else
							opcode[0] = (byte)(0b11111000 | SSS);
					}
					/* LrI */
					else if((mnemonic.charAt(1) != 'M' && mnemonic.charAt(1) != 'I') && (mnemonic.charAt(2) == 'I'))
					{
						instructionLength = 2;
						currentAddress += 2;
						
						byte DDD = (byte)(mnemonic.charAt(1) - 'A');
						int imm = parseImmediate(S.next());
						if(mnemonic.charAt(1) == 'H')
							DDD = (byte)0b101;
						else if(mnemonic.charAt(1) == 'L')
							DDD = (byte)0b110;
						
						if(DDD > 0b110 || imm == 0xDEADDEAD)
							hasSyntaxError = true;
						else
						{
							opcode[0] = (byte)(0b00000110 | (DDD << 3));
							opcode[1] = (byte)imm;
						}
					}
					/* LMI */
					else if((mnemonic.charAt(1) == 'M') && (mnemonic.charAt(2) == 'I'))
					{
						instructionLength = 2;
						currentAddress += 2;
						
						int imm = parseImmediate(S.next());
						if(imm == 0xDEADDEAD)
							hasSyntaxError = true;
						else
						{
							opcode[0] = (byte)0b00111110;
							opcode[1] = (byte)imm;
						}
					}
				}
				/* INr - Increment */
				else if(mnemonic.charAt(0) == 'I')
				{
					instructionLength = 1;
					currentAddress++;
					
					byte DDD = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						DDD = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						DDD = (byte)0b110;
					
					if(DDD > 0b110 || DDD == 0)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b00000000 | (DDD << 3));		
					
				}
				/* DCr - Decrement */
				else if(mnemonic.charAt(0) == 'D')
				{
					instructionLength = 1;
					currentAddress++;
					
					byte DDD = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						DDD = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						DDD = (byte)0b110;
					
					if(DDD > 0b110 || DDD == 0)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b00000001 | (DDD << 3));
				}
				/* ADr */
				else if((mnemonic.charAt(0) == 'A') && (mnemonic.charAt(1) == 'D') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10000000 | SSS);
				}
				/* ACr */
				else if((mnemonic.charAt(0) == 'A') && (mnemonic.charAt(1) == 'C') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10001000 | SSS);
				}
				/* SUr */
				else if((mnemonic.charAt(0) == 'S') && (mnemonic.charAt(1) == 'U') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10010000 | SSS);
				}
				/* SBr */
				else if((mnemonic.charAt(0) == 'S') && (mnemonic.charAt(1) == 'B') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10011000 | SSS);
				}
				/* NDr */
				else if((mnemonic.charAt(0) == 'N') && (mnemonic.charAt(1) == 'D') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10100000 | SSS);
				}
				/* XRr */
				else if((mnemonic.charAt(0) == 'X') && (mnemonic.charAt(1) == 'R') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10101000 | SSS);
				}
				/* ORr */
				else if((mnemonic.charAt(0) == 'O') && (mnemonic.charAt(1) == 'R') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10110000 | SSS);
				}
				/* CPr */
				else if((mnemonic.charAt(0) == 'C') && (mnemonic.charAt(1) == 'P') && (mnemonic.charAt(2) != 'M') && (mnemonic.charAt(2) != 'I'))
				{
					instructionLength = 1;
					currentAddress++;
					
					byte SSS = (byte)(mnemonic.charAt(2) - 'A');
					if(mnemonic.charAt(2) == 'H')
						SSS = (byte)0b101;
					else if(mnemonic.charAt(2) == 'L')
						SSS = (byte)0b110;
					
					if(SSS > 0b110)
						hasSyntaxError = true;
					else
						opcode[0] = (byte)(0b10111000 | SSS);
				}
				/* ADM */
				else if(mnemonic.equals("ADM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10000111;
				}
				/* ACM */
				else if(mnemonic.equals("ACM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10001111;
				}
				/* SUM */
				else if(mnemonic.equals("SUM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10010111;
				}
				/* SBM */
				else if(mnemonic.equals("SBM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10011111;
				}
				/* NDM */
				else if(mnemonic.equals("NDM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10100111;
				}
				/* XRM */
				else if(mnemonic.equals("XRM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10101111;
				}
				/* ORM */
				else if(mnemonic.equals("ORM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10110111;
				}
				/* CPM */
				else if(mnemonic.equals("CPM"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b10111111;
				}
				
				/* ADI */
				else if(mnemonic.equals("ADI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00000100;
						opcode[1] = (byte)imm;
					}
				}
				/* ACI */
				else if(mnemonic.equals("ACI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00001100;
						opcode[1] = (byte)imm;
					}
				}
				/* SUI */
				else if(mnemonic.equals("SUI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00010100;
						opcode[1] = (byte)imm;
					}
				}
				/* SBI */
				else if(mnemonic.equals("SBI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00011100;
						opcode[1] = (byte)imm;
					}
				}
				/* NDI */
				else if(mnemonic.equals("NDI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00100100;
						opcode[1] = (byte)imm;
					}
				}
				/* XRI */
				else if(mnemonic.equals("XRI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00101100;
						opcode[1] = (byte)imm;
					}
				}
				/* ORI */
				else if(mnemonic.equals("ORI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00110100;
						opcode[1] = (byte)imm;
					}
				}
				/* CPI */
				else if(mnemonic.equals("CPI"))
				{
					instructionLength = 2;
					currentAddress += 2;
						
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = (byte)0b00111100;
						opcode[1] = (byte)imm;
					}
				}
				
				/* RLC */
				else if(mnemonic.equals("RLC"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b00000010;
				}
				/* RRC */
				else if(mnemonic.equals("RRC"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b00001010;
				}
				/* RAL */
				else if(mnemonic.equals("RAL"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b00010010;
				}
				/* RAR */
				else if(mnemonic.equals("RAR"))
				{
					instructionLength = 1;
					currentAddress++;
					opcode[0] = (byte)0b00011010;
				}
				
				/* JMP */
				else if(mnemonic.equals("JMP"))
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)0b01000100;
					opcode[1] = lo;
					opcode[2] = hi;
				}
				/* JFc */
				else if(mnemonic.charAt(0) == 'J' && mnemonic.charAt(1) == 'F')
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)(0b01000000 | (flag << 3));
					opcode[1] = lo;
					opcode[2] = hi;
				}
				
				/* JTc */
				else if(mnemonic.charAt(0) == 'J' && mnemonic.charAt(1) == 'T')
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)(0b01100000 | (flag << 3));
					opcode[1] = lo;
					opcode[2] = hi;
				}
				
				/* CAL */
				else if(mnemonic.equals("CAL"))
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)0b01000110;
					opcode[1] = lo;
					opcode[2] = hi;
				}
				
				/* CFc */
				else if(mnemonic.charAt(0) == 'C' && mnemonic.charAt(1) == 'F')
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)(0b01000010 | (flag << 3));
					opcode[1] = lo;
					opcode[2] = hi;
				}
				
				/* CTc */
				else if(mnemonic.charAt(0) == 'C' && mnemonic.charAt(1) == 'T')
				{
					instructionLength = 3;
					currentAddress += 3;
					
					if(onFirstPass)
					{
						currentLineNumber++;
						continue;
					}
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					String target = S.next();
					byte hi = 0, lo = 0;
					/* If not an immediate value, assume is a label */
					if(target.charAt(0) != '$' && target.charAt(0) != '#' && target.charAt(0) != '%')
					{
						/* If there is a bad label */
						if(!labelAddress.containsKey(target))
							hasSyntaxError = true;
						else
						{
							hi = (byte)((labelAddress.get(target) & 0x0000FF00) >>> 8);
							lo = (byte)(labelAddress.get(target) & 0x000000FF);
						}
					}
					else
					{
						hi = (byte)((parseImmediate(target) & 0x0000FF00) >>> 8);
						lo = (byte)(parseImmediate(target) & 0x000000FF);
					}
					opcode[0] = (byte)(0b01100010 | (flag << 3));
					opcode[1] = lo;
					opcode[2] = hi;
				}
				
				/* RET */
				else if(mnemonic.equals("RET"))
				{
					instructionLength = 1;
					currentAddress++;
					
					opcode[0] = (byte)(0b00000111);
				}
				/* RFc*/
				else if(mnemonic.charAt(0) == 'R' && mnemonic.charAt(1) == 'F')
				{
					instructionLength = 1;
					currentAddress++;
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					opcode[0] = (byte)(0b00000011 | (flag << 3));
				}
				
				/* RTc*/
				else if(mnemonic.charAt(0) == 'R' && mnemonic.charAt(1) == 'T')
				{
					instructionLength = 1;
					currentAddress++;
					
					byte flag = 0;
					
					switch(mnemonic.charAt(2))
					{
						case 'C':
							flag = 0b00;
							break;
						case 'Z':
							flag = 0b01;
							break;
						case 'S':
							flag = 0b10;
							break;
						case 'P':
							flag = 0b11;
							break;
						default:
							hasSyntaxError = true;
					}
					
					opcode[0] = (byte)(0b00100011 | (flag << 3));
				}
				/* FNC - Function Call */
				else if(mnemonic.equals("FNC"))
				{
					instructionLength = 2;
					currentAddress += 2;
					
					int imm = parseImmediate(S.next());
					if(imm == 0xDEADDEAD)
						hasSyntaxError = true;
					else
					{
						opcode[0] = 0b01001111;
						opcode[1] = (byte)imm;
					}
				}
				
				/* Anything else is considering a syntax error */
				else
				{
					hasSyntaxError = true;
				}
					
				
				
				
				if(hasSyntaxError)
				{
					System.out.printf("Syntax error on line %d: %s\n", currentLineNumber, mnemonic);
					/* If syntax error, destroy the file */
					dstFile.setLength(0);
					return;
				}
				
				if(onFirstPass)
				{
					currentLineNumber++;
					continue;
				}
				
				/* Write the bytes to the file */
				dstFile.write(opcode,0,instructionLength);
				
				System.out.printf("%04X | ",currentAddress-instructionLength);	
				
				if(instructionLength == 1)
					System.out.printf("%d\t%s\n",currentLineNumber,
						String.format("%8s",Integer.toBinaryString(opcode[0] & 0xFF).replace(" ", "0")));
				else if(instructionLength == 2)
					System.out.printf("%d\t%s %s\n",currentLineNumber,
						String.format("%8s",Integer.toBinaryString(opcode[0] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[1] & 0xFF).replace(" ", "0")));
				else if(instructionLength == 3)
					System.out.printf("%d\t%s %s %s\n",currentLineNumber,
						String.format("%8s",Integer.toBinaryString(opcode[0] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[1] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[2] & 0xFF).replace(" ", "0")));
				else if(instructionLength == 4)
					System.out.printf("%d\t%s %s %s %s\n",currentLineNumber,
						String.format("%8s",Integer.toBinaryString(opcode[0] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[1] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[2] & 0xFF).replace(" ", "0")),
						String.format("%8s",Integer.toBinaryString(opcode[3] & 0xFF).replace(" ", "0")));
				
				currentLineNumber++;
			}	
			
			System.out.println("File successfully assembled.");
		}
		catch(IOException e)
		{
			System.out.println("File error while assembling.");
		}
	}
	
}