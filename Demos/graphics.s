; graphics.s
; Demo program to demonstrate Sim8008 Simulator & Assembler
; Program to demonstrate virtual display of simulator

			org $0
MAIN		fnc #7			Initialize Virtual Display
BEGIN		pam $3800		Set HL to point to video segment
			
LOOP1		lmi #1			Set pixel
			fnc #8
			lda #35			Sleep 20 miliseconds
			fnc #9
			inl				Move to the next pixel
			lal				Move to accumulator to test whether at the end of the screen
			cpi #63
			jtz	LOOP2		If at the end of the screen, do the second loop to clear
			jmp LOOP1
			
LOOP2		lmi #0			Unset Pixel
			fnc #8
			lda #35			Sleep 20 miliseconds
			fnc #9
			dcl				Move to the previous pixel
			lal				Move to accumulator to test whether at the beginning of screen
			cpi #0
			jtz LOOP1		If at the beginning, start the program over again
			jmp LOOP2
