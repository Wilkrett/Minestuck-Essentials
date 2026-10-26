package org.wilkretawesomesauce.minestuck_essentials.deploy;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record MultiblockPreview(Vec3i size, List<PreviewBlock> blocks)
{
	public static final int MAX_BLOCKS = 4096;
	public static final StreamCodec<ByteBuf, MultiblockPreview> STREAM_CODEC = StreamCodec.composite(
			ItemizedMultiblock.SIZE_STREAM_CODEC, MultiblockPreview::size,
			PreviewBlock.STREAM_CODEC.apply(ByteBufCodecs.list()), MultiblockPreview::blocks,
			MultiblockPreview::new);

	public record PreviewBlock(BlockPos pos, BlockState state)
	{
		public static final StreamCodec<ByteBuf, PreviewBlock> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, PreviewBlock::pos,
				ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), PreviewBlock::state,
				PreviewBlock::new);
	}

	public static Optional<Vec3i> templateSize(CompoundTag template)
	{
		ListTag size = template.getList(StructureTemplate.SIZE_TAG, Tag.TAG_INT);
		return size.size() == 3 ? Optional.of(new Vec3i(size.getInt(0), size.getInt(1), size.getInt(2))) : Optional.empty();
	}

	public static Optional<MultiblockPreview> fromTemplate(CompoundTag template)
	{
		Optional<Vec3i> size = templateSize(template);
		if(size.isEmpty())
			return Optional.empty();

		ListTag paletteTag = template.contains(StructureTemplate.PALETTE_LIST_TAG, Tag.TAG_LIST)
				? template.getList(StructureTemplate.PALETTE_LIST_TAG, Tag.TAG_LIST).getList(0)
				: template.getList(StructureTemplate.PALETTE_TAG, Tag.TAG_COMPOUND);
		HolderGetter<Block> blockLookup = BuiltInRegistries.BLOCK.asLookup();
		List<BlockState> palette = paletteTag.stream().map(tag -> NbtUtils.readBlockState(blockLookup, (CompoundTag) tag)).toList();

		Map<BlockPos, BlockState> states = new HashMap<>();
		for(Tag blockTag : template.getList(StructureTemplate.BLOCKS_TAG, Tag.TAG_COMPOUND))
		{
			CompoundTag block = (CompoundTag) blockTag;
			ListTag pos = block.getList(StructureTemplate.BLOCK_TAG_POS, Tag.TAG_INT);
			int state = block.getInt(StructureTemplate.BLOCK_TAG_STATE);
			if(pos.size() == 3 && state >= 0 && state < palette.size() && !palette.get(state).isAir())
				states.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), palette.get(state));
		}

		List<PreviewBlock> visible = states.entrySet().stream()
				.filter(entry -> Arrays.stream(Direction.values()).anyMatch(side -> !isSolid(states, entry.getKey().relative(side))))
				.map(entry -> new PreviewBlock(entry.getKey(), entry.getValue()))
				.toList();
		return visible.isEmpty() || visible.size() > MAX_BLOCKS ? Optional.empty() : Optional.of(new MultiblockPreview(size.get(), visible));
	}

	private static boolean isSolid(Map<BlockPos, BlockState> states, BlockPos pos)
	{
		BlockState state = states.get(pos);
		return state != null && state.isSolidRender(EmptyBlockGetter.INSTANCE, pos);
	}
}
