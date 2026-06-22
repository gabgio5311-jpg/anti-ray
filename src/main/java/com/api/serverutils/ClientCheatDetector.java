package com.api.serverutils;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;

@Mod.EventBusSubscriber(modid = ServerUtilsCore.MOD_ID, value = Dist.CLIENT)
public class ClientCheatDetector {

    @SubscribeEvent
    public static void onServerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();

        // CORREÇÃO: Se event.getConnection() for local (Singleplayer), não faz nada
        if (mc.isLocalServer()) {
            return; // Ignora a checagem se estiver jogando offline/singleplayer
        }

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                verificarCliente();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
    private static void verificarCliente() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourcePackRepository() == null) return;

        for (Pack pack : mc.getResourcePackRepository().getSelectedPacks()) {
            String id = pack.getId().toLowerCase();
            String titulo = pack.getTitle().getString().toLowerCase();

            if (id.contains("xray") || id.contains("x-ray") || titulo.contains("xray") || titulo.contains("x-ray")) {
                ModNetwork.CHANNEL.sendToServer(new AlertPacket("Resource Pack", pack.getTitle().getString()));
            }
        }

        for (IModInfo mod : ModList.get().getMods()) {
            String modId = mod.getModId().toLowerCase();

            if (modId.contains("xray") || modId.contains("wurst") || modId.contains("meteor") || modId.contains("inertiaclient")) {
                ModNetwork.CHANNEL.sendToServer(new AlertPacket("Mod Cheat", mod.getDisplayName()));
            }
        }
    }
}