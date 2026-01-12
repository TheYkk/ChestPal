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

import org.assertj.core.api.Assertions.assertThat
import org.bukkit.Location
import org.bukkit.Material
import org.junit.jupiter.api.Test
import tf.sou.mc.pal.domain.MaterialLocation
import tf.sou.mc.pal.domain.ReceiverChests

internal class LocationCacheTest {

    @Test
    fun `test remove location does not remove entire material entry`() {
        val loc1 = Location(null, 1.0, 2.0, 3.0)
        val loc2 = Location(null, 4.0, 5.0, 6.0)

        val receivers = ReceiverChests(
            listOf(
                MaterialLocation(Material.DIRT, listOf(loc1, loc2)),
            ),
        )

        val cache = LocationCache(receivers, emptyList())

        // Remove loc1
        val removed = cache.removeLocation(loc1)

        assertThat(removed).isTrue
        assertThat(cache.receiverLocationsFor(Material.DIRT)).containsExactly(loc2)
        assertThat(cache.isReceiverChestLocation(loc1)).isFalse
        assertThat(cache.isReceiverChestLocation(loc2)).isTrue
    }

    @Test
    fun `test remove last location removes material entry`() {
        val loc1 = Location(null, 1.0, 2.0, 3.0)
        val receivers = ReceiverChests(
            listOf(
                MaterialLocation(Material.DIRT, listOf(loc1)),
            ),
        )
        val cache = LocationCache(receivers, emptyList())

        cache.removeLocation(loc1)

        assertThat(cache.receiverLocationsFor(Material.DIRT)).isNull()
    }
}
