package segerfast.mccompose.screen.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component
import segerfast.mccompose.Fonts
import segerfast.mccompose.TaggedMessageLogger
import segerfast.mccompose.screen.MinecraftUiApplier
import segerfast.mccompose.screen.TextNode

@Composable
fun Text(
    text: String,
    font: Font = Fonts.DEFAULT
) {
    val logger = remember { TaggedMessageLogger("TextNodeComposable") }
    ComposeNode<TextNode, MinecraftUiApplier>(
        factory = {
            logger.d("factory()")
            val stringWidget = StringWidget(Component.literal(text), Fonts.DEFAULT)
            TextNode(stringWidget)
        },
        update = {
            update(text) { text ->
                logger.d("Updating text!")
                stringWidget.message = Component.literal(text)
            }
            update(font) { font ->
                logger.d("Updating font!")
                stringWidget = StringWidget(stringWidget.message.copy(), font)
            }
        }
    )
}