package org.squiddev.cctweaks.integration.multipart;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.multipart.PartSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.network.INetworkNodeProvider;
import org.squiddev.cctweaks.api.network.IWorldNetworkNode;
import org.squiddev.cctweaks.api.peripheral.IPeripheralHost;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.registry.IClientModule;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.utils.Helpers;
import org.squiddev.cctweaks.integration.ModIntegration;
import org.squiddev.cctweaks.integration.multipart.network.PartCable;
import org.squiddev.cctweaks.integration.multipart.network.PartModem;
import org.squiddev.cctweaks.integration.multipart.network.PartWirelessBridge;

public class MultipartIntegration extends ModIntegration implements IClientModule {
	public static final String MOD_NAME = "mcmultipart";

	private final ItemCustomPart itemPart = new ItemCustomPart();

	public MultipartIntegration() {
		super(MOD_NAME);
	}

	@Override
	public boolean canLoad() {
		return super.canLoad() && Config.Integration.mcMultipart;
	}

	@Override
	public void preInit() {
		GameRegistry.registerItem(itemPart, "itemPart");
		Helpers.twoWayCrafting(new ItemStack(Registry.blockNetworked, 1, 0), new ItemStack(itemPart, 1, 0));

		// Register parts
		MultipartRegistry.registerPart(PartCable.class, CCTweaks.NAME + ":Cable");
		MultipartRegistry.registerPart(PartModem.class, CCTweaks.NAME + ":Modem");
		MultipartRegistry.registerPart(PartWirelessBridge.class, CCTweaks.NAME + ":wirelessBridge");

		// Multipart converters
		MultipartConverter converter = new MultipartConverter();
		MultipartRegistry.registerPartConverter(converter);
		MultipartRegistry.registerReversePartConverter(converter);

		// CC providers
		CCTweaksAPI.instance().networkRegistry().addNodeProvider(new INetworkNodeProvider() {
			@Override
			public IWorldNetworkNode getNode(TileEntity tile) {
				if (tile instanceof IMultipartContainer) {
					IMultipart part = ((IMultipartContainer) tile).getPartInSlot(PartSlot.CENTER);
					return part == null ? null : MultipartHelpers.getWorldNode(part);
				}

				return null;
			}

			@Override
			public boolean isNode(TileEntity tile) {
				return getNode(tile) != null;
			}
		});
		ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider() {
			@Override
			public IPeripheral getPeripheral(World world, BlockPos position, EnumFacing enumFacing) {
				TileEntity tile = world.getTileEntity(position);
				if (tile instanceof IMultipartContainer) {
					IMultipart part = ((IMultipartContainer) tile).getPartInSlot(PartSlot.getFaceSlot(enumFacing));
					if (part instanceof IPeripheral) return (IPeripheral) part;
					if (part instanceof IPeripheralHost) return ((IPeripheralHost) part).getPeripheral(enumFacing);
				}
				return null;
			}
		});
	}

	@Override
	public void clientInit() {
		Helpers.setupModel(itemPart, 0, "wirelessBridgeSmall");
	}
}