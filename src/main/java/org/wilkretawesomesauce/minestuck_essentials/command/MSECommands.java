package org.wilkretawesomesauce.minestuck_essentials.command;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;

@EventBusSubscriber(modid = Minestuck_essentials.MODID)
public final class MSECommands
{
	public static final String ROOT = "minessentials";

	private MSECommands()
	{
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event)
	{
		event.getDispatcher().register(Commands.literal(ROOT)
				.then(DeployListCommand.getArgumentBuilder()));
	}
}
