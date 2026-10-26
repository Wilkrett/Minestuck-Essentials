package org.wilkretawesomesauce.minestuck_essentials.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.client.MultiblockPreviews;
import org.wilkretawesomesauce.minestuck_essentials.deploy.MultiblockPreview;

import java.util.HashMap;
import java.util.Map;

public record MultiblockPreviewsPayload(Map<ResourceLocation, MultiblockPreview> previews) implements CustomPacketPayload
{
	public static final Type<MultiblockPreviewsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Minestuck_essentials.MODID, "multiblock_previews"));
	public static final StreamCodec<ByteBuf, MultiblockPreviewsPayload> STREAM_CODEC = ByteBufCodecs.<ByteBuf, ResourceLocation, MultiblockPreview, Map<ResourceLocation, MultiblockPreview>>map(
					HashMap::new, ResourceLocation.STREAM_CODEC, MultiblockPreview.STREAM_CODEC)
			.map(MultiblockPreviewsPayload::new, MultiblockPreviewsPayload::previews);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public void handle(IPayloadContext context)
	{
		MultiblockPreviews.set(previews);
	}
}
