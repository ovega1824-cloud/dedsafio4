package com.desafio4.item;

import com.desafio4.Desafio4;
import com.desafio4.block.ModBlocks;
import com.desafio4.server.Game;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Desafio4.MODID);

    public static final String[] ROULETTE_NAMES = {"red", "orange", "yellow", "green", "cyan", "blue", "purple", "pink"};

    private static RegistryObject<Item> action(String name, ActionItem.Use use, boolean consume, boolean foil, int cooldown, int stack, Rarity rarity) {
        return ITEMS.register(name, () -> new ActionItem(new Item.Properties().stacksTo(stack).rarity(rarity), use, consume, foil, cooldown));
    }

    private static RegistryObject<Item> roulette(String color, int idx) {
        return action("roulette_" + color, (p, h) -> {
            Game.roulette(p.server, idx);
            return true;
        }, false, false, 180, 1, Rarity.UNCOMMON);
    }

    // --- 8 ruletas de colores ---
    public static final RegistryObject<Item> ROULETTE_RED = roulette("red", 0);
    public static final RegistryObject<Item> ROULETTE_ORANGE = roulette("orange", 1);
    public static final RegistryObject<Item> ROULETTE_YELLOW = roulette("yellow", 2);
    public static final RegistryObject<Item> ROULETTE_GREEN = roulette("green", 3);
    public static final RegistryObject<Item> ROULETTE_CYAN = roulette("cyan", 4);
    public static final RegistryObject<Item> ROULETTE_BLUE = roulette("blue", 5);
    public static final RegistryObject<Item> ROULETTE_PURPLE = roulette("purple", 6);
    public static final RegistryObject<Item> ROULETTE_PINK = roulette("pink", 7);

    // --- Eventos del mundo ---
    public static final RegistryObject<Item> SMOKE_BOMB = action("smoke_bomb", (p, h) -> {
        Game.smoke(p.server, 25);
        return true;
    }, true, false, 20, 16, Rarity.UNCOMMON);

    public static final RegistryObject<Item> BLACK_CIRCLE = action("black_circle", (p, h) -> {
        Game.setEclipse(p.server, !Game.data(p.server).eclipse);
        return true;
    }, false, true, 40, 1, Rarity.EPIC);

    public static final RegistryObject<Item> LAYER_UP = action("layer_up", (p, h) -> {
        if (p.isShiftKeyDown()) {
            Game.setLayer(p.server, true, false, Game.data(p.server).layerUp);
        } else {
            Game.open(p, "layer_up");
        }
        return true;
    }, false, false, 10, 1, Rarity.RARE);

    public static final RegistryObject<Item> LAYER_DOWN = action("layer_down", (p, h) -> {
        if (p.isShiftKeyDown()) {
            Game.setLayer(p.server, false, false, Game.data(p.server).layerDown);
        } else {
            Game.open(p, "layer_down");
        }
        return true;
    }, false, false, 10, 1, Rarity.RARE);

    public static final RegistryObject<Item> ACID_DROP = action("acid_drop", (p, h) -> {
        Game.acid(p.server, 45);
        return true;
    }, true, false, 20, 16, Rarity.UNCOMMON);

    public static final RegistryObject<Item> FLIGHT_FEATHER = action("flight_feather", (p, h) -> {
        Game.toggleFly(p);
        return true;
    }, false, false, 10, 1, Rarity.UNCOMMON);

    // --- Corazones y menus ---
    public static final RegistryObject<Item> CONFIG_HEART = action("config_heart", (p, h) -> {
        Game.open(p, "panel");
        return true;
    }, false, true, 10, 1, Rarity.EPIC);

    public static final RegistryObject<Item> CRAFT_LIMIT_HEART = action("craft_limit_heart", (p, h) -> {
        Game.open(p, "craftlimit");
        return true;
    }, false, false, 10, 1, Rarity.RARE);

    public static final RegistryObject<Item> GOLD_HEART = action("gold_heart", (p, h) -> Game.goldHeart(p),
            true, true, 20, 16, Rarity.EPIC);

    public static final RegistryObject<Item> RESURRECTION_HEART = action("resurrection_heart", (p, h) -> {
        Game.say(p, "Usa este corazón sobre una Fogata de Resurrección");
        return false;
    }, false, true, 0, 16, Rarity.EPIC);

    public static final RegistryObject<Item> MISSION_BOOK = action("mission_book", (p, h) -> {
        Game.open(p, "mission");
        return true;
    }, false, false, 10, 1, Rarity.UNCOMMON);

    public static final RegistryObject<Item> MOMENT_CRYSTAL = action("moment_crystal", (p, h) -> {
        Game.open(p, "moment");
        return true;
    }, false, true, 10, 1, Rarity.RARE);

    public static final RegistryObject<Item> DIFFICULTY_DIE = action("difficulty_die", (p, h) -> {
        Game.cycleDifficulty(p.server);
        return true;
    }, false, false, 20, 1, Rarity.RARE);

    // --- Totems ---
    public static final RegistryObject<Item> TOTEM_LIFE = action("totem_life", (p, h) -> {
        Game.say(p, "Se activa solo al morir (guárdalo en el inventario)");
        return false;
    }, false, true, 0, 1, Rarity.EPIC);

    public static final RegistryObject<Item> TOTEM_REVIVE = action("totem_revive", (p, h) -> {
        Game.open(p, "revive");
        return false;
    }, false, true, 10, 1, Rarity.EPIC);

    public static final RegistryObject<Item> TOTEM_CHAOS = action("totem_chaos", (p, h) -> {
        Game.chaos(p.server);
        return true;
    }, true, true, 40, 1, Rarity.EPIC);

    // --- Bloque ---
    public static final RegistryObject<Item> RESURRECTION_CAMPFIRE = ITEMS.register("resurrection_campfire",
            () -> new BlockItem(ModBlocks.RESURRECTION_CAMPFIRE.get(), new Item.Properties().rarity(Rarity.EPIC)));
}
