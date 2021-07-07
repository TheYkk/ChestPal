package tf.sou.mc.pal.domain

import org.bukkit.Material
import org.bukkit.event.player.PlayerInteractEvent
import tf.sou.mc.pal.ChestPal
import tf.sou.mc.pal.RECEIVER_MATERIAL
import tf.sou.mc.pal.SENDER_MATERIAL
import tf.sou.mc.pal.utils.asTextComponent
import tf.sou.mc.pal.utils.findItemFrame
import tf.sou.mc.pal.utils.reply
import tf.sou.mc.pal.utils.toVectorString

@Suppress("unused")
enum class HoeType(private val material: Material) : EventActor<PlayerInteractEvent> {
    DEBUG(Material.DIAMOND_HOE) {
        override fun act(event: PlayerInteractEvent, pal: ChestPal) {
            if (event.player.displayName().contains("0x1".asTextComponent())) {
                event.clickedBlock?.let { event.reply(it.location.toVectorString()) }
            }
        }
    },
    SENDER(SENDER_MATERIAL) {
        override fun act(event: PlayerInteractEvent, pal: ChestPal) {
            val chestLocation = event.clickedBlock?.location ?: return
            if (pal.database.isRegisteredChest(chestLocation)) {
                event.reply("This chest is already a registered!")
                return
            }
            event.player.sendMessage("Saving sender chest at location ${chestLocation.toVectorString()}!")
            pal.database.saveSenderLocation(chestLocation)
        }
    },
    RECEIVER(RECEIVER_MATERIAL) {
        override fun act(event: PlayerInteractEvent, pal: ChestPal) {
            val chestLocation = event.clickedBlock?.location ?: return
            if (pal.database.isRegisteredChest(chestLocation)) {
                // Either a receiver or sender chest.
                event.reply("This chest is already registered!")
                return
            }

            when (val result = chestLocation.findItemFrame()) {
                is ItemFrameResult.NoFrame -> event.reply("Could not find item frame")
                is ItemFrameResult.NoItem -> event.reply("Item frame does not have an item")
                is ItemFrameResult.Found -> {
                    val type = result.frame.item.type
                    pal.database.saveMaterialLocation(type, chestLocation)
                    event.reply("Saved new item chest for type $type!")
                }
            }
        }
    };

    companion object {
        fun find(material: Material) = values().find { it.material == material }
    }
}