package com.api.serverutils.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.api.serverutils.gui.ScreenHistory;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void abrirTelaHistorico(HistoryResponsePacket packet) {
        System.out.println("[ANTI-CHEAT DEBUG] Cliente recebeu a resposta do servidor! Tentando abrir a GUI...");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new ScreenHistory(packet.getLista()));
            System.out.println("[ANTI-CHEAT DEBUG] mc.setScreen foi chamado com sucesso.");
        }
    }
}