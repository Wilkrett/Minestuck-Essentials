package org.wilkretawesomesauce.minestuck_essentials.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.wilkretawesomesauce.minestuck_essentials.deploy.MultiblockPreview;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblock;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItemComponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MultiblockPreviews
{
	private static Map<ResourceLocation, MultiblockPreview> previews = Map.of();
	private static final Map<Key, MultiblockPreview> ROTATED = new HashMap<>();

	private record Key(ResourceLocation structure, Rotation rotation)
	{
	}

	private MultiblockPreviews()
	{
	}

	public static void set(Map<ResourceLocation, MultiblockPreview> newPreviews)
	{
		previews = Map.copyOf(newPreviews);
		ROTATED.clear();
	}

	public static Optional<MultiblockPreview> get(ItemStack stack)
	{
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		MultiblockPreview preview = itemized == null ? null : previews.get(itemized.structure());
		if(preview == null)
			return Optional.empty();

		Rotation rotation = itemized.rotationTowards(Direction.SOUTH);
		return Optional.of(ROTATED.computeIfAbsent(new Key(itemized.structure(), rotation), key -> rotate(preview, rotation)));
	}

	private static MultiblockPreview rotate(MultiblockPreview preview, Rotation rotation)
	{
		Vec3i size = preview.size();
		BoundingBox box = BoundingBox.fromCorners(StructureTemplate.transform(BlockPos.ZERO, Mirror.NONE, rotation, BlockPos.ZERO),
				StructureTemplate.transform(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1), Mirror.NONE, rotation, BlockPos.ZERO));
		List<MultiblockPreview.PreviewBlock> blocks = preview.blocks().stream()
				.map(block -> new MultiblockPreview.PreviewBlock(
						StructureTemplate.transform(block.pos(), Mirror.NONE, rotation, BlockPos.ZERO).offset(-box.minX(), -box.minY(), -box.minZ()),
						block.state().rotate(rotation)))
				.toList();
		return new MultiblockPreview(new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()), blocks);
	}
}
