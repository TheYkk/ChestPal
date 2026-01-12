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
package tf.sou.mc.pal.persistence

import org.bukkit.Location
import org.bukkit.Material
import tf.sou.mc.pal.domain.MaterialLocation
import tf.sou.mc.pal.domain.ReceiverChests
import java.io.File

/**
 * Cache class to facilitate interactions between the json layer and [Location] objects.
 */
class LocationCache(receivers: ReceiverChests, senderLocations: List<Location>) {
    private val receiverChests = receivers
        .data.associate { it.material to it.receivers.toMutableSet() }.toMutableMap()
    internal val senderLocations = senderLocations.toMutableSet()

    // Use a Set for O(1) lookup instead of a List.
    private var cachedChestLocations = receiverChests.values.flatten().toSet()

    internal fun receiverLocationsFor(material: Material): Set<Location>? = receiverChests[material]

    internal fun isSenderChestLocation(location: Location?): Boolean = location in senderLocations

    internal fun isReceiverChestLocation(location: Location?): Boolean =
        location in cachedChestLocations

    internal fun isRegisteredChestLocation(location: Location?): Boolean =
        isSenderChestLocation(location) || location in cachedChestLocations

    internal fun chestLocationsToReceiverChests(): ReceiverChests {
        val materialChests = receiverChests.map { MaterialLocation(it.key, it.value.toList()) }
        return ReceiverChests(materialChests)
    }

    internal fun removeLocation(location: Location): Boolean {
        if (senderLocations.remove(location)) {
            return true
        }
        // Correctly remove the location from the sets within the map,
        // instead of removing the entire map entry.
        var removed = false
        receiverChests.values.forEach {
            if (it.remove(location)) {
                removed = true
            }
        }

        if (removed) {
            // Clean up empty entries.
            receiverChests.entries.removeIf { it.value.isEmpty() }
            cachedChestLocations = receiverChests.values.flatten().toSet()
            return true
        }
        return false
    }

    internal fun addReceiverLocation(material: Material, location: Location): Boolean {
        val result = receiverChests.computeIfAbsent(material) { mutableSetOf() }.add(location)
        cachedChestLocations = receiverChests.values.flatten().toSet()
        return result
    }

    internal fun addSenderLocation(location: Location): Boolean = senderLocations.add(location)

    companion object {
        internal fun fromFiles(
            receiverLocationFile: File,
            senderLocationFile: File,
            locationTransformer: (String) -> List<Location>,
            receiverTransformer: (String) -> ReceiverChests,
        ): LocationCache {
            val receivers = receiverLocationFile
                .takeIf { it.exists() }?.let { receiverTransformer(it.readText()) }
                ?: ReceiverChests(emptyList())

            val senders = senderLocationFile
                .takeIf { it.exists() }?.let { locationTransformer(it.readText()) } ?: emptyList()

            return LocationCache(receivers, senders)
        }
    }
}
