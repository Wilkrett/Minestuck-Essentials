package org.wilkretawesomesauce.minestuck_essentials.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItems;

@EventBusSubscriber(modid = Minestuck_essentials.MODID, value = Dist.CLIENT)
public final class MSEClient
{
	public static final ResourceLocation DISPLAY = ResourceLocation.fromNamespaceAndPath(Minestuck_essentials.MODID, "display");

	private MSEClient()
	{
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		event.enqueueWork(() -> ItemProperties.register(MSEItems.ITEMIZED_MULTIBLOCK.get(), DISPLAY,
				(stack, level, entity, seed) -> MultiblockIcons.get(stack).isPresent() ? 1F : MultiblockPreviews.get(stack).isPresent() ? 0.5F : 0F));
	}

	@SubscribeEvent
	public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
	{
		event.registerItem(new IClientItemExtensions()
		{
			private ItemizedMultiblockRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer()
			{
				if(renderer == null)
					renderer = new ItemizedMultiblockRenderer();
				return renderer;
			}
		}, MSEItems.ITEMIZED_MULTIBLOCK.get());
	}

	@SubscribeEvent
	public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> MultiblockIcons.clear());
	}
}
