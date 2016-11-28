/*	RegFile.java
	Register File for Simulated CPU Core
	
	Written by Patrick Cland
	CE2336 Term Project Fall 2016	*/
	
public class RegFile
{
	/* There are 7 8 bit registers, with the first being the primary accumulator */
	public byte[] Register = new byte[7];
	
	/* Program counter is 16 bits */
	public char PC;
	/* There is a 8 level push down call stack. Technically the PC is the first level */
	public char[] Stack = new char[7];
	
	
	/* There are 4 condition flip flops internally. */
	/* Representing them by a boolean is the most loyal */
	public boolean FCarry;
	public boolean FParity;
	public boolean FZero;
	public boolean FSign;
	
	/* Only the lower 14 bits of HL count as an effective address */
	public final char HLMask = 0b0011111111111111;
}