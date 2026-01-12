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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ExtensionsKtTest {
    private data class FakeStack(var amount: Int)

    private data class FakeTypedStack(val type: String, val amount: Int)

    enum class TestEnum {
        DIRT,
        DEEPSLATE_COPPER_ORE,
        GOLD_ORE
    }

    @ParameterizedTest
    @CsvSource(
        "0|0|0|0.0,0.0,0.0",
        "1.2|3.4|5.6|1.2,3.4,5.6",
        delimiter = '|'
    )
    fun `test location to vector string`(x: Double, y: Double, z: Double, expected: String) {
        val location = Location.deserialize(mapOf("x" to x, "y" to y, "z" to z))
        assertThat(location.toVectorString()).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "1,64,1",
        "64,64,1",
        "65,64,2",
        "128,64,2",
        "129,64,3"
    )
    fun `test stack partitioning`(amount: Int, maxStackSize: Int, listSize: Int) {
        val template = FakeStack(amount = 1)
        val stacks = amount.asStacks(
            maxStackSize = maxStackSize,
            template = template,
            clone = { it.copy() },
            setAmount = { stack, chunkSize -> stack.amount = chunkSize }
        )
        assertThat(stacks.size).isEqualTo(listSize)
    }

    @Test
    fun `asStacks clones stacks and does not mutate template`() {
        val template = FakeStack(amount = 1)
        val stacks = 129.asStacks(
            maxStackSize = 64,
            template = template,
            clone = { it.copy() },
            setAmount = { stack, chunkSize -> stack.amount = chunkSize }
        )

        assertThat(template.amount).isEqualTo(1)
        assertThat(stacks).hasSize(3)
        assertThat(stacks.map { it.amount }).containsExactly(64, 64, 1)
        assertThat(stacks[0]).isNotSameAs(stacks[1])
        assertThat(stacks[1]).isNotSameAs(stacks[2])
    }

    @Test
    fun `countAvailableSpace only counts empty slots and similar stacks`() {
        val item = FakeTypedStack(type = "DIRT", amount = 1)
        val contents = arrayOf<FakeTypedStack?>(
            null,
            FakeTypedStack(type = "DIRT", amount = 32),
            FakeTypedStack(type = "STONE", amount = 10)
        )

        val available = countAvailableSpaceFor(
            contents = contents,
            item = item,
            maxStackSize = 64,
            isSimilar = { stack, other -> stack.type == other.type },
            amount = { it.amount }
        )
        assertThat(available).isEqualTo(96)
    }

    @ParameterizedTest
    @CsvSource(
        "DIRT,Dirt",
        "DEEPSLATE_COPPER_ORE,Deepslate Copper Ore",
        "GOLD_ORE,Gold Ore"
    )
    fun `test enum to pretty string`(value: TestEnum, expected: String) {
        assertThat(value.toPrettyString()).isEqualTo(expected)
    }
}
