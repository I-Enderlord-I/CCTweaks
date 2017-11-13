package org.squiddev.cctweaks.client.gui;

import dan200.computercraft.client.gui.GuiComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.IComputer;
import org.squiddev.cctweaks.command.ContainerAnyComputer;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;

@SideOnly(Side.CLIENT)
public class GuiAnyComputer extends GuiComputer {
	public GuiAnyComputer(IComputer computer, ComputerFamily family) {
		super(new ContainerAnyComputer(computer), family, computer, computer.getTerminal().getWidth(),
				computer.getTerminal().getHeight());
	}
}