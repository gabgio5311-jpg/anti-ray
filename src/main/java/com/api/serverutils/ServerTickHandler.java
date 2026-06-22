package com.api.serverutils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ServerUtilsCore.MOD_ID)
public class ServerTickHandler {

    private static int contadorTicks = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        contadorTicks++;
        if (contadorTicks >= 10) {
            contadorTicks = 0;

            if (ModNetwork.JOGADORES_COM_XRAY.isEmpty()) return;

            event.getServer().getPlayerList().getPlayers().forEach(jogador -> {
                UUID uuid = jogador.getUUID();

                if (ModNetwork.JOGADORES_COM_XRAY.containsKey(uuid)) {
                    spamarTelaDoXRay(jogador);
                }
            });
        }
    }

    private static void spamarTelaDoXRay(ServerPlayer jogador) {
        Component tituloGrande = Component.literal("§c§lX-RAY DETECTADO!");
        Component subtitulo = Component.literal("§eRemova o mod/textura para voltar a jogar.");

        jogador.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 0));
        jogador.connection.send(new ClientboundSetTitleTextPacket(tituloGrande));

        jogador.sendSystemMessage(subtitulo, true);

        for (int i = 0; i < 5; i++) {
            jogador.sendSystemMessage(Component.literal("§c§k|||||||||| §4§lX-RAY DETECTADO §c§k||||||||||"));
        }
    }
}