package org.wilkretawesomesauce.minestuck_essentials.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;

@EventBusSubscriber(modid = Minestuck_essentials.MODID)
public final class MSENetwork
{
	private MSENetwork()
	{
	}

	@SubscribeEvent
	public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
	{
		event.registrar("1").playToClient(MultiblockPreviewsPayload.TYPE, MultiblockPreviewsPayload.STREAM_CODEC, MultiblockPreviewsPayload::handle);
	}
}
