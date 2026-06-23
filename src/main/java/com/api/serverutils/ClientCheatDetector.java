package com.api.serverutils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import java.io.File;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import com.api.serverutils.network.HistoryRequestPacket;
import com.mojang.blaze3d.platform.InputConstants;

@OnlyIn(Dist.CLIENT)
public class ClientCheatDetector {

    private static int contadorTicks = 0;
    public static boolean jaDetectou = false;

    // 1. CRIANDO A TECLA 'H'
    public static final KeyMapping ABRIR_HISTORICO_KEY = new KeyMapping(
            "Abrir Histórico Anti-Cheat",
            InputConstants.KEY_H,
            "key.categories.misc"
    );

    // Evento que escuta o Tick do Cliente (Barramento FORGE padrão)
    @Mod.EventBusSubscriber(modid = ServerUtilsCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            // --- ESCUTA O CLIQUE DA TECLA H COM O SEU DEBUG (Roda a cada tick do jogo!) ---
            while (ABRIR_HISTORICO_KEY.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    System.out.println("[ANTI-CHEAT DEBUG] Tecla H detectada! Enviando pacote ao servidor...");
                    ModNetwork.CHANNEL.sendToServer(new HistoryRequestPacket());
                }
            }

            contadorTicks++;
            if (contadorTicks >= 100) { // A cada 5 segundos
                contadorTicks = 0;

                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null || mc.isLocalServer()) {
                    return;
                }
                verificarCliente();
            }
        }
    }

    // 2. REGISTRANDO A TECLA NO EVENTO DE INICIALIZAÇÃO (Barramento MOD essencial)
    @Mod.EventBusSubscriber(modid = ServerUtilsCore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(ABRIR_HISTORICO_KEY);
        }
    }

    // Palavras-chave procuradas nos NOMES dos arquivos de cada pasta
    private static final String[] PALAVRAS_RESOURCE_PACK = {"xray", "x-ray"};
    private static final String[] PALAVRAS_MOD = {"xray", "x-ray", "wurst", "cheatutils", "meteor", "inertiaclient"};

    // Lógica interna que valida e limpa o estado
    private static void verificarCliente() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = mc.gameDirectory;

        File pastaResourcePacks = new File(gameDir, "resourcepacks");
        File pastaMods = new File(gameDir, "mods");

        String tipoDetectado = null;
        String nomeDetectado = null;

        // 1. Escaneia a PASTA resourcepacks (mesmo que o pack NÃO esteja selecionado/ativado)
        String rp = procurarNaPasta(pastaResourcePacks, PALAVRAS_RESOURCE_PACK);
        if (rp != null) {
            tipoDetectado = "Resource Pack";
            nomeDetectado = rp;
        }

        // 2. Escaneia a PASTA mods (pelo nome do arquivo .jar)
        if (tipoDetectado == null) {
            String m = procurarNaPasta(pastaMods, PALAVRAS_MOD);
            if (m != null) {
                tipoDetectado = "Mod Cheat";
                nomeDetectado = m;
            }
        }

        // 3. Reforço: checa os mods realmente CARREGADOS (pega jar renomeado, pois usa o modId interno)
        // ATENÇÃO: um mod carregado NÃO descarrega ao apagar o .jar; só some ao REINICIAR o jogo.
        if (tipoDetectado == null) {
            for (IModInfo mod : ModList.get().getMods()) {
                String modId = mod.getModId().toLowerCase();
                for (String palavra : PALAVRAS_MOD) {
                    if (modId.contains(palavra)) {
                        tipoDetectado = "Mod Cheat (carregado na memoria)";
                        nomeDetectado = mod.getDisplayName();
                        break;
                    }
                }
                if (tipoDetectado != null) break;
            }
        }

        // --- DEBUG: mostra exatamente o que foi verificado ---
        System.out.println("[ANTI-CHEAT DEBUG] Verificando...");
        System.out.println("  resourcepacks -> " + pastaResourcePacks.getAbsolutePath() + " (existe: " + pastaResourcePacks.isDirectory() + ")");
        System.out.println("  mods          -> " + pastaMods.getAbsolutePath() + " (existe: " + pastaMods.isDirectory() + ")");
        System.out.println("  Resultado     -> " + (tipoDetectado == null ? "LIMPO" : tipoDetectado + " | " + nomeDetectado));

        // --- ENVIO DO RESULTADO ---
        if (tipoDetectado != null) {
            if (!jaDetectou) {
                ModNetwork.CHANNEL.sendToServer(new AlertPacket(tipoDetectado, nomeDetectado));
                jaDetectou = true;
            }
        } else if (jaDetectou) {
            // Só limpa quando o arquivo foi REALMENTE removido da pasta
            jaDetectou = false;
            System.out.println("[ANTI-CHEAT DEBUG] O jogador removeu as trapaças das pastas. Avisando o servidor...");
            ModNetwork.CHANNEL.sendToServer(new AlertPacket("Nenhum", "Limpo"));
        }
    }

    // Procura na pasta um arquivo cujo nome contenha qualquer uma das palavras-chave.
    // Retorna o nome do arquivo encontrado, ou null se nada bater.
    private static String procurarNaPasta(File pasta, String[] palavrasChave) {
        if (pasta == null || !pasta.isDirectory()) return null;

        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return null;

        for (File arquivo : arquivos) {
            String nome = arquivo.getName().toLowerCase();
            for (String palavra : palavrasChave) {
                if (nome.contains(palavra)) {
                    return arquivo.getName();
                }
            }
        }
        return null;
    }
} // <-- Corrigido aqui para fechar a classe corretamente!