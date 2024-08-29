package segerfast.mccompose

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import segerfast.mccompose.MinecraftCompose.ID
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

object Fonts {
    val DEFAULT: Font = Minecraft.getInstance().font
}

inline fun runForClient(block: () -> Unit) = runForDist(
    clientTarget = block,
    serverTarget = { }
)

// rootTag - Package name basically.
// tag - Default tag.
// extraTag - Additional tag.

class TaggedMessageLogger(private val instanceTagOrNull: String? = null) {
    private var logger: Logger = LogManager.getLogger(ID)
    private var dynamicTagOrNull: String? = null // Make atomic?

    fun d(msg: String) {
        logger.debug(formattedLogString(msg))
    }

    private fun formattedLogString(msg: String): String {
        val dynamicTag = dynamicTagOrNull?.let { "/$it" } ?: ""
        val fullTag = instanceTagOrNull?.let { "[$it$dynamicTag] " } ?: ""
        return "$fullTag$msg"
    }

    fun setTag(tag: String) {
        this.dynamicTagOrNull = tag
    }

    fun clearTag() {
        this.dynamicTagOrNull = null
    }
}

class FpsCounter(private val onFpsValue: (Int) -> Unit) {
    private var fpsCounter = 0
    private var lastTimestamp = System.currentTimeMillis()

    fun onFrame() {
        fpsCounter++
        val now = System.currentTimeMillis()
        if(now >= lastTimestamp + 1000) {
            lastTimestamp = now
            onFpsValue(fpsCounter)
            fpsCounter = 0
        }
    }
}
