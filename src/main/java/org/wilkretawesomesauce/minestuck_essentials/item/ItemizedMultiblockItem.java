package org.wilkretawesomesauce.minestuck_essentials.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemizedMultiblockItem extends BlockItem
{
	public ItemizedMultiblockItem(Properties properties)
	{
		super(Blocks.STRUCTURE_BLOCK, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context)
	{
		ItemStack stack = context.getItemInHand();
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		if(itemized == null)
			return InteractionResult.FAIL;
		if(!(context.getLevel() instanceof ServerLevel level))
			return InteractionResult.SUCCESS;

		Player player = context.getPlayer();
		Optional<StructureTemplate> template = level.getStructureManager().get(itemized.structure());
		if(template.isEmpty())
		{
			if(player != null)
				player.displayClientMessage(Component.translatable(getDescriptionId() + ".missing", itemized.structure().toString()), true);
			return InteractionResult.FAIL;
		}

		Direction placerFacing = context.getHorizontalDirection();
		Rotation rotation = itemized.rotationTowards(placerFacing);
		Vec3i size = template.get().getSize();
		BoundingBox box = placementBox(size, rotation, context.getClickedPos(), placerFacing);
		if(!canPlaceIn(context, box))
		{
			if(player != null)
				player.displayClientMessage(Component.translatable(getDescriptionId() + ".no_room"), true);
			return InteractionResult.FAIL;
		}

		BoundingBox relative = relativeBox(size, rotation);
		BlockPos origin = new BlockPos(box.minX() - relative.minX(), box.minY() - relative.minY(), box.minZ() - relative.minZ());
		template.get().placeInWorld(level, origin, origin, new StructurePlaceSettings().setRotation(rotation), level.getRandom(), Block.UPDATE_ALL);
		level.playSound(null, context.getClickedPos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(player, GameEvent.BLOCK_PLACE, context.getClickedPos());
		if(player == null || !player.getAbilities().instabuild)
			stack.shrink(1);

		return InteractionResult.SUCCESS;
	}

	public static BoundingBox placementBox(Vec3i size, Rotation rotation, BlockPos target, Direction placerFacing)
	{
		BoundingBox relative = relativeBox(size, rotation);
		int minX = switch(placerFacing)
		{
			case EAST -> target.getX();
			case WEST -> target.getX() - relative.getXSpan() + 1;
			default -> target.getX() - (relative.getXSpan() - 1) / 2;
		};
		int minZ = switch(placerFacing)
		{
			case SOUTH -> target.getZ();
			case NORTH -> target.getZ() - relative.getZSpan() + 1;
			default -> target.getZ() - (relative.getZSpan() - 1) / 2;
		};
		return relative.moved(minX - relative.minX(), target.getY() - relative.minY(), minZ - relative.minZ());
	}

	private static BoundingBox relativeBox(Vec3i size, Rotation rotation)
	{
		return BoundingBox.fromCorners(StructureTemplate.transform(BlockPos.ZERO, Mirror.NONE, rotation, BlockPos.ZERO),
				StructureTemplate.transform(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1), Mirror.NONE, rotation, BlockPos.ZERO));
	}

	public static boolean canPlaceIn(BlockPlaceContext context, BoundingBox box)
	{
		Level level = context.getLevel();
		Player player = context.getPlayer();
		for(BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
		{
			if(level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced(context))
				return false;
			if(player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand())))
				return false;
		}
		return true;
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item)
	{
	}

	@Override
	public String getDescriptionId()
	{
		return getOrCreateDescriptionId();
	}

	@Override
	public Component getName(ItemStack stack)
	{
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		return itemized != null && itemized.name().isPresent() ? itemized.name().get() : super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		if(itemized != null)
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip", itemized.structure().toString()).withStyle(ChatFormatting.GRAY));
	}
}
