package org.wilkretawesomesauce.minestuck_essentials;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Minestuck_essentials.MODID)
public class Minestuck_essentials {
    public static final String MODID = "minestuck_essentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Minestuck_essentials(IEventBus modEventBus, ModContainer modContainer) {
    }
}
