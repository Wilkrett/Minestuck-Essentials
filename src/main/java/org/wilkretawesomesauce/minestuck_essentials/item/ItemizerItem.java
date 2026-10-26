package org.wilkretawesomesauce.minestuck_essentials.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.wilkretawesomesauce.minestuck_essentials.client.ItemizerPrompt;

import java.util.List;

public class ItemizerItem extends Item
{
	private static final int START_COOLDOWN = 10;

	public ItemizerItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
	{
		Player player = context.getPlayer();
		if(player == null)
			return InteractionResult.PASS;
		if(player.getCooldowns().isOnCooldown(this))
			return InteractionResult.FAIL;

		Level level = context.getLevel();
		if(player.isSecondaryUseActive())
		{
			clearSelection(level, player, stack);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		BlockPos pos = context.getClickedPos();
		ItemizerSelection selection = stack.get(MSEItemComponents.ITEMIZER_SELECTION.get());
		if(selection == null || selection.isComplete() || selection.dimension() != level.dimension())
		{
			player.getCooldowns().addCooldown(this, START_COOLDOWN);
			if(!level.isClientSide)
			{
				stack.set(MSEItemComponents.ITEMIZER_SELECTION.get(), ItemizerSelection.startingAt(level.dimension(), pos));
				player.displayClientMessage(Component.translatable(getDescriptionId() + ".start", formatPos(pos)), true);
			}
		} else
		{
			ItemizerSelection complete = selection.withEnd(pos);
			if(level.isClientSide)
				ItemizerPrompt.open(complete);
			else
			{
				stack.set(MSEItemComponents.ITEMIZER_SELECTION.get(), complete);
				player.displayClientMessage(Component.translatable(getDescriptionId() + ".end", formatPos(pos), formatSize(complete.area(pos))), true);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(player.isSecondaryUseActive())
		{
			clearSelection(level, player, stack);
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}

		ItemizerSelection selection = stack.get(MSEItemComponents.ITEMIZER_SELECTION.get());
		if(selection != null && selection.isComplete() && selection.dimension() == level.dimension())
		{
			if(level.isClientSide)
				ItemizerPrompt.open(selection);
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		return InteractionResultHolder.pass(stack);
	}

	private void clearSelection(Level level, Player player, ItemStack stack)
	{
		if(!level.isClientSide && stack.remove(MSEItemComponents.ITEMIZER_SELECTION.get()) != null)
			player.displayClientMessage(Component.translatable(getDescriptionId() + ".cleared"), true);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		ItemizerSelection selection = stack.get(MSEItemComponents.ITEMIZER_SELECTION.get());
		if(selection != null)
		{
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.start", formatPos(selection.start())).withStyle(ChatFormatting.GRAY));
			selection.end().ifPresent(end -> tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.end",
					formatPos(end), formatSize(selection.area(end))).withStyle(ChatFormatting.GRAY)));
		}
		tooltip.add(Component.translatable(getDescriptionId() + ".tooltip.usage").withStyle(ChatFormatting.DARK_GRAY));
	}

	private static String formatPos(BlockPos pos)
	{
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static String formatSize(BoundingBox area)
	{
		return area.getXSpan() + "x" + area.getYSpan() + "x" + area.getZSpan();
	}
}
