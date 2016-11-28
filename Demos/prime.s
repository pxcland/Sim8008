; prime.s
; Demo program to demonstrate Sim8008 Simulator & Assembler
; Program to calculate prime numbers up to n based off user input

			org $0			Main program binary begins at address 0x0000
MAIN		cal INTRO		Call the intro subroutine to prompt input
			ldi #1			Start testing primes at number 2
LOOP		ind
			cal TESTPRIME	Test if contents of D is prime and print if so
			lad				Determine is we have already tested MAX numbers
			pam MAX
			cpm
			jfz	LOOP		If we tested MAX amount of numbers, we are done
			
			hlt				

; Intro subroutine
; Display prompt and receive input
INTRO		pam PROMPT1		Display prompt
			fnc #2
			pam PROMPT2	
			fnc #2
			pam MAX			Get user input
			fnc #3
			ret
			
; Test Prime subroutine
; Tests whether the number stored in register D is a prime number.
; If it is, it prints it. If not, it does not print.
TESTPRIME	lei #2			Divide the number we are testing by 2, to minimize unnecessary divisions
			cal DIVIDE
			adi #1
			lha				Store (number/2)+1 in register H
			lei #1
			
PRIMELOOP	ine				Increment E up until H.
			lae				Move E to accumulator to test equality to H
			cph				Compare accumulator to H
			jtz	ISPRIME		If they are equal, we have done (number/2)+1 divisions, and this number is prime
			cal DIVIDE		Divide the number by E.
			lab				Move the remainder to A to test equality to zero
			cpi #0			If the accumulator is zero, then there was no remainder, this number is not prime.
			jtz NOTPRIME
			jmp PRIMELOOP	If the number may still be prime, test the next number

ISPRIME		pam PRIMENUM	Store the number in D, which is prime, into memory to print it.
			lmd
			fnc #0			Print the number that is prime.
			pam NEWLINE		Print a newline
			fnc #2
			ret

NOTPRIME	ret
			

; Divide subroutine.
; Divides contents of register D by contents of register E.
; Stores the quotient in accumulator and remainder in register B.
; Destroys register C.
DIVIDE		lci #0			Set Counter to 0
			lae				Accumulator = E
			cpi #0			If E = 0, Divide by 0 error
			jtz ERROR
			
			lad				Accumulator = D
			adi #0			Set flags for the status of the accumulator
			jfs LOOPSMALL	If sign bit is set, Accumulator is 128 or higher

LOOPBIG		sue				Subtract E from accumulator until it is below 128
			inc				Increment counter which stores quotient
			adi #0			Set Flags for accumulator
			jts LOOPBIG		If still above 128, keep doing until below 128

LOOPSMALL	sue				Subtract E from accumulator until it is < 0
			inc				Increment quotient
			adi #0			Set flags for accumulator
			jts DIVDONE		If flag is set, A < 0, so done dividing
			jmp LOOPSMALL
			
DIVDONE		ade				Add E to Accumulator to get the remainder
			lba				Save remainder in B
			dcc				Decrement quotient since it is 1 too high
			lac				Save quotient in Accumulator	
			ret				Finish. Accumulator has quotient and B has remainder

ERROR		lai #254		Magic number of decimal 254 represents
			ret				Divide by 0 error

			

; Data declarations
			org $A0		Data resides at memory address 0x00A0
; String declarations
PROMPT1		ascii	Welcome to the Sim8008 demo program!
			db #13
			db #10
			db #0

PROMPT2		ascii	List all prime numbers up to (valid range is 2-255): 
			db $20
			db #0
			

NEWLINE		db #13
			db #10
			db #0
			
; Variable declarations			
MAX			db #0
PRIMENUM	db #0


