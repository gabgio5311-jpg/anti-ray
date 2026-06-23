package com.api.serverutils;

import net.minecraft.network.FriendlyByteBuf;

public class AlertPacket {
    private final String tipoCheat;
    private final String nomeDetectado;

    public AlertPacket(String tipoCheat, String nomeDetectado) {
        this.tipoCheat = tipoCheat;
        this.nomeDetectado = nomeDetectado; // <-- Corrigido aqui (estava nomeDetected)
    }

    public AlertPacket(FriendlyByteBuf buffer) {
        this.tipoCheat = buffer.readUtf(32767);
        this.nomeDetectado = buffer.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.tipoCheat);
        buffer.writeUtf(this.nomeDetectado);
    }

    public String getTipoCheat() { return tipoCheat; }
    public String getNomeDetectado() { return nomeDetectado; }
    // Adicione ou atualize o método handle dentro do seu AlertPacket:
    public static void handle(AlertPacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> contextSupplier) {
        net.minecraftforge.network.NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Executa apenas no Servidor Dedicado
            net.minecraft.server.level.ServerPlayer jogador = context.getSender();
            if (jogador != null) {
                String nomeJogador = jogador.getName().getString();
                java.util.UUID uuid = jogador.getUUID();

                // --- NOVIDADE: SE O JOGADOR REMOVEU AS TRAPAÇAS ---
                if (packet.getTipoCheat().equals("Nenhum")) {
                    if (ModNetwork.JOGADORES_COM_XRAY.containsKey(uuid)) {
                        ModNetwork.JOGADORES_COM_XRAY.remove(uuid);

                        // Limpa título, subtítulo e actionbar de forma instantânea e garantida.
                        jogador.connection.send(new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(true));

                        System.out.println("[ANTI-CHEAT] O jogador " + nomeJogador + " removeu o X-Ray e foi perdoado.");
                    }
                    return; // Interrompe aqui para não salvar "Nenhum" no histórico de alertas!
                }

                // --- LOGICA ANTIGA: SE ELE FOI PEGO COM CHEAT ---
                // Coloca o jogador na lista do ServerTickHandler para começar o spam visual
                ModNetwork.JOGADORES_COM_XRAY.put(uuid, packet.getNomeDetectado());

                // 1. SALVA NO HISTÓRICO EM MEMÓRIA (Para o comando /serverutils alerts achar)
                AlertHistoryManager.adicionarAlerta(nomeJogador, packet.getTipoCheat(), packet.getNomeDetectado());

                // 2. Aqui fica o resto do seu código antigo (o bombardeio visual, logs no console, etc.)
                System.out.println("[ANTI-CHEAT] O jogador " + nomeJogador + " foi pego usando: " + packet.getTipoCheat());
            }
        });
        context.setPacketHandled(true);
    }
}