package com.api.serverutils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ServerUtilsCore.MOD_ID)
public class ServerTickHandler {

    private static int contadorTicks = 0;

    // === CONFIGURAÇÃO ===
    // Quantos segundos o jogador tem para remover o X-Ray antes de levar kick.
    private static final int SEGUNDOS_ATE_KICK = 10;
    // O tick roda a cada 10 ticks = 0,5s, ou seja, 2 ciclos por segundo.
    private static final int CICLOS_ATE_KICK = SEGUNDOS_ATE_KICK * 2;

    // Quando o jogador desconecta, removemos a "ficha" dele.
    // Sem isso, o servidor continuaria spamando o título quando ele reconectasse,
    // mesmo já tendo removido o X-Ray, pois ninguém avisa que ele limpou.
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ModNetwork.CONTAGEM_KICK.remove(uuid);
        if (ModNetwork.JOGADORES_COM_XRAY.remove(uuid) != null) {
            System.out.println("[ANTI-CHEAT] Jogador " + event.getEntity().getName().getString()
                    + " desconectou; ficha de X-Ray limpa.");
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        contadorTicks++;
        if (contadorTicks >= 10) {
            contadorTicks = 0;

            if (ModNetwork.JOGADORES_COM_XRAY.isEmpty()) return;

            event.getServer().getPlayerList().getPlayers().forEach(jogador -> {
                UUID uuid = jogador.getUUID();

                // O tick só age se ele REALMENTE estiver no mapa.
                // Se o jogador foi perdoado (removeu o X-Ray), este if falha e tudo para.
                if (!ModNetwork.JOGADORES_COM_XRAY.containsKey(uuid)) return;

                // Pega/inicia a contagem regressiva deste jogador.
                int ciclosRestantes = ModNetwork.CONTAGEM_KICK.getOrDefault(uuid, CICLOS_ATE_KICK);

                if (ciclosRestantes <= 0) {
                    // Tempo esgotado: expulsa o jogador do servidor.
                    expulsarJogador(jogador);
                    ModNetwork.JOGADORES_COM_XRAY.remove(uuid);
                    ModNetwork.CONTAGEM_KICK.remove(uuid);
                } else {
                    int segundosRestantes = (int) Math.ceil(ciclosRestantes / 2.0);
                    spamarTelaDoXRay(jogador, segundosRestantes);
                    ModNetwork.CONTAGEM_KICK.put(uuid, ciclosRestantes - 1);
                }
            });
        }
    }

    private static void expulsarJogador(ServerPlayer jogador) {
        System.out.println("[ANTI-CHEAT] Expulsando " + jogador.getName().getString() + " por X-Ray.");
        Component motivoKick = Component.literal(
                "§c§lX-RAY DETECTADO\n\n§eVocê foi removido do servidor.\n§7Remova o mod/textura de X-Ray para poder voltar.");
        jogador.connection.disconnect(motivoKick);
    }

    private static void spamarTelaDoXRay(ServerPlayer jogador, int segundosRestantes) {
        Component tituloGrande = Component.literal("§c§lX-RAY DETECTADO!");
        Component subtitulo = Component.literal(
                "§eRemova o X-Ray! §cKick em §4§l" + segundosRestantes + "s");

        jogador.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 0));
        jogador.connection.send(new ClientboundSetTitleTextPacket(tituloGrande));

        // Actionbar (some sozinha). NÃO usar chat aqui: mensagens de chat são
        // permanentes e continuariam na tela mesmo depois de o jogador remover o X-Ray.
        jogador.sendSystemMessage(subtitulo, true);
    }
}