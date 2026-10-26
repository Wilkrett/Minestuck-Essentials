package org.wilkretawesomesauce.minestuck_essentials.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;

public record ItemizerSelection(ResourceKey<Level> dimension, BlockPos start, Optional<BlockPos> end)
{
	public static final Codec<ItemizerSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(ItemizerSelection::dimension),
			BlockPos.CODEC.fieldOf("start").forGetter(ItemizerSelection::start),
			BlockPos.CODEC.optionalFieldOf("end").forGetter(ItemizerSelection::end)
	).apply(instance, ItemizerSelection::new));
	public static final StreamCodec<ByteBuf, ItemizerSelection> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), ItemizerSelection::dimension,
			BlockPos.STREAM_CODEC, ItemizerSelection::start,
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ItemizerSelection::end,
			ItemizerSelection::new);

	public static ItemizerSelection startingAt(ResourceKey<Level> dimension, BlockPos start)
	{
		return new ItemizerSelection(dimension, start, Optional.empty());
	}

	public ItemizerSelection withEnd(BlockPos end)
	{
		return new ItemizerSelection(dimension, start, Optional.of(end));
	}

	public boolean isComplete()
	{
		return end.isPresent();
	}

	public BoundingBox area(BlockPos end)
	{
		return BoundingBox.fromCorners(start, end);
	}
}
