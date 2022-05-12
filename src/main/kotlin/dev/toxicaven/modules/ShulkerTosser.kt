package dev.toxicaven.modules

import dev.toxicaven.ShulkerTosserPlugin
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.mixin.extension.syncCurrentPlayItem
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.items.firstByStack
import com.lambda.client.util.items.inventorySlots
import com.lambda.client.util.items.throwAllInSlot
import com.lambda.client.util.threads.safeListener
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.NonNullList
import net.minecraftforge.fml.common.gameevent.TickEvent


/**
 * This is a module. First set properties then settings then add listener.
 * **/
internal object ShulkerTosser : PluginModule(
    name = "ShulkerTosser",
    category = Category.MISC,
    description = "Yeets empty shulkerboxes",
    pluginMain = ShulkerTosserPlugin
) {
    private val blankList: NonNullList<ItemStack> = NonNullList.withSize(27, ItemStack.EMPTY)
    private val timer = TickTimer(TimeUnit.TICKS)
    private val delay by setting("Delay Ticks", 1, 0..20, 1)

    init {
        safeListener<TickEvent.ClientTickEvent> {
            if (it.phase != TickEvent.Phase.START) return@safeListener
            if (!timer.tick(delay.toLong())) return@safeListener

            doEjections()

            playerController.syncCurrentPlayItem()
        }
    }

    private fun SafeClientEvent.doEjections() {
        getEjections()?.let {
            throwAllInSlot(it)
        }
    }

    private fun SafeClientEvent.getEjections(): Slot? {
        return player.inventorySlots.firstByStack {
            !it.isEmpty && checkItem(it)
        }
    }

    private fun checkItem(itemStack: ItemStack): Boolean {
        if (itemStack.item is ItemShulkerBox) {
            return !isShulkerEmpty(itemStack)
        }
        return false
    }

    private fun isShulkerEmpty(itemStack: ItemStack): Boolean {
        val tag: NBTTagCompound? = itemStack.item.getNBTShareTag(itemStack)
        if (tag != null) {
            if (tag.hasKey("BlockEntityTag", 10)) {
                val blockEntityTag = tag.getCompoundTag("BlockEntityTag")
                if (blockEntityTag.hasKey("Items", 9)) {
                    val itemList = NonNullList.withSize(27, ItemStack.EMPTY)
                    ItemStackHelper.loadAllItems(tag, itemList)
                    if (itemList == blankList) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
