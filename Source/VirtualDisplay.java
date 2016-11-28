/*	VirtualDisplay.java
	Virtual memory mapped bitmap display for simulated 8008.
	
	Written by Patrick Cland
	CE2336 Term Project Fall 2016	*/

import java.awt.*;

public class VirtualDisplay extends Frame
{
	private byte[] pixelData = new byte[0x1000];
	
	public void updatePixelData(byte[] RAM)
	{
		for(int i = 0; i < 0xFFF; i++)
		{
			pixelData[i] = RAM[0x3000 + i];
		}
	}
	
	public void initializeDisplay()
	{
		/* Pixels are 8x8, with resolution of 64x64 */
		setSize(512,512);
		setVisible(true); //9 from top, 2 from side 61 from bottom
	}
	
	@Override
	public void paint(Graphics g)
	{
		g.setColor(Color.black);
		for(int i = 0; i < 64; i++)
		{
			for(int j = 0; j < 64; j++)
			{
				if(pixelData[i*64 + j] != 0)
				{
					g.fillRect((j * 8),(i * 8), 8, 8);
				}
			}
		}
	}
}