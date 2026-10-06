package com.desafio4.server;

import com.desafio4.data.D4Data;
import com.desafio4.data.Mission;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/** Comando /d4 : todo lo del mod se puede activar por comandos. */
public class ModCommands {
    private static final String[] COLORS = {"rojo", "naranja", "amarillo", "verde", "celeste", "azul", "morado", "rosa"};

    private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGEST =
            (c, b) -> SharedSuggestionProvider.suggest(new String[]{"rojo", "naranja", "amarillo", "verde", "celeste", "azul", "morado", "rosa", "random"}, b);
    private static final SuggestionProvider<CommandSourceStack> DIFF_SUGGEST =
            (c, b) -> SharedSuggestionProvider.suggest(D4Data.PRESET_IDS, b);
    private static final SuggestionProvider<CommandSourceStack> MOMENT_SUGGEST =
            (c, b) -> SharedSuggestionProvider.suggest(new String[]{"revelacion", "epico", "terror", "victoria"}, b);

    private static int ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static MinecraftServer srv(CommandContext<CommandSourceStack> c) {
        return c.getSource().getServer();
    }

    public static void register(CommandDispatcher<CommandSourceStack> disp) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("d4").requires(s -> s.hasPermission(2));

        // /d4 menu | misiones
        root.then(Commands.literal("menu").executes(c -> {
            Game.open(c.getSource().getPlayerOrException(), "panel");
            return 1;
        }));
        root.then(Commands.literal("misiones").executes(c -> {
            Game.open(c.getSource().getPlayerOrException(), "mission");
            return 1;
        }));

        // /d4 vidas ...
        root.then(Commands.literal("vidas")
                .then(Commands.literal("set").then(Commands.argument("jugadores", EntityArgument.players())
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 10)).executes(c -> setLives(c, false)))))
                .then(Commands.literal("add").then(Commands.argument("jugadores", EntityArgument.players())
                        .then(Commands.argument("n", IntegerArgumentType.integer(-10, 10)).executes(c -> setLives(c, true)))))
                .then(Commands.literal("ver").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                    D4Data d = Game.data(srv(c));
                    return ok(c, p.getName().getString() + " tiene " + d.getLives(p.getUUID()) + " vidas");
                }))));

        // /d4 config ...
        root.then(Commands.literal("config")
                .then(Commands.literal("inicio").then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.startLives = IntegerArgumentType.getInteger(c, "n");
                    if (d.maxLives < d.startLives) d.maxLives = d.startLives;
                    d.setDirty();
                    Game.syncAll(srv(c));
                    return ok(c, "Vidas iniciales: " + d.startLives);
                })))
                .then(Commands.literal("maximo").then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.maxLives = Math.max(IntegerArgumentType.getInteger(c, "n"), d.startLives);
                    d.setDirty();
                    Game.syncAll(srv(c));
                    return ok(c, "Vidas máximas: " + d.maxLives);
                })))
                .then(Commands.literal("limitecorazones").then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.craftLimit = IntegerArgumentType.getInteger(c, "n");
                    D4Data.refresh(d);
                    d.setDirty();
                    return ok(c, "Se pueden craftear " + d.craftLimit + " corazones de resurrección (ya hechos: " + d.crafted + ")");
                })))
                .then(Commands.literal("reiniciarcrafteos").executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.crafted = 0;
                    D4Data.refresh(d);
                    d.setDirty();
                    return ok(c, "Contador de corazones crafteados reiniciado");
                }))
                .then(Commands.literal("dropcorazon").then(Commands.argument("valor", BoolArgumentType.bool()).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.pvpDrop = BoolArgumentType.getBool(c, "valor");
                    d.setDirty();
                    return ok(c, "Corazón dorado en PvP: " + d.pvpDrop);
                }))));

        // /d4 dificultad ...
        root.then(Commands.literal("dificultad")
                .then(Commands.argument("nivel", StringArgumentType.word()).suggests(DIFF_SUGGEST).executes(c -> {
                    String n = StringArgumentType.getString(c, "nivel").toLowerCase();
                    for (int i = 0; i < D4Data.PRESET_IDS.length; i++) {
                        if (D4Data.PRESET_IDS[i].equals(n)) {
                            Game.preset(srv(c), i);
                            return ok(c, "Dificultad: " + D4Data.PRESET_NAMES[i]);
                        }
                    }
                    c.getSource().sendFailure(Component.literal("Niveles: facil, normal, dificil, hardcore, pesadilla"));
                    return 0;
                }))
                .then(Commands.literal("danio").then(Commands.argument("x", FloatArgumentType.floatArg(0.1f, 10f)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.mobDamage = FloatArgumentType.getFloat(c, "x");
                    d.setDirty();
                    return ok(c, "Daño de mobs x" + d.mobDamage);
                })))
                .then(Commands.literal("vida").then(Commands.argument("x", FloatArgumentType.floatArg(0.1f, 10f)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.mobHealth = FloatArgumentType.getFloat(c, "x");
                    d.setDirty();
                    return ok(c, "Vida de mobs x" + d.mobHealth + " (aplica a los mobs nuevos)");
                })))
                .then(Commands.literal("velocidad").then(Commands.argument("x", FloatArgumentType.floatArg(0.5f, 3f)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.mobSpeed = FloatArgumentType.getFloat(c, "x");
                    d.setDirty();
                    return ok(c, "Velocidad de mobs x" + d.mobSpeed + " (aplica a los mobs nuevos)");
                })))
                .then(Commands.literal("hambre").then(Commands.argument("x", FloatArgumentType.floatArg(0.1f, 10f)).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.hunger = FloatArgumentType.getFloat(c, "x");
                    d.setDirty();
                    return ok(c, "Hambre x" + d.hunger);
                })))
                .then(Commands.literal("regen").then(Commands.argument("valor", BoolArgumentType.bool()).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    d.regen = BoolArgumentType.getBool(c, "valor");
                    d.setDirty();
                    Game.applyRules(srv(c));
                    return ok(c, "Regeneración natural: " + d.regen);
                }))));

        // /d4 mision ...
        root.then(Commands.literal("mision")
                .then(Commands.literal("lista").executes(c -> {
                    D4Data d = Game.data(srv(c));
                    for (Mission m : d.missions) {
                        String st = m.done ? "[HECHA] " : "[" + m.progress + "/" + m.amount + "] ";
                        c.getSource().sendSuccess(() -> Component.literal(m.id + " " + st + m.text), false);
                    }
                    return d.missions.size();
                }))
                .then(Commands.literal("agregar").then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000))
                        .then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> {
                            D4Data d = Game.data(srv(c));
                            int n = 1;
                            for (Mission m : d.missions) {
                                try {
                                    n = Math.max(n, Integer.parseInt(m.id.substring(1)) + 1);
                                } catch (Exception ignored) {
                                }
                            }
                            Mission m = new Mission("m" + n, StringArgumentType.getString(c, "texto"), "MANUAL", "",
                                    IntegerArgumentType.getInteger(c, "cantidad"));
                            d.missions.add(m);
                            d.setDirty();
                            return ok(c, "Misión " + m.id + " agregada");
                        }))))
                .then(Commands.literal("progreso").then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 100000)).executes(c -> {
                            D4Data d = Game.data(srv(c));
                            Mission m = d.mission(StringArgumentType.getString(c, "id"));
                            if (m == null) {
                                c.getSource().sendFailure(Component.literal("No existe esa misión"));
                                return 0;
                            }
                            Missions.add(srv(c), d, m, IntegerArgumentType.getInteger(c, "n"));
                            return ok(c, m.id + ": " + m.progress + "/" + m.amount);
                        }))))
                .then(Commands.literal("completar").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    Mission m = d.mission(StringArgumentType.getString(c, "id"));
                    if (m == null) {
                        c.getSource().sendFailure(Component.literal("No existe esa misión"));
                        return 0;
                    }
                    if (!m.done) Missions.complete(srv(c), d, m);
                    return 1;
                })))
                .then(Commands.literal("quitar").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                    D4Data d = Game.data(srv(c));
                    String id = StringArgumentType.getString(c, "id");
                    boolean r = d.missions.removeIf(m -> m.id.equals(id));
                    d.setDirty();
                    return ok(c, r ? "Misión quitada" : "No existe esa misión");
                })))
                .then(Commands.literal("reiniciar").executes(c -> {
                    D4Data d = Game.data(srv(c));
                    for (Mission m : d.missions) {
                        m.progress = 0;
                        m.done = false;
                    }
                    d.setDirty();
                    return ok(c, "Misiones reiniciadas");
                })));

        // /d4 ruleta <color|random>
        root.then(Commands.literal("ruleta")
                .executes(c -> {
                    Game.roulette(srv(c), -1);
                    return 1;
                })
                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGEST).executes(c -> {
                    String n = StringArgumentType.getString(c, "color").toLowerCase();
                    int idx = -1;
                    for (int i = 0; i < COLORS.length; i++) if (COLORS[i].equals(n)) idx = i;
                    Game.roulette(srv(c), idx);
                    return 1;
                })));

        // /d4 momento <tipo> <titulo|subtitulo>
        root.then(Commands.literal("momento")
                .then(Commands.argument("tipo", StringArgumentType.word()).suggests(MOMENT_SUGGEST)
                        .then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> {
                            String[] parts = StringArgumentType.getString(c, "texto").split("\\|", 2);
                            Game.moment(srv(c), StringArgumentType.getString(c, "tipo"), parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
                            return 1;
                        }))));

        // /d4 caja <texto>
        root.then(Commands.literal("caja").then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> {
            Game.box(srv(c), StringArgumentType.getString(c, "texto"));
            return 1;
        })));

        // /d4 humo [segundos] , acido [segundos]
        root.then(Commands.literal("humo")
                .executes(c -> {
                    Game.smoke(srv(c), 25);
                    return 1;
                })
                .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600)).executes(c -> {
                    Game.smoke(srv(c), IntegerArgumentType.getInteger(c, "segundos"));
                    return 1;
                })));
        root.then(Commands.literal("acido")
                .executes(c -> {
                    Game.acid(srv(c), 45);
                    return 1;
                })
                .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600)).executes(c -> {
                    Game.acid(srv(c), IntegerArgumentType.getInteger(c, "segundos"));
                    return 1;
                })));

        // /d4 eclipse <on|off>
        root.then(Commands.literal("eclipse").then(Commands.argument("valor", BoolArgumentType.bool()).executes(c -> {
            Game.setEclipse(srv(c), BoolArgumentType.getBool(c, "valor"));
            return 1;
        })));

        // /d4 vuelo <jugadores> <valor>
        root.then(Commands.literal("vuelo").then(Commands.argument("jugadores", EntityArgument.players())
                .then(Commands.argument("valor", BoolArgumentType.bool()).executes(c -> {
                    boolean on = BoolArgumentType.getBool(c, "valor");
                    D4Data d = Game.data(srv(c));
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "jugadores")) {
                        if (on) d.flyers.add(p.getUUID());
                        else d.flyers.remove(p.getUUID());
                        Game.applyFly(p);
                    }
                    d.setDirty();
                    return ok(c, "Vuelo " + (on ? "activado" : "desactivado"));
                }))));

        // /d4 capa subir|bajar <y> | off
        root.then(Commands.literal("capa")
                .then(Commands.literal("subir")
                        .then(Commands.argument("y", IntegerArgumentType.integer(-64, 320)).executes(c -> {
                            Game.setLayer(srv(c), true, true, IntegerArgumentType.getInteger(c, "y"));
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            Game.setLayer(srv(c), true, false, Game.data(srv(c)).layerUp);
                            return 1;
                        })))
                .then(Commands.literal("bajar")
                        .then(Commands.argument("y", IntegerArgumentType.integer(-64, 320)).executes(c -> {
                            Game.setLayer(srv(c), false, true, IntegerArgumentType.getInteger(c, "y"));
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            Game.setLayer(srv(c), false, false, Game.data(srv(c)).layerDown);
                            return 1;
                        }))));

        // /d4 revivir <jugador> [x y z]
        root.then(Commands.literal("revivir").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
            ServerPlayer t = EntityArgument.getPlayer(c, "jugador");
            D4Data d = Game.data(srv(c));
            d.dead.put(t.getUUID(), t.getName().getString());
            Vec3 at = c.getSource().getPosition();
            Game.revive(srv(c), t.getUUID(), c.getSource().getLevel(), at);
            return ok(c, "Reviviendo a " + t.getName().getString());
        })));

        // /d4 animacion muerte|revivir <jugador>
        root.then(Commands.literal("animacion")
                .then(Commands.literal("muerte").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
                    ServerPlayer t = EntityArgument.getPlayer(c, "jugador");
                    Game.fx("death", t.getName().getString() + Game.SEP
                            + Component.Serializer.toJson(Component.literal(t.getName().getString() + " perdió una vida")) + Game.SEP + t.getUUID());
                    return 1;
                })))
                .then(Commands.literal("revivir").then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
                    Game.fx("revive", EntityArgument.getPlayer(c, "jugador").getName().getString());
                    return 1;
                })))
                .then(Commands.literal("corazondorado").then(Commands.argument("jugadores", EntityArgument.players()).executes(c -> {
                    Collection<ServerPlayer> ps = EntityArgument.getPlayers(c, "jugadores");
                    for (ServerPlayer p : ps) Game.fxTo(p, "goldheart", "");
                    return ps.size();
                }))));

        // /d4 apagar
        root.then(Commands.literal("apagar").executes(c -> {
            Game.stopAll(srv(c));
            return ok(c, "Efectos apagados");
        }));

        disp.register(root);
    }

    private static int setLives(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
        D4Data d = Game.data(srv(c));
        int n = IntegerArgumentType.getInteger(c, "n");
        Collection<ServerPlayer> ps = EntityArgument.getPlayers(c, "jugadores");
        for (ServerPlayer p : ps) {
            int v = add ? d.getLives(p.getUUID()) + n : n;
            d.setLives(p.getUUID(), v);
            if (d.getLives(p.getUUID()) <= 0) {
                d.dead.put(p.getUUID(), p.getName().getString());
                Game.makeSpectator(p);
            } else if (d.dead.remove(p.getUUID()) != null) {
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            Game.sync(p);
        }
        d.setDirty();
        return ok(c, "Vidas actualizadas para " + ps.size() + " jugador(es)");
    }
}
