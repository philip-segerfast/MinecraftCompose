package segerfast.mccompose.screen

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.sync.Mutex
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import segerfast.mccompose.Fonts
import segerfast.mccompose.FpsCounter
import segerfast.mccompose.MinecraftCompose
import segerfast.mccompose.TaggedMessageLogger

/**
 * TODO - support if screen is attached/detached.
 * */
// Note: The code here is inspired and partially copied from the mosaic library (https://github.com/JakeWharton/mosaic)
class ComposeScreen(
    title: Component,
    private val content: @Composable () -> Unit
): Screen(title) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var hasFrameWaiters = false
    private val clock = BroadcastFrameClock {
        hasFrameWaiters = true
    }
    private val job = Job(scope.coroutineContext[Job])
    private val composeContext = scope.coroutineContext + clock + job
    private val rootNode: MinecraftUiNode = object : MinecraftUiNode() {}
    private val recomposer = Recomposer(composeContext)
    private var displaySignal: CompletableDeferred<Unit>? = null
    private val applier = MinecraftUiApplier(rootNode) {
        displaySignal?.complete(Unit)
        hasFrameWaiters = false
    }
    private val composition: Composition = Composition(applier, recomposer)
    private lateinit var snapshotObserverHandle: ObserverHandle
    private val snapshotObserverMutex = Mutex()
    private val logger = TaggedMessageLogger("ComposeScreen")
    private val fpsCounter = FpsCounter { /* logger.d("FPS: $it") */ }
    private var removed = false

    override fun init() {
        logger.d("ComposeScreen.init()")
        super.init()

        logger.d("Setting up composition...")
        displaySignal?.complete(Unit)
        hasFrameWaiters = false

        handleRecompositions()
        registerSnapshotObserver()
        composition.setContent(content)
    }

    private fun handleRecompositions() {
        // Start undispatched to ensure we can use suspending things inside the content.
        scope.launch(start = UNDISPATCHED, context = composeContext) {
            logger.d("Launching job to handle recomposer.runRecomposeAndApplyChanges()")
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    private fun registerSnapshotObserver() {
        var snapshotNotificationsPending = false
        if(removed) {
            println("ComposeScreen was removed before SnapshotObserver listener was registered.")
            return
        }
        snapshotObserverHandle = Snapshot.registerGlobalWriteObserver {
            if (!snapshotNotificationsPending) {
                snapshotNotificationsPending = true
                scope.launch {
                    snapshotNotificationsPending = false
                    Snapshot.sendApplyNotifications()
                }
            }
        }
    }

    override fun tick() {
        super.tick()
    }

    override fun removed() {
        removed = true
        logger.d("ComposeScreen.removed() - DESTROYING COMPOSITION")
        clock.cancel()
        snapshotObserverHandle.dispose()

        runBlocking {
            // Ensure the final state modification is discovered. We need to ensure that the coroutine
            // which is running the recomposition loop wakes up, notices the changes, and waits for the
            // next frame. If you are using snapshots this only requires a single yield. If you are not
            // then it requires two yields. THIS IS NOT GREAT! But at least it's implementation detail...
            // TODO https://issuetracker.google.com/issues/169425431
            yield()
            yield()
            Snapshot.sendApplyNotifications()
            yield()
            yield()

            if (hasFrameWaiters) {
                CompletableDeferred<Unit>().also {
                    displaySignal = it
                    it.await()
                }
            }

            job.cancel()
            composition.dispose()
        }

        logger.d("Canceling scope.")
        scope.cancel()

        super.removed()
    }

    override fun added() {
        logger.d("ComposeScreen.added()")
        super.added()
    }

    override fun resize(minecraft: Minecraft, width: Int, height: Int) {
        logger.d("ComposeScreen.resize()")
        super.resize(minecraft, width, height)
    }

    override fun onClose() {
        logger.d("ComposeScreen.onClose()")
        super.onClose()
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        clock.sendFrame(0L)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        // Draw screen graphics itself and child nodes.
        rootNode.draw(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        fpsCounter.onFrame()
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

internal class MinecraftUiApplier(
    root: MinecraftUiNode,
    private val onEndChanges: () -> Unit = {},
) : AbstractApplier<MinecraftUiNode>(root) {

    private val logger: Logger = LogManager.getLogger(MinecraftCompose.ID)

    override fun onClear() {
        logger.debug("onClear")
    }

    override fun onEndChanges() {
        onEndChanges.invoke()
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