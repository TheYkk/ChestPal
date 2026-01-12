/*
 * This file is part of ChestPal.
 *
 * ChestPal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ChestPal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChestPal.  If not, see <https://www.gnu.org/licenses/>.
 */
package tf.sou.mc.pal.listeners

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.PluginNameConversationPrefix
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import tf.sou.mc.pal.ChestPal
import tf.sou.mc.pal.domain.HoeType
import tf.sou.mc.pal.domain.ItemFrameResult
import tf.sou.mc.pal.prompts.ChestBreakPrompt
import tf.sou.mc.pal.utils.asItemStacks
import tf.sou.mc.pal.utils.countAvailableSpace
import tf.sou.mc.pal.utils.findBadItems
import tf.sou.mc.pal.utils.findItemFrame
import tf.sou.mc.pal.utils.redMessage
import tf.sou.mc.pal.utils.resolveContainer
import tf.sou.mc.pal.utils.toChestInventoryProxy
import tf.sou.mc.pal.utils.toPrettyString

/**
 * Listeners for chest based events.
 */
class ChestListener(private val pal: ChestPal) : Listener {
    private val conversationFactory = ConversationFactory(pal)
        .withModality(true)
        .withFirstPrompt(ChestBreakPrompt())
        .withTimeout(7)
        .withPrefix(PluginNameConversationPrefix(pal))
        .thatExcludesNonPlayersWithMessage("No console users!")

    @EventHandler
    fun onInventoryCloseEvent(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val location = inventory.location ?: return

        val chestProxy = inventory.toChestInventoryProxy()
        if (!chestProxy.isRegistered(database = pal.database)) {
            return
        }

        val eventChest = location.resolveContainer() ?: return
        val eventInventory = eventChest.inventory.toChestInventoryProxy()
        if (eventInventory.isReceiver(database = pal.database)) {
            handleClosedReceiverChest(location, inventory, event)
            return
        }

        // Group items by material to optimize database lookups and avoid redundant processing.
        val chestItems = inventory.contents
            .filterNotNull()
            .groupBy { it.type }

        for ((materialType, items) in chestItems) {
            val receiverChests = pal.database.receiverLocationsFor(materialType)
            if (receiverChests.isEmpty()) {
                continue
            }

            var totalTransportAmount = items.sumOf { it.amount }
            val originalItem = items.first() // Use as template for metadata

            val iterator = receiverChests.iterator()
            while (totalTransportAmount > 0 && iterator.hasNext()) {
                val chest =
                    iterator.next().resolveContainer() ?: continue

                val available = chest.inventory.countAvailableSpace(materialType)
                val allowedToAdd = available.coerceAtMost(totalTransportAmount)
                if (allowedToAdd > 0) {
                    var actuallyAdded = 0
                    for (stack in allowedToAdd.asItemStacks(originalItem)) {
                        val leftover = chest.inventory.addItem(stack)
                        val notAdded = leftover.values.sumOf { it.amount }
                        actuallyAdded += (stack.amount - notAdded)
                    }
                    totalTransportAmount -= actuallyAdded
                }
            }

            // Safely remove items from the source inventory.
            // Instead of inventory.remove(ItemStack), we remove by type and amount.
            var amountToRemove = items.sumOf { it.amount } - totalTransportAmount
            if (amountToRemove > 0) {
                val removedCount = amountToRemove
                for (slot in 0 until inventory.size) {
                    val item = inventory.getItem(slot) ?: continue
                    if (item.type == materialType) {
                        if (item.amount <= amountToRemove) {
                            amountToRemove -= item.amount
                            inventory.setItem(slot, null)
                        } else {
                            item.amount -= amountToRemove
                            amountToRemove = 0
                        }
                    }
                    if (amountToRemove <= 0) break
                }
                event.player.sendMessage("Moved ${materialType.toPrettyString()} (x$removedCount)")
            }
        }
    }

    private fun handleClosedReceiverChest(
        location: Location,
        inventory: Inventory,
        event: InventoryCloseEvent,
    ) {
        val item = location.findItemFrame() as? ItemFrameResult.Found ?: return
        val allowedItem = item.frame.item.type
        val badItems = inventory.findBadItems(allowedItem)
        if (badItems.isEmpty()) {
            return
        }
        // Clean up.
        badItems.forEach {
            inventory.remove(it)
            // Handle full player inventory by dropping items at their feet.
            val leftover = event.player.inventory.addItem(it)
            leftover.values.forEach { remaining ->
                event.player.world.dropItemNaturally(event.player.location, remaining)
            }
        }
        event.player.sendMessage("This receiver chest only takes ${allowedItem.toPrettyString()}!")
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only handle the main-hand interaction to avoid firing twice (offhand + main hand).
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val clickedBlock = event.clickedBlock ?: return
        if (clickedBlock.location.resolveContainer() == null) return

        val itemInHand = event.player.inventory.itemInMainHand
        val hoeType = HoeType.find(itemInHand) ?: return
        hoeType.act(event, pal)
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        val container = event.block.location.resolveContainer() ?: return
        val proxy = container.inventory.toChestInventoryProxy()
        if (proxy.isRegistered(pal.database)) {
            val player = event.player
            if (!player.hasPermission("chestpal.remove_chest")) {
                event.isCancelled = true
                player.redMessage("You are not allowed to break registered chests!")
                return
            }
            conversationFactory
                .withInitialSessionData(mapOf("block" to event.block))
                .buildConversation(event.player)
                .begin()
            // Cancel the event because we delegated to the conversation handler.
            event.isCancelled = true
        }
    }
}
