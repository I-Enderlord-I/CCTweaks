package org.squiddev.cctweaks.lua.asm;

import org.squiddev.cctweaks.lua.asm.binary.BinaryUtils;


public class NewTweaks
{
  public static void setup(CustomChain chain)
  {
    chain.addPatchFile("org.luaj.vm2.lib.DebugLib");
    chain.addPatchFile("org.luaj.vm2.lib.StringLib");
    
    chain.add(new AddAdditionalData());
    chain.add(new CustomAPIs());
    chain.add(new CustomBios());
    chain.add(new CustomTimeout());
    chain.add(new InjectLuaJC());
    chain.add(new WhitelistDebug());
    BinaryUtils.inject(chain);
  }
}

/* Location:           C:\Users\Edlinger\.gradle\caches\modules-2\files-2.1\org.squiddev\cctweaks-lua\1.0.13\8fee6a054de75c965092fa9bdbd74e0075014bc4\cctweaks-lua-1.0.13.jar
 * Qualified Name:     org.squiddev.cctweaks.lua.asm.Tweaks
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.7.1
 */