package org.squiddev.cctweaks.command;

import com.google.common.collect.Lists;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.squiddev.cctweaks.GuiHandler;
import org.squiddev.cctweaks.core.command.*;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import static org.squiddev.cctweaks.core.command.ChatHelpers.*;

public final class CommandCCTweaks {

	private CommandCCTweaks() {
	}

	public static ICommand create(MinecraftServer server) {
		CommandRoot root = new CommandRoot(
			"cctweaks", "Various commands for CCTweaks.",
			"The CCTweaks command provides various debugging and administrator tools for controlling and interacting " +
				"with computers."
		);

		root.register(new SubCommandBase(
			"shutdown", "[ids...]", "Shutdown computers remotely.",
			"Shutdown the listed computers or all if none are specified. You can either specify the computer's instance " +
				"id (e.g. 123) or computer id (e.g #123)."
		) {
			@Override
			public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull CommandContext context, @Nonnull List<String> arguments) throws CommandException {
				List<ServerComputer> computers = Lists.newArrayList();
				if (arguments.size() > 0) {
					for (String arg : arguments) {
						computers.add(ComputerSelector.getComputer(arg));
					}
				} else {
					computers.addAll(ComputerCraft.serverComputerRegistry.getComputers());
				}

				int shutdown = 0;
				for (ServerComputer computer : computers) {
					if (computer.isOn()) shutdown++;
					computer.unload();
				}
				sender.addChatMessage(text("Shutdown " + shutdown + " / " + computers.size()));
			}

			@Nonnull
			@Override
			public List<String> getCompletion(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull List<String> arguments) {
				return arguments.size() == 0
					? Collections.<String>emptyList()
					: ComputerSelector.completeComputer(arguments.get(arguments.size() - 1));
			}
		});

		root.register(new SubCommandBase(
			"tp", "<id>", "Teleport to a specific computer.",
			"Teleport to the location of a computer. You can either specify the computer's instance " +
				"id (e.g. 123) or computer id (e.g #123)."
		) {
			@Override
			public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull CommandContext context, @Nonnull List<String> arguments) throws CommandException {
				if (arguments.size() != 1) throw new CommandException(context.getFullUsage());

				ServerComputer computer = ComputerSelector.getComputer(arguments.get(0));
				World world = computer.getWorld();
				ChunkCoordinates pos = computer.getPosition();

				if (world == null || pos == null) throw new CommandException("Cannot locate computer in world");

				if (!(sender instanceof Entity)) throw new CommandException("Sender is not an entity");

				if (sender instanceof EntityPlayerMP) {
					EntityPlayerMP entity = (EntityPlayerMP) sender;
					if (entity.getEntityWorld() != world) {
						server.getConfigurationManager().transferPlayerToDimension(entity, world.provider.dimensionId);
					}

					entity.setPositionAndUpdate(pos.posX + 0.5, pos.posY, pos.posZ + 0.5);
				} else {
					Entity entity = (Entity) sender;
					if (entity.worldObj != world) {
						entity.travelToDimension(world.provider.dimensionId);
					}

					entity.setLocationAndAngles(
						pos.posX + 0.5, pos.posY, pos.posZ + 0.5,
						entity.rotationYaw, entity.rotationPitch
					);
				}
			}

			@Nonnull
			@Override
			public List<String> getCompletion(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull List<String> arguments) {
				return arguments.size() == 1
					? ComputerSelector.completeComputer(arguments.get(0))
					: Collections.<String>emptyList();
			}
		});

		root.register(new SubCommandGive());

		root.register(new SubCommandBase(
			"view", "<id>", "View the terminal of a computer.",
			"Open the terminal of a computer, allowing remote control of a computer. This does not provide access to " +
				"turtle's inventories. You can either specify the computer's instance id (e.g. 123) or computer id (e.g #123)."
		) {
			@Override
			public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull CommandContext context, @Nonnull List<String> arguments) throws CommandException {
				if (arguments.size() != 1) throw new CommandException(context.getFullUsage());

				if (!(sender instanceof EntityPlayerMP)) {
					throw new CommandException("Cannot open terminal for non-player");
				}

				ServerComputer computer = ComputerSelector.getComputer(arguments.get(0));
				GuiHandler.openComputer((EntityPlayerMP) sender, computer);
			}

			@Nonnull
			@Override
			public List<String> getCompletion(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull List<String> arguments) {
				return arguments.size() == 1
					? ComputerSelector.completeComputer(arguments.get(0))
					: Collections.<String>emptyList();
			}
		});
		
		return new CommandDelegate(server, root);
	}

	private static IChatComponent linkComputer(ServerComputer computer) {
		return link(
			text(Integer.toString(computer.getInstanceID())),
			"/cctweaks dump " + computer.getInstanceID(),
			"View more info about this computer"
		);
	}

	private static IChatComponent linkPosition(ServerComputer computer) {
		return link(
			position(computer.getPosition().posX,computer.getPosition().posY,computer.getPosition().posZ),
			"/cctweaks tp " + computer.getInstanceID(),
			"Teleport to this computer"
		);
	}
}
