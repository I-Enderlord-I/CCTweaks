package org.squiddev.cctweaks.core.asm;

import com.google.common.eventbus.EventBus;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.Config;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions({"org.squiddev.cctweaks.core.asm.", "org.squiddev.cctweaks.core.Config", "org.squiddev.patcher"})
@IFMLLoadingPlugin.MCVersion("${mc_version}")
public class TweaksLoadingPlugin implements IFMLLoadingPlugin {
	public static File minecraftDir;

	public TweaksLoadingPlugin() {
		if (minecraftDir == null) {
			minecraftDir = (File) FMLInjectionData.data()[6];
			Config.init(new File(new File(minecraftDir, "config"), CCTweaks.ID + ".cfg"));
		}
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[]{ASMTransformer.class.getName()};
	}

	@Override
	public String getModContainerClass() {
		return CCTweaksDummyMod.class.getName();
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> stringObjectMap) {
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	public static class CCTweaksDummyMod extends DummyModContainer {
		public CCTweaksDummyMod() {
			super(new ModMetadata());
			ModMetadata md = getMetadata();
			md.name = md.modId = "<CCTweaks ASM>";
			md.authorList = Arrays.asList("SquidDev", "ElvishJerricco");
			md.description = "CCTweaks ASM Transformer. Refer to the main CCTweaks mod for info.";
			md.version = "${mod_version}";
		}

		@Override
		public boolean registerBus(EventBus bus, LoadController controller) {
			bus.register(this);
			return true;
		}
	}
}
