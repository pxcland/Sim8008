/*	i8008.java
	Intel 8008 core interpreter and main system
	
	Written by Patrick Cland
	CE2336 Term Project Fall 2016	*/
	
import java.util.Scanner;
import java.io.*;
	
public class i8008
{
	/* The core has a register file */
	private RegFile RegisterFile = new RegFile();
	
	/* The address bus is 14 bits wide, allowing 0x3FFF bytes of addressable RAM */
	private byte[]	RAM;
	
	/* There is a virtual display available with 64x64 pixels */
	private VirtualDisplay VD = new VirtualDisplay();
	
	private byte	currentOpcode;
	private boolean	isExecuting;
	private boolean isHalted;
	
	/* These are the binary representations of number of the register */
	private final byte A = 0b000;
	private final byte B = 0b001;
	private final byte C = 0b010;
	private final byte D = 0b011;
	private final byte E = 0b100;
	/* Registers H and L form a 14 bit address with the upper 2 bits */
	/* of H counting as dont cares */
	private final byte H = 0b101;
	private final byte L = 0b110;
	
	/* Constructor, initializes the core system */
	i8008()
	{
		/* There are no guarantees that the processor will be in a known state at power on */
		RAM = new byte[0x3FFF];
		isExecuting = false;
		isHalted = false;
		currentOpcode = 0;
		RegisterFile.PC = 0;
	}
	
	public void powerOn()
	{
		isExecuting = true;
	}
	
	public void powerOff()
	{
		isExecuting = false;
		currentOpcode = 0;
		RegisterFile.PC = 0;
	}
	
	public void coreDump()
	{
		System.out.printf("\nA: 0x%X\tB: 0x%X\nC: 0x%X\tD: 0x%X\nE: 0x%X\tH: 0x%X\nL: 0x%X\tPC: 0x%X\n",
									RegisterFile.Register[A],
									RegisterFile.Register[B],
									RegisterFile.Register[C],
									RegisterFile.Register[D],
									RegisterFile.Register[E],
									RegisterFile.Register[H],
									RegisterFile.Register[L],
									(RegisterFile.PC & 0xFFFF));
		System.out.println("Carry: " + RegisterFile.FCarry + "\tParity: " + RegisterFile.FParity + "\tZero: " + RegisterFile.FZero + "\tSign: " + RegisterFile.FSign);
		System.out.printf("(HL): 0x%X\n",RAM[getEffectiveAddress()]);
		System.out.printf("PC: 0x%X\tS0: 0x%X\tS1: 0x%X\tS2: 0x%X\n\n",RegisterFile.PC & 0xFFFF,RegisterFile.Stack[0] & 0xFFFF,RegisterFile.Stack[1] & 0xFFFF,RegisterFile.Stack[2] & 0xFFFF);
	}
	
	/* Return bits 3-5 */
	public int get543(byte o)
	{
		return (o & 0b00111000) >>> 3;
	}
	/* Return bits 0-2 */
	public int get210(byte o)
	{
		return (o & 0b00000111);
	}
	
	public int getEffectiveAddress()
	{
		return ((RegisterFile.Register[H] << 8) | (RegisterFile.Register[L] & 0xFF)) & RegisterFile.HLMask;
	}
	
	public boolean zeroOf(byte r)
	{
		if(r == 0)
			return true;
		return false;
	}
	
	public boolean signOf(byte r)
	{
		if(((r >>> 8) & 0b1) == 1)
			return true;
		return false;
	}
	
	public boolean parityOf(byte r)
	{
		if((Integer.bitCount(r) % 2) == 0)
			return true;
		return false;
	}
	
	public void loadProgramToRAM(String path)
	{
		try
		{
			RandomAccessFile f = new RandomAccessFile(path,"r");
			f.seek(0);
			for(int i = 0; i < f.length(); i++)
			{
				RAM[i] = f.readByte();
			}
			f.close();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Cannot open source binary " + path);
		}
		catch(IOException e)
		{
			System.out.println("Cannot open source binary!");
		}
	}
	
	public void systemCall(byte code)
	{
		int effectiveAddress = getEffectiveAddress();
		Scanner s = new Scanner(System.in);
		String imm;
		
		switch(code)
		{
			/* Call 0. Print decimal integer addressed by HL */
			case 0:
				System.out.print(RAM[effectiveAddress] & 0xFF);
				break;
			/* Call 1. Print hexadecimal integer addressed by HL */
			case 1:
				System.out.printf("0x%X",RAM[effectiveAddress]);
				break;
			/* Call 2. Print null terminated string addressed by HL */
			case 2:
				for(int i = 0; RAM[effectiveAddress + i] != 0; i++)
				{
					System.out.printf("%c",RAM[effectiveAddress + i]);
				}
				break;
			/* Call 3. Read decimal integer from keyboard and store in memory addressed by HL */
			case 3:
				imm = s.next();
				int val = 0;
				for(int i = 0; i < imm.length(); i++)
				{
					val += (byte)(imm.charAt(i) - '0') * Math.pow(10,(imm.length() - 1) - i);
				}
				RAM[effectiveAddress] = (byte)val;
				break;
			/* Call 4. Read string from keyboard, null terminate it and store in sequential memory addressed by HL */
			case 4:
				imm = s.nextLine();
				for(int i = 0; i < imm.length(); i++)
					RAM[effectiveAddress + i] = (byte)imm.charAt(i);
				RAM[effectiveAddress + imm.length()] = 0;
				break;
			/* Call 5. Core Dump */
			case 5:
				System.out.printf("\nA: 0x%X\tB: 0x%X\nC: 0x%X\tD: 0x%X\nE: 0x%X\tH: 0x%X\nL: 0x%X\tPC: 0x%X\n",
									RegisterFile.Register[A],
									RegisterFile.Register[B],
									RegisterFile.Register[C],
									RegisterFile.Register[D],
									RegisterFile.Register[E],
									RegisterFile.Register[H],
									RegisterFile.Register[L],
									(RegisterFile.PC & 0xFFFF));
				System.out.println("Carry: " + RegisterFile.FCarry + "\tParity: " + RegisterFile.FParity + "\tZero: " + RegisterFile.FZero + "\tSign: " + RegisterFile.FSign);
				System.out.printf("(HL): 0x%X\n",RAM[getEffectiveAddress()]);
				break;
			/* Call 6. Print character addressed by HL */
			case 6:
				System.out.printf("%c",RAM[effectiveAddress]);
				break;
			/* Call 7. Initialize Virtual Display */
			case 7:
				VD.initializeDisplay();
				break;
			/* Call 8. Update Virtual Display */
			case 8:
				VD.updatePixelData(RAM);
				VD.repaint();
				break;
			/* Call 9. Sleep the amount of miliseconds as defined by the accumulator */
			case 9:
				try
				{
					Thread.sleep(RegisterFile.Register[A]);
				}
				catch(InterruptedException e){}
				break;
			/* Anything else is just a no op */
			default:
				break;
		}
	}
	
	public void execute()
	{
		/* Temporary byte to store the result of operation before putting in accumulator */
		byte result = 0;
		/* The immediate value to fetch after the opcode in memory */
		byte fetch = 0;
		/* For instructions which take a 2 byte operand, such as a jump */
		char address = 0;
		/* Offset received by combining H and L */
		int effectiveAddress = 0;
		
		/* Move down one line to make the window look cleaner */
		System.out.println("");
		
		while(isExecuting && !isHalted)
		{
			/* Always fetch the opcode pointed to by RegisterFile.PC and then move to the next instruction */
			currentOpcode = RAM[RegisterFile.PC++];
			/* The first 2 bits are a function code, in most cases */
			switch((currentOpcode & 0b11000000) >>> 6)
			{
				case 0b11:
					/* 0b11111111 is a HLT instruction */
					if(currentOpcode == (byte)0xFF)
					{
						isHalted = true;
						System.out.printf("\n\n*** HALT: \n");
						coreDump();
						continue;
					}
					/*******************************************************/
					/****** 11 DDD SSS - Load DDD with data from SSS ******/
					/*****************************************************/
					/* Lr1r2 - DDD != SSS != 111. Load r1 with contents of r2 */
					else if((get543(currentOpcode) != 0b111) && (get210(currentOpcode) != 0b111))
					{
						/* If DDD = SSS, the instruction is a no op */
						if(get543(currentOpcode) == get210(currentOpcode))
						{
							/* No op */
							continue;
						}
						else
						{
							RegisterFile.Register[get543(currentOpcode)] = RegisterFile.Register[get210(currentOpcode)];
						}
					}
					/* LrM - DDD != 111. Load register with contents of memory addressed by HL */
					else if(get210(currentOpcode) == 0b111)
					{
						effectiveAddress = getEffectiveAddress();
						RegisterFile.Register[get543(currentOpcode)] = RAM[effectiveAddress];
					}
					/*LMr - SSS != 111. Load memory addressed by HL with contents of register */
					else if(get543(currentOpcode) == 0b111)
					{
						effectiveAddress = getEffectiveAddress();
						RAM[effectiveAddress] = RegisterFile.Register[get210(currentOpcode)];
					}
					break;
					
					
				case 0b00:
					/* Opcode 0b00000000 is halt */
					if(currentOpcode == 0)
					{
						isHalted = true;
						continue;
					}
					/*******************************************************/
					/****** 00 DDD 110 - Load Data Immediate         ******/
					/*****************************************************/
					if(get210(currentOpcode) == 0b110)
					{
						byte immediate = RAM[RegisterFile.PC++];
						/*LrI - DDD != 111. Load immediate to register */
						if(get543(currentOpcode) != 0b111)
						{
							RegisterFile.Register[get543(currentOpcode)] = immediate;
						}
						/*LMI - DDD = 111. Load immediate to memory addressed by HL */
						else if(get543(currentOpcode) == 0b111)
						{
							effectiveAddress = getEffectiveAddress();
							RAM[effectiveAddress] = immediate;
						}
					}
					/*******************************************************/
					/****** 00 DDD 000/001 - Increment/Decrement Register */
					/*****************************************************/
					/* Increment DDD. Parity, Zero, Sign flags are set */
					/* Register A cannot be incremented. DDD != 000 */
					else if(get210(currentOpcode) == 0b000)
					{
						int DDD = get543(currentOpcode);
						RegisterFile.Register[DDD]++;
						RegisterFile.FParity = parityOf(RegisterFile.Register[DDD]);
						RegisterFile.FZero = zeroOf(RegisterFile.Register[DDD]);
						RegisterFile.FSign = signOf(RegisterFile.Register[DDD]);
					}
					/* Decrement DDD. Parity, Zero, Sign flags are set */
					/* Register A cannot be decremented. DDD != 000 */
					else if(get210(currentOpcode) == 0b001)
					{
						int DDD = get543(currentOpcode);
						RegisterFile.Register[DDD]--;
						RegisterFile.FParity = parityOf(RegisterFile.Register[DDD]);
						RegisterFile.FZero = zeroOf(RegisterFile.Register[DDD]);
						RegisterFile.FSign = signOf(RegisterFile.Register[DDD]);
					}
					
					/******************************************************/
					/*            ALU Immediate Instructions             */
					/*             00 XXX 100 , DDDD DDDD               */
					/***************************************************/
					else if(get210(currentOpcode) == 0b100)
					{
						byte imm = RAM[RegisterFile.PC++];
						/* ADI - XXX = 000. Add immediate to accumulator */
						if(get543(currentOpcode) == 0b000)
						{
							RegisterFile.FCarry = (((RegisterFile.Register[A] & 0xFF) + (imm & 0xFF)) > 0xFF) ? true : false;
							
							RegisterFile.Register[A] += imm;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ACI - XXX = 001. Add immediate and carry to accumulator */
						else if(get543(currentOpcode) == 0b001)
						{
							byte tmp = RegisterFile.FCarry ? (byte)1 : 0;
							/* If there is an overflow, set the carry bit */
							RegisterFile.FCarry = (((RegisterFile.Register[A] & 0xFF) + (imm & 0xFF) + tmp) > 0xFF) ? true : false;
							
							RegisterFile.Register[A] += (imm + tmp);
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SUI - XXX = 010. Subtract immediate from accumulator */
						else if(get543(currentOpcode) == 0b010)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~imm + 1));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~imm & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((imm & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
									
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SBI - XXX = 011. Subtract immediate with borrow from accumulator */
						else if(get543(currentOpcode) == 0b011)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~imm + 1) - (RegisterFile.FCarry ? 1 : 0));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~imm & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((imm & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
									
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* NDI - XXX = 100. Logical AND accumulator and immediate */
						else if(get543(currentOpcode) == 0b100)
						{
							/* Logical operations set carry to 0 */
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] &= imm;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* XRI - XXX = 101. Logical XOR accumulator and immediate */
						else if(get543(currentOpcode) == 0b101)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] ^= imm;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ORI - XXX = 110. Logical OR accumulator and immediate */
						else if(get543(currentOpcode) == 0b110)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] ^= imm;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* CPI - XXX = 111. Compare accumulator and immediate and set flags accordingly */
						else if(get543(currentOpcode) == 0b111)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~imm+1));
							RegisterFile.FParity = parityOf(tmp);
							RegisterFile.FSign = signOf(tmp);
							/* Equality is indicated by the zero flag */
							RegisterFile.FZero = zeroOf(tmp);
							/* Less than/greater than is indicated by the carry flag */
							RegisterFile.FCarry =	((((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((imm & 0b10000000) >>> 7)) |
										(((imm & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)) |
										(((tmp & 0b10000000) >>> 7) & ((~RegisterFile.Register[A] & 0b10000000) >>> 7)))
										== 0b01 ? true : false;
						}
					}
					/*******************************************************/
					/****** 00 XXX 010 -     Rotate Accumulator           */
					/*****************************************************/
					else if(get210(currentOpcode) == 0b010)
					{
						/* The only flag affected is the carry flag, which A7 or A0 goes into */
						/* RLC - XXX = 000. Rotate accumulator left */
						if(get543(currentOpcode) == 0b000)
						{
							RegisterFile.FCarry = ((RegisterFile.Register[A] & 0b10000000) >>> 7) == 1 ? true : false;
							RegisterFile.Register[A] = (byte)((RegisterFile.Register[A] << 1) | (RegisterFile.FCarry ? 1 : 0));
						}
						/* RRC - XXX = 001. Rotate accumulator right */
						else if(get543(currentOpcode) == 0b001)
						{
							RegisterFile.FCarry = (RegisterFile.Register[A] & 0b00000001) == 1 ? true : false;
							RegisterFile.Register[A] = (byte)((RegisterFile.Register[A] >>> 1) | ((RegisterFile.FCarry ? 1 : 0) << 7));
						}
						/* RAL - XXX = 010. Rotate accumulator left, extra precision with carry */
						else if(get543(currentOpcode) == 0b010)
						{
							byte tmp = RegisterFile.FCarry ? (byte)1 : 0;
							RegisterFile.FCarry = ((RegisterFile.Register[A] & 0b10000000) >>> 7) == 1 ? true : false;
							RegisterFile.Register[A] = (byte)((RegisterFile.Register[A] << 1) | (tmp == 1 ? 1 : 0));
						}
						/* RAR - XXX = 011. Rotate accumulator right, extra precision with carry */
						else if(get543(currentOpcode) == 0b011)
						{
							byte tmp = RegisterFile.FCarry ? (byte)1 : 0;
							RegisterFile.FCarry = (RegisterFile.Register[A] & 0b00000001) == 1 ? true : false;
							RegisterFile.Register[A] = (byte)((RegisterFile.Register[A] >>> 1) | ((tmp == 1? 1 : 0) << 7));
						}
					}
					/*******************************************************/
					/****** 00 XXX ZZZ -     Return from Subroutine       */
					/*****************************************************/
					/* RET - 00 XXX 111. Return unconditionally from subroutine */
					else if(get210(currentOpcode) == 0b111)
					{
						/* RegisterFile.Stack pops up one level */
						RegisterFile.PC = RegisterFile.Stack[0];
						for(int i = 0; i < 6; i++)
							RegisterFile.Stack[i] = RegisterFile.Stack[i+1];
						RegisterFile.Stack[6] = 0;
					}
					/* Conditional return */
					else if(get210(currentOpcode) == 0b011)
					{
						boolean willReturn = false;
						/* C4C3: 00:Carry	01:Zero		10:Sign		11:Parity */
						/* RFc - 00 0C4C3 011 - Return if C4C3 is false */
						if(((currentOpcode & 0b00100000) >>> 5) == 0)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									willReturn = !RegisterFile.FCarry ? true : false;
									break;
								case 0b01:
									willReturn = !RegisterFile.FZero ? true : false;
									break;
								case 0b10:
									willReturn = !RegisterFile.FSign ? true : false;
									break;
								case 0b11:
									willReturn = !RegisterFile.FParity ? true : false;
									break;
								default:
									break;
							}
							if(willReturn)
							{
								RegisterFile.PC = RegisterFile.Stack[0];
								for(int i = 0; i < 6; i++)
									RegisterFile.Stack[i] = RegisterFile.Stack[i+1];
								RegisterFile.Stack[6] = 0;
							}
						}
						else if(((currentOpcode & 0b00100000) >>> 5) == 1)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									willReturn = RegisterFile.FCarry ? true : false;
									break;
								case 0b01:
									willReturn = RegisterFile.FZero ? true : false;
									break;
								case 0b10:
									willReturn = RegisterFile.FSign ? true : false;
									break;
								case 0b11:
									willReturn = RegisterFile.FParity ? true : false;
									break;
								default:
									break;
							}
							if(willReturn)
							{
								RegisterFile.PC = RegisterFile.Stack[0];
								for(int i = 0; i < 6; i++)
									RegisterFile.Stack[i] = RegisterFile.Stack[i+1];
								RegisterFile.Stack[6] = 0;
							}
						}
					}
					
					
					break;
					

				case 0b10:
					/* ALU Index Register Instructions */
					/* 10 XXX SSS - SSS != 111 */
					if(get210(currentOpcode) != 0b111)
					{
						int SSS = get210(currentOpcode);
						/* ADr - XXX = 000. Add R to A and store in A */
						if(get543(currentOpcode) == 0b000)
						{
							/* If there is an overflow, set the carry bit */
							RegisterFile.FCarry = (((RegisterFile.Register[A] & 0xFF) + (RegisterFile.Register[SSS] & 0xFF)) > 0xFF) ? true : false;
							
							RegisterFile.Register[A] += RegisterFile.Register[SSS];
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ACr - XXX = 001. Add R + carry to A and store in A */
						else if(get543(currentOpcode) == 0b001)
						{
							byte tmp = RegisterFile.FCarry ? (byte)1 : 0;
							/* If there is an overflow, set the carry bit */
							RegisterFile.FCarry = (((RegisterFile.Register[A] & 0xFF) + (RegisterFile.Register[SSS] & 0xFF) + tmp) > 0xFF) ? true : false;
							
							RegisterFile.Register[A] += (RegisterFile.Register[SSS] + tmp);
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SUr - XXX = 010. Subtract R from A and put in A */
						else if(get543(currentOpcode) == 0b010)
						{
							/* If there is an underflow, set the carry bit */
							/* To be loyal to the spec, two's complement subtraction is used */
							/* Two of the same signs resulting in another sign sets the carry */
							byte tmp = (byte)(RegisterFile.Register[A] + (~RegisterFile.Register[SSS] + 1));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~RegisterFile.Register[SSS] & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((RegisterFile.Register[SSS] & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
									
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SBr - XXX = 011. Subtract R and borrow from A and put in A */
						else if(get543(currentOpcode) == 0b011)
						{
							/* Same as above, but with borrow */
							byte tmp = (byte)(RegisterFile.Register[A] + (~RegisterFile.Register[SSS] + 1) - (RegisterFile.FCarry ? 1 : 0));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~RegisterFile.Register[SSS] & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((RegisterFile.Register[SSS] & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
									
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* NDr - XXX = 100. Bitwise And of A and R and put in A */
						else if(get543(currentOpcode) == 0b100)
						{
							/* Logical operations set carry to 0 */
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] &= RegisterFile.Register[SSS];
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* XRr - XXX = 101. Bitwise XOR of A and R and put in A */
						else if(get543(currentOpcode) == 0b101)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] ^= RegisterFile.Register[SSS];
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ORr - XXX = 110. Bitwise OR of A and R and put in A */
						else if(get543(currentOpcode) == 0b110)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] |= RegisterFile.Register[SSS];
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* CPr - XXX = 111. Compare A and R and set flags accordingly */
						else if(get543(currentOpcode) == 0b111)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~RegisterFile.Register[SSS]+1));
							RegisterFile.FParity = parityOf(tmp);
							RegisterFile.FSign = signOf(tmp);
							/* Equality is indicated by the zero flag */
							RegisterFile.FZero = zeroOf(tmp);
							/* Less than/greater than is indicated by the carry flag */
							RegisterFile.FCarry =	((((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((RegisterFile.Register[SSS] & 0b10000000) >>> 7)) |
										(((RegisterFile.Register[SSS] & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)) |
										(((tmp & 0b10000000) >>> 7) & ((~RegisterFile.Register[A] & 0b10000000) >>> 7)))
										== 0b01 ? true : false;
						}
					}
					
					/* ALU Operations with Memory */
					/* 10 XXX 111 */
					else if(get210(currentOpcode) == 0b111)
					{
						effectiveAddress = getEffectiveAddress();
						byte M = RAM[effectiveAddress];
						/* ADM - XXX = 000. Add M and accumulator and put in accumulator */
						if(get543(currentOpcode) == 0b000)
						{
							RegisterFile.FCarry = ((RegisterFile.Register[A] & 0xFF) + (M & 0xFF)) > 0xFF ? true : false;
							
							RegisterFile.Register[A] += M;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ACM - XXX = 001. Add M + carry and accumulator and put in accumulator */
						else if(get543(currentOpcode) == 0b001)
						{
							byte tmp = RegisterFile.FCarry ? (byte)1 : 0;
							RegisterFile.FCarry = ((RegisterFile.Register[A] & 0xFF) + (M & 0xFF) + tmp) > 0xFF ? true : false;
							
							RegisterFile.Register[A] += (M + tmp);
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SUM - XXX = 010. Subtract M from accumulator and put in accumulator */
						else if(get543(currentOpcode) == 0b010)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~M + 1));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~M & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((M & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
							
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* SBM - XXX = 011. Subtract M with borrow from accumulator and put in accumulator */
						else if(get543(currentOpcode) == 0b011)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~M + 1) - (RegisterFile.FCarry ? 1 : 0));
							RegisterFile.FCarry =	((((RegisterFile.Register[A] & 0b10000000) >>> 7) & ((~M & 0b10000000) >>> 7) & ((~tmp & 0b10000000) >>> 7)) |
										(((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((M & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)))
										== 0b1 ? true : false;
									
							RegisterFile.Register[A] = tmp;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* NDM - XXX = 100. Logical AND Accumulator and M and put in accumulator */
						else if(get543(currentOpcode) == 0b100)
						{
							/* Logical operations set carry to 0 */
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] &= M;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* XRM - XXX = 101. Logical XOR Accumulator and M and put in accumulator */
						else if(get543(currentOpcode) == 0b101)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] ^= M;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* ORM - XXX = 110. Logical OR Accumulator and M and put in accumulator */
						else if(get543(currentOpcode) == 0b110)
						{
							RegisterFile.FCarry = false;
							RegisterFile.Register[A] |= M;
							RegisterFile.FParity = parityOf(RegisterFile.Register[A]);
							RegisterFile.FZero = zeroOf(RegisterFile.Register[A]);
							RegisterFile.FSign = signOf(RegisterFile.Register[A]);
						}
						/* CPM - XXX = 111. Compare Accumulator and M and set flags accordingly */
						else if(get543(currentOpcode) == 0b111)
						{
							byte tmp = (byte)(RegisterFile.Register[A] + (~M+1));
							RegisterFile.FParity = parityOf(tmp);
							RegisterFile.FSign = signOf(tmp);
							/* Equality is indicated by the zero flag */
							RegisterFile.FZero = zeroOf(tmp);
							/* Less than/greater than is indicated by the carry flag */
							RegisterFile.FCarry =	((((~RegisterFile.Register[A] & 0b10000000) >>> 7) & ((M & 0b10000000) >>> 7)) |
										(((M & 0b10000000) >>> 7) & ((tmp & 0b10000000) >>> 7)) |
										(((tmp & 0b10000000) >>> 7) & ((~RegisterFile.Register[A] & 0b10000000) >>> 7)))
										== 0b01 ? true : false;
						}	
					}
					break;
				
				
				/* Program counter and RegisterFile.Stack control instructions */
				case 0b01:
					/* FNC - 0b01001111, <B2> - System call with code B2 */
					if(currentOpcode == 0b01001111)
					{
						byte code = RAM[RegisterFile.PC++];
						systemCall(code);
						break;
					}
				
					/* The following are used for jmp instructions */
					byte B2 = RAM[RegisterFile.PC++];
					byte B3 = RAM[RegisterFile.PC++];
					char target = (char)((((B3 & 0xFF) << 8) | (B2 & 0xFF)) & RegisterFile.HLMask);
					/* JMP - 01 XXX 100, <B2>, <B3>. Jump unconditionally to <B3><B2>. XXX is a don't care */
					if(get210(currentOpcode) == 0b100)
					{
						RegisterFile.PC = target;
					}
					/* Conditional jump */
					else if(get210(currentOpcode) == 0b000)
					{
						/* C4C3: 00:Carry	01:Zero		10:Sign		11:Parity */
						/* JFc - 01 0C4C3 000, <B2>, <B3>. Jump if condition is false */
						if(((currentOpcode & 0b00100000) >>> 5) == 0)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									/* If condition is false, set RegisterFile.PC to target. Otherwise remain the same */
									RegisterFile.PC = !RegisterFile.FCarry ? target : RegisterFile.PC;
									break;
								case 0b01:
									RegisterFile.PC = !RegisterFile.FZero ? target : RegisterFile.PC;
									break;
								case 0b10:
									RegisterFile.PC = !RegisterFile.FSign ? target : RegisterFile.PC;
									break;
								case 0b11:
									RegisterFile.PC = !RegisterFile.FParity ? target : RegisterFile.PC;
									break;
								default:
									break;
							}
						}
						/* JTc - 01 0C4C3 000, <B2>, <B3>. Jump if condition is false */
						else if(((currentOpcode & 0b00100000) >>> 5) == 1)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									/* If condition is false, set RegisterFile.PC to target. Otherwise remain the same */
									RegisterFile.PC = RegisterFile.FCarry ? target : RegisterFile.PC;
									break;
								case 0b01:
									RegisterFile.PC = RegisterFile.FZero ? target : RegisterFile.PC;
									break;
								case 0b10:
									RegisterFile.PC = RegisterFile.FSign ? target : RegisterFile.PC;
									break;
								case 0b11:
									RegisterFile.PC = RegisterFile.FParity ? target : RegisterFile.PC;
									break;
								default:
									break;
							}
						}
					}
					/* CAL - 01 XXX 110, <B2>, <B3> - Call subroutine unconditionally */
					else if(get210(currentOpcode) == 0b110)
					{
						/* With a call instruction, each RegisterFile.Stack level is pushed down. The RegisterFile.PC is the first RegisterFile.Stack level. */
						/* There are 7 levels, so any call after that destroys the RegisterFile.Stack */
						for(int i = 6; i > 0; i--)
							RegisterFile.Stack[i] = RegisterFile.Stack[i-1];	/* Level 2 goes to 3, 3 to 4, etc */
						/* Then push the program counter onto the first level, and set RegisterFile.PC as target */
						RegisterFile.Stack[0] = RegisterFile.PC;
						RegisterFile.PC = target;
					}
					/* Conditional calls */
					else if(get210(currentOpcode) == 0b010)
					{
						boolean willCall = false;
						/* C4C3: 00:Carry	01:Zero		10:Sign		11:Parity */
						/* CFc - 01 0C4C3 010, <B2>, <B3> - Call subroutine if condition is false */
						if(((currentOpcode & 0b00100000) >>> 5) == 0)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									willCall = !RegisterFile.FCarry ? true : false;
									break;
								case 0b01:
									willCall = !RegisterFile.FZero ? true : false;
									break;
								case 0b10:
									willCall = !RegisterFile.FSign ? true : false;
									break;
								case 0b11:
									willCall = !RegisterFile.FParity ? true : false;
									break;
								default:
									break;
							}
							if(willCall)
							{
								for(int i = 6; i > 0; i--)
									RegisterFile.Stack[i] = RegisterFile.Stack[i-1];
								RegisterFile.Stack[0] = RegisterFile.PC;
								RegisterFile.PC = target;
							}
						}
						/* CTc - 01 0C4C3 010, <B2>, <B3> - Call subroutine if condition is false */
						else if(((currentOpcode & 0b00100000) >>> 5) == 1)
						{
							switch(((currentOpcode) & 0b00011000) >> 3)
							{
								case 0b00:
									willCall = RegisterFile.FCarry ? true : false;
									break;
								case 0b01:
									willCall = RegisterFile.FZero ? true : false;
									break;
								case 0b10:
									willCall = RegisterFile.FSign ? true : false;
									break;
								case 0b11:
									willCall = RegisterFile.FParity ? true : false;
									break;
								default:
									break;
							}
							if(willCall)
							{
								for(int i = 6; i > 0; i--)
									RegisterFile.Stack[i] = RegisterFile.Stack[i-1];
								RegisterFile.Stack[0] = RegisterFile.PC;
								RegisterFile.PC = target;
							}
						}
					}
					
					break;
				
				default:
					isHalted = true;
			}
		}
	}
}