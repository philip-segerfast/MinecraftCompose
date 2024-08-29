package segerfast.mccompose

import androidx.compose.runtime.*
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.client.event.InputEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import segerfast.mccompose.screen.ComposeScreen
import segerfast.mccompose.screen.node.Text
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

/**
 * Main mod class.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(MinecraftCompose.ID)
object MinecraftCompose {
    const val ID = "mccompose"

    // the logger for our mod
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.log(Level.INFO, "MinecraftCompose #INIT")

        val obj = runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                FORGE_BUS.addListener(MinecraftCompose::composeEvent)
                Minecraft.getInstance()
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
                "test"
            })

        println(obj)

        val x: @Composable () -> Unit = {

        }
    }

    private var numState by mutableIntStateOf(0)

    private fun composeEvent(event: InputEvent.Key) {
        if(event.action != InputConstants.PRESS) return

        when(event.key) {
            InputConstants.KEY_C -> setCompose()
            InputConstants.KEY_UP -> {
                numState++
            }
            InputConstants.KEY_DOWN -> {
                numState--
            }
        }
    }

    private fun setCompose() {
        println("Creating screen...")
        val screen = ComposeScreen(Component.literal("Title")) {

            Text("Number is: $numState")

        }

        println("Setting screen...")
        Minecraft.getInstance().setScreen(screen)
    }

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */
    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
    }

    /**
     * Fired on the global Forge bus.
     */
    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Hello! This is working!")
    }
}

class MyOtherScreen : Screen(Component.literal("Whatever")) {
    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        StringWidget(Component.literal("This is a String"), Minecraft.getInstance().font).render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    fun setContent(content: @Composable () -> Unit) {

    }
}