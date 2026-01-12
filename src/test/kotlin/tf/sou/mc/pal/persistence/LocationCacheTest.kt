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
import org.junit.jupiter.api.Test

internal class LocationCacheTest {
    private fun loc(x: Double, y: Double, z: Double): Location {
        return Location.deserialize(mapOf("x" to x, "y" to y, "z" to z))
    }

    @Test
    fun `removeFromSetsMap removes only the specified value and prunes empty sets`() {
        val loc1 = loc(0.0, 0.0, 0.0)
        val loc2 = loc(1.0, 0.0, 0.0)
        val loc3 = loc(2.0, 0.0, 0.0)

        val map = mutableMapOf(
            "DIRT" to mutableSetOf(loc1, loc2),
            "STONE" to mutableSetOf(loc3)
        )

        assertThat(removeFromSetsMap(map, loc1)).isTrue
        assertThat(map["DIRT"]).containsExactlyInAnyOrder(loc2)
        assertThat(map["STONE"]).containsExactlyInAnyOrder(loc3)

        assertThat(removeFromSetsMap(map, loc2)).isTrue
        assertThat(map).doesNotContainKey("DIRT")
        assertThat(map["STONE"]).containsExactlyInAnyOrder(loc3)

        assertThat(removeFromSetsMap(map, loc1)).isFalse
    }
}

