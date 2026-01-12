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
package tf.sou.mc.pal.utils

import org.assertj.core.api.Assertions.assertThat
import org.bukkit.Location
import org.bukkit.Material
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ExtensionsKtTest {

    @ParameterizedTest
    @CsvSource(
        "0|0|0|0.0,0.0,0.0",
        "1.2|3.4|5.6|1.2,3.4,5.6",
        delimiter = '|',
    )
    fun `test location to vector string`(x: Double, y: Double, z: Double, expected: String) {
        val location = Location(null, x, y, z)
        assertThat(location.toVectorString()).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "DIRT,Dirt",
        "DEEPSLATE_COPPER_ORE,Deepslate Copper Ore",
        "GOLD_ORE,Gold Ore",
    )
    fun `test enum to pretty string`(material: Material, expected: String) {
        assertThat(material.toPrettyString()).isEqualTo(expected)
    }
}
