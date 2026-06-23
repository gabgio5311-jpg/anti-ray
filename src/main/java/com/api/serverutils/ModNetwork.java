package com.api.serverutils;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.api.serverutils.network.HistoryRequestPacket;
import com.api.serverutils.network.HistoryResponsePacket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ServerUtilsCore.MOD_ID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    public static final ConcurrentHashMap<UUID, String> JOGADORES_COM_XRAY = new ConcurrentHashMap<>();
    // Ciclos restantes (de 0,5s cada) até o jogador levar kick. Gerenciado pelo ServerTickHandler.
    public static final ConcurrentHashMap<UUID, Integer> CONTAGEM_KICK = new ConcurrentHashMap<>();

    public static void register() {
        // Registro do alerta original (ID 0)
        CHANNEL.registerMessage(packetId++,
                AlertPacket.class,
                AlertPacket::toBytes,
                AlertPacket::new,
                ModNetwork::handleAlertPacket);

        // --- ADICIONADOS COM SEU PADRÃO DE REGISTRO (IDs 1 e 2) ---
        CHANNEL.registerMessage(packetId++,
                HistoryRequestPacket.class,
                HistoryRequestPacket::toBytes,
                HistoryRequestPacket::new,
                HistoryRequestPacket::handle);

        CHANNEL.registerMessage(packetId++,
                HistoryResponsePacket.class,
                HistoryResponsePacket::toBytes,
                HistoryResponsePacket::new,
                HistoryResponsePacket::handle);
    }

    public static void handleAlertPacket(AlertPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer jogador = ctx.get().getSender();
            if (jogador != null) {
                String nomeJogador = jogador.getGameProfile().getName();
                UUID uuidJogador = jogador.getUUID();

                // --- SE O JOGADOR REMOVEU AS TRAPAÇAS (cliente envia "Nenhum") ---
                if (msg.getTipoCheat().equals("Nenhum")) {
                    if (JOGADORES_COM_XRAY.containsKey(uuidJogador)) {
                        JOGADORES_COM_XRAY.remove(uuidJogador);
                        CONTAGEM_KICK.remove(uuidJogador); // cancela a contagem de kick

                        // Limpa título, subtítulo e actionbar de forma instantânea e garantida.
                        jogador.connection.send(new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(true));

                        System.out.println("[ANTI-CHEAT] O jogador " + nomeJogador + " removeu o X-Ray e foi perdoado.");
                    }
                    return; // Não salva "Nenhum" no histórico
                }

                if (JOGADORES_COM_XRAY.containsKey(uuidJogador)) return;

                Component alertaChat = Component.literal("§c[ANTI-CHEAT] O jogador §e" + nomeJogador +
                        " §cfoi detectado usando " + msg.getTipoCheat() + ": §7" + msg.getNomeDetectado());
                jogador.server.getPlayerList().broadcastSystemMessage(alertaChat, false);

                // 1. Salva no arquivo de texto antigo
                salvarNoHistorico(nomeJogador, msg.getTipoCheat(), msg.getNomeDetectado());

                // 2. ADICIONADO: Salva no gerenciador para aparecer na nova aba visual (Tecla H)
                AlertHistoryManager.adicionarAlerta(nomeJogador, msg.getTipoCheat(), msg.getNomeDetectado());

                String motivo = msg.getTipoCheat() + " (" + msg.getNomeDetectado() + ")";
                JOGADORES_COM_XRAY.put(uuidJogador, motivo);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void salvarNoHistorico(String jogador, String tipo, String detalhe) {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) logDir.mkdirs();

            File logFile = new File(logDir, "xray_alerts.txt");
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            pw.println("[" + dtf.format(now) + "] Jogador: " + jogador + " | Tipo: " + tipo + " | Detectado: " + detalhe);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}