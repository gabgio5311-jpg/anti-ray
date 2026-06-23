package com.api.serverutils.network;

import com.api.serverutils.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.api.serverutils.AlertHistoryManager;
import java.util.function.Supplier;

public class HistoryRequestPacket {
    public HistoryRequestPacket() {}
    public HistoryRequestPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}
    public static void handle(HistoryRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            System.out.println("[ANTI-CHEAT DEBUG] Servidor recebeu o pedido de histórico.");
            if (context.getSender() != null) {
                System.out.println("[ANTI-CHEAT DEBUG] Permissão do jogador OP: " + context.getSender().hasPermissions(2));

                // Se o if falhar por falta de OP, saberemos aqui!
                if (context.getSender().hasPermissions(2)) {
                    System.out.println("[ANTI-CHEAT DEBUG] Enviando resposta com a lista para o cliente...");
                    ModNetwork.CHANNEL.sendTo(
                            new HistoryResponsePacket(AlertHistoryManager.HISTORICO),
                            context.getSender().connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }
}