package com.cavetale.kingofthering;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public final class Platform {
    protected List<Block> blocks = new ArrayList<>();
    protected List<BlockData> blockData = new ArrayList<>();
    protected int ticks = 0;

    protected void tick() {
        ticks += 1;
        if (ticks == 1) {
            setAll(Material.BLACK_STAINED_GLASS.createBlockData());
            Location loc = blocks.get(blocks.size() / 2).getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.MASTER, 1.0f, 2.0f);
        } else if (ticks == 20) {
            setAll(Material.GRAY_STAINED_GLASS.createBlockData());
        } else if (ticks == 40) {
            setAll(Material.LIGHT_GRAY_STAINED_GLASS.createBlockData());
        } else if (ticks == 60) {
            setAll(Material.WHITE_STAINED_GLASS.createBlockData());
        } else if (ticks == 80) {
            setAll(Material.BARRIER.createBlockData());
            Location loc = blocks.get(blocks.size() / 2).getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 0.5f, 1.0f);
        } else if (ticks == 120) {
            setAll(Material.AIR.createBlockData());
        }
    }

    protected void setAll(BlockData data) {
        for (int i = 0; i < blocks.size(); i += 1) {
            blocks.get(i).setBlockData(data);
        }
    }

    protected void resetAll() {
        for (int i = 0; i < blocks.size(); i += 1) {
            blocks.get(i).setBlockData(blockData.get(i), false);
        }
    }
}
