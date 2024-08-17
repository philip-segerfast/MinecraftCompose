package segerfast.mccompose.screen

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import segerfast.mccompose.Fonts
import segerfast.mccompose.MinecraftCompose

internal interface ComposeOwner {
    fun setContent(content: @Composable () -> Unit)
    fun dispose()
}

class ComposeScreen(title: Component): Screen(title), ComposeOwner {
    private val clock = BroadcastFrameClock() // TODO - CHANGE IMPLEMENTATION!!!
    private val job = Job()
    private val composeContext = clock + job
    private val recomposer = Recomposer(composeContext)
    private val root: MinecraftUiNode = object : MinecraftUiNode() {}
    private val applier = MinecraftUiApplier(root)
    private val composition: Composition = Composition(applier, recomposer)

    override fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
    }

    override fun dispose() {
        composition.dispose()
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        // Draw screen graphics itself and child nodes.
        root.draw(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }
}

internal abstract class MinecraftUiNode {
    val children = mutableListOf<MinecraftUiNode>()

    fun draw(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        drawSelf(graphics, mouseX, mouseY, partialTick)
        drawChildren(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawChildren(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        children.forEach { it.draw(graphics, mouseX, mouseY, partialTick) }
    }

    protected open fun drawSelf(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}
}

internal open class MinecraftRenderableNode(protected val renderable: Renderable) : MinecraftUiNode() {
    override fun drawSelf(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderable.render(graphics, mouseX, mouseY, partialTick)
    }
}

internal class TextNode(
    var stringWidget: StringWidget
): MinecraftUiNode() {
    override fun drawSelf(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.drawSelf(graphics, mouseX, mouseY, partialTick)
        stringWidget.render(graphics, mouseX, mouseY, partialTick)
    }
}

@Composable
fun Text(
    text: String,
    font: Font = Fonts.DEFAULT
) {
    ComposeNode<TextNode, MinecraftUiApplier>(
        factory = {
            val stringWidget = StringWidget(Component.literal(text), Fonts.DEFAULT)
            TextNode(stringWidget)
        },
        update = {
            update(text) { text -> stringWidget.message = Component.literal(text) }
            update(font) { font -> stringWidget = StringWidget(stringWidget.message.copy(), font) }
        }
    )
}

private class MinecraftUiApplier(root: MinecraftUiNode) : AbstractApplier<MinecraftUiNode>(root) {

    private val logger: Logger = LogManager.getLogger(MinecraftCompose.ID)

    override fun onClear() {
        logger.debug("onClear")
    }

    override fun insertBottomUp(index: Int, instance: MinecraftUiNode) {
        current.children.add(index, instance)
    }

    override fun insertTopDown(index: Int, instance: MinecraftUiNode) {
        // insertBottomUp is preferred
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

}