package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.core.apis.ILuaAPI;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.core.terminal.Terminal;

import java.util.List;

import org.squiddev.cctweaks.lua.patch.iface.ComputerPatched;
import org.squiddev.patcher.visitors.MergeVisitor;

public class Computer_Patch extends Computer implements ComputerPatched {
	private IMount romMount;
	private String biosPath;

	@MergeVisitor.Stub
	private static IMount s_romMount;
	@MergeVisitor.Stub
	private final IComputerEnvironment m_environment = null;
	@MergeVisitor.Stub
	private FileSystem m_fileSystem;
	@MergeVisitor.Stub
	private ILuaMachine m_machine;
	@MergeVisitor.Stub
	private List<ILuaAPI> m_apis;
	@MergeVisitor.Stub
	private Terminal m_terminal;

	@MergeVisitor.Stub
	public Computer_Patch(IComputerEnvironment environment, Terminal terminal, int id) {
		super(environment, terminal, id);
	}

	@Override
	public void setRomMount(String biosPath, IMount mount) {
		this.biosPath = biosPath;
		this.romMount = mount;
	}

	private boolean initFileSystem() {
		assignID();

		try {
			m_fileSystem = new FileSystem("hdd", getRootMount());
			
			if (romMount == null) {
				if (s_romMount == null) s_romMount = m_environment.createResourceMount("computercraft", "lua/rom");
				romMount = s_romMount;
			}

			if (romMount != null) {
				m_fileSystem.mount("rom", "rom", romMount);
				return true;
			} else {
				return false;
			}
		} catch (FileSystemException var3) {
			var3.printStackTrace();
			return false;
		}
	}
	/*
	private void initLua()
	{
		ILuaMachine machine = new LuaJLuaMachine(this);
		
		Iterator<ILuaAPI> it = m_apis.iterator();
		while (it.hasNext())
		{
			ILuaAPI api = (ILuaAPI)it.next();
			machine.addAPI(api);
			api.startup();
		}
		
		InputStream biosStream = null;
		
		try
		{
			if (romMount.exists("bios.lua")) 
				try {
					biosStream = romMount.openForRead("bios.lua");
				} catch (Exception e) {}
			if (biosStream == null) biosStream = Computer.class.getResourceAsStream("/assets/computercraft/lua/bios.lua");
		}
		catch (Exception e) {}
		
		if (biosStream != null)
		{
			machine.loadBios(biosStream);
			try {
				biosStream.close();
			}
			catch (IOException e) {}
	    
			if (machine.isFinished())
			{
				m_terminal.reset();
				m_terminal.write("Error starting bios.lua");
				m_terminal.setCursorPos(0, 1);
				m_terminal.write("ComputerCraft may be installed incorrectly");
				
				machine.unload();
				m_machine = null;
			}
			else
			{
				m_machine = machine;
			}
		}
		else
		{
			m_terminal.reset();
			m_terminal.write("Error loading bios.lua");
			m_terminal.setCursorPos(0, 1);
			m_terminal.write("ComputerCraft may be installed incorrectly");
			
			machine.unload();
			m_machine = null;
		}
	}*/
}