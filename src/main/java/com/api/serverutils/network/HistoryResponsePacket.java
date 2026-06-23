package com.api.serverutils.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.api.serverutils.AlertHistoryManager.AlertEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HistoryResponsePacket {
    private final List<AlertEntry> lista;

    public HistoryResponsePacket(List<AlertEntry> lista) {
        this.lista = lista;
    }

    public HistoryResponsePacket(FriendlyByteBuf buf) {
        int tamanho = buf.readInt();
        this.lista = new ArrayList<>();
        for (int i = 0; i < tamanho; i++) {
            lista.add(new AlertEntry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(lista.size());
        for (AlertEntry entry : lista) {
            buf.writeUtf(entry.playerName);
            buf.writeUtf(entry.cheatType);
            buf.writeUtf(entry.cheatName);
            buf.writeUtf(entry.timestamp);
        }
    }

    public List<AlertEntry> getLista() {
        return this.lista;
    }

    public static void handle(HistoryResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Usa o DistExecutor para executar o código visual APENAS se estiver no cliente de fato.
            // O servidor dedicado vai ignorar completamente o que estiver dentro desse bloco.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.abrirTelaHistorico(packet));
        });
        context.setPacketHandled(true);
    }
}