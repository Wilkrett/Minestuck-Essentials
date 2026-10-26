package org.wilkretawesomesauce.minestuck_essentials.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

import java.util.Optional;

public record ItemizedMultiblock(ResourceLocation structure, Optional<Component> name, Direction facing, Optional<Vec3i> size, Optional<ResourceLocation> icon)
{
	public static final Codec<Direction> HORIZONTAL_DIRECTION = Direction.CODEC.validate(direction -> direction.getAxis().isHorizontal()
			? DataResult.success(direction) : DataResult.error(() -> "Facing must be horizontal: " + direction));
	public static final StreamCodec<ByteBuf, Vec3i> SIZE_STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Vec3i::getX,
			ByteBufCodecs.VAR_INT, Vec3i::getY,
			ByteBufCodecs.VAR_INT, Vec3i::getZ,
			Vec3i::new);

	public static final Codec<ItemizedMultiblock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("structure").forGetter(ItemizedMultiblock::structure),
			ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(ItemizedMultiblock::name),
			HORIZONTAL_DIRECTION.optionalFieldOf("facing", Direction.NORTH).forGetter(ItemizedMultiblock::facing),
			Vec3i.CODEC.optionalFieldOf("size").forGetter(ItemizedMultiblock::size),
			ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ItemizedMultiblock::icon)
	).apply(instance, ItemizedMultiblock::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemizedMultiblock> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, ItemizedMultiblock::structure,
			ComponentSerialization.OPTIONAL_STREAM_CODEC, ItemizedMultiblock::name,
			Direction.STREAM_CODEC, ItemizedMultiblock::facing,
			ByteBufCodecs.optional(SIZE_STREAM_CODEC), ItemizedMultiblock::size,
			ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ItemizedMultiblock::icon,
			ItemizedMultiblock::new);

	public Rotation rotationTowards(Direction placerFacing)
	{
		return Rotation.values()[Math.floorMod(placerFacing.get2DDataValue() - facing.get2DDataValue(), 4)];
	}
}
