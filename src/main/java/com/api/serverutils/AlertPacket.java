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
}