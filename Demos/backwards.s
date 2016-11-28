; backwards.s
; Demo program to demonstrate Sim8008 Simulator & Assembler
; Receives a string from the user and prints the reverse

			org $0
MAIN			pam PROMPT1		Print prompt to ask user for input
			fnc #2
			pam PROMPT2
			fnc #2
			pam STRING		Receive null terminated string from the keyboard
			fnc #4
			
GETLENGTH		lci #0			Set C register as counter, initialize to 0
			xra			Set Accumulator to zero to compare against null terminator
			
LENGTHLOOP		cpm			Compare Accumulator (0) to byte in the string
			jtz LENGTHDONE	If null terminator is found, we are done calculating the length
			
			inc			If not null terminator yet, then increment the counter
			inl			And increment to the next character in the string
			jmp LENGTHLOOP
			
LENGTHDONE		pam PROMPT3		Print the header for the reversed string
			fnc #2

			pam STRING		Reload HL with the address of the string
			lal			Load accumulator with L
			adc			Add length of string to accumulator to get offset of last character in string
			lla			Load L back with the new offset
			
			lac			Load accumulator with the length of the string
			adi #1			Add 1 to accumulator to correct length when printing backwards
			
PRINTLOOP		fnc #6			Print character at HL
			dcl			Decrement string offset
			sui #1			Decrement length counter
			jtz DONE		If the length counter is 0, we are done printing
			jmp PRINTLOOP
			
DONE			hlt
			

			org $50
; Data declarations
PROMPT1			ascii	Welcome to the Sim8008 demo program!
			db $0D
			db $0A
			db $0
PROMPT2			ascii	Enter string to reverse:
			db $20
			db $0
PROMPT3			ascii	The reversed string is:
			db $0D
			db $0A
			db $0
			
			org $B0
; Variable Declarations
STRING			db $0		Memory location for the string we will enter
