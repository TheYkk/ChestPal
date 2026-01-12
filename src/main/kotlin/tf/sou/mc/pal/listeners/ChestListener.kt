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
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.PluginNameConversationPrefix
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import tf.sou.mc.pal.ChestPal
import tf.sou.mc.pal.domain.HoeType
import tf.sou.mc.pal.domain.ItemFrameResult
import tf.sou.mc.pal.prompts.ChestBreakPrompt
import tf.sou.mc.pal.utils.isSupportedStorage
import tf.sou.mc.pal.utils.toChestInventoryProxy
import tf.sou.mc.pal.utils.resolveContainer
import tf.sou.mc.pal.utils.toPrettyString
import tf.sou.mc.pal.utils.findItemFrame
import tf.sou.mc.pal.utils.redMessage

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

        val container = location.resolveContainer() ?: return
        if (!container.isSupportedStorage()) {
            return
        }

        val chestProxy = inventory.toChestInventoryProxy()
        if (!chestProxy.isRegistered(database = pal.database)) {
            return
        }

        if (chestProxy.isReceiver(database = pal.database)) {
            handleClosedReceiverChest(location, inventory, event)
            return
        }

        val invalidReceiverLocations = mutableSetOf<Location>()
        for (slot in 0 until inventory.size) {
            val stack = inventory.getItem(slot) ?: continue

            val receiverChests = pal.database.receiverLocationsFor(stack.type)
            if (receiverChests.isEmpty()) {
                continue
            }

            var remainingAmount = stack.amount
            val iterator = receiverChests.iterator()
            while (remainingAmount > 0 && iterator.hasNext()) {
                val receiverLocation = iterator.next()
                val receiverChest = receiverLocation.resolveContainer()
                if (receiverChest == null || !receiverChest.isSupportedStorage()) {
                    invalidReceiverLocations.add(receiverLocation)
                    continue
                }

                val toAdd = stack.clone()
                toAdd.amount = remainingAmount
                val leftovers = receiverChest.inventory.addItem(toAdd)
                remainingAmount = leftovers.values.sumOf { it.amount }
            }

            val moved = stack.amount - remainingAmount
            if (moved <= 0) {
                continue
            }

            if (remainingAmount <= 0) {
                inventory.setItem(slot, null)
            } else {
                val remaining = stack.clone()
                remaining.amount = remainingAmount
                inventory.setItem(slot, remaining)
            }

            event.player.sendMessage("Moved ${stack.type.toPrettyString()} (x$moved)")
        }

        invalidReceiverLocations.forEach { pal.database.removeLocation(it) }
    }

    private fun handleClosedReceiverChest(
        location: Location,
        inventory: Inventory,
        event: InventoryCloseEvent
    ) {
        // This could be a cache lookup in the future.
        val item = location.findItemFrame() as? ItemFrameResult.Found ?: return
        val allowedItem = item.frame.item.type

        var removedAny = false
        for (slot in 0 until inventory.size) {
            val stack = inventory.getItem(slot) ?: continue
            if (stack.type == allowedItem) {
                continue
            }

            inventory.setItem(slot, null)
            removedAny = true

            val leftovers = event.player.inventory.addItem(stack)
            leftovers.values.forEach { leftover ->
                event.player.world.dropItemNaturally(event.player.location, leftover)
            }
        }

        if (removedAny) {
            event.player.sendMessage("This receiver chest only takes ${allowedItem.toPrettyString()}!")
        }
    }

    @EventHandler
    fun onPlayerInteractEntityEvent(event: PlayerInteractEvent) {
        val hoeType = event.item?.let { HoeType.find(it.type) } ?: return
        val container = event.clickedBlock?.location?.resolveContainer() ?: return
        if (!container.isSupportedStorage()) return
        hoeType.act(event, pal)
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        val container = event.block.location.resolveContainer() ?: return
        if (!container.isSupportedStorage()) return

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
