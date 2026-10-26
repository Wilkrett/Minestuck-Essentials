package org.wilkretawesomesauce.minestuck_essentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.wilkretawesomesauce.minestuck_essentials.command.DeployListCommand;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizerSelection;

public final class ItemizerPrompt
{
	private ItemizerPrompt()
	{
	}

	public static void open(ItemizerSelection selection)
	{
		String command = "/" + DeployListCommand.registerCommand(selection.start(), selection.end().orElse(selection.start())) + " ";
		Minecraft.getInstance().setScreen(new PromptScreen(command, command.indexOf("\"\"") + 1));
	}

	private static class PromptScreen extends ChatScreen
	{
		private final int nameCursor;

		PromptScreen(String command, int nameCursor)
		{
			super(command);
			this.nameCursor = nameCursor;
		}

		@Override
		protected void init()
		{
			super.init();
			input.setCursorPosition(nameCursor);
			input.setHighlightPos(nameCursor);
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers)
		{
			int cursor = input.getCursorPosition();
			boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
			if(input.getCursorPosition() != cursor && input.getCursorPosition() == input.getValue().length())
				input.setValue(input.getValue());
			return handled;
		}
	}
}
