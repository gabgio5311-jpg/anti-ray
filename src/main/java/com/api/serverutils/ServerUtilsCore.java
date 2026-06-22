package com.api.serverutils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext; // <-- Corrigido o caminho do pacote aqui

@Mod(ServerUtilsCore.MOD_ID)
public class ServerUtilsCore {
    public static final String MOD_ID = "server_utils_api";

    public ServerUtilsCore() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}