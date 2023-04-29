
import io.github.sintrastes.kinetix.Incremental
import io.github.sintrastes.kinetix.map2
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IncrementalTest: StringSpec({
    "test incremental sum" {
        val data = listOf(1, 2, 3, 4, 5)
        val incrementalSum = data.map { Incremental(it) }.reduce { a, b ->
            a.flatMap { aVal ->
                b.map { bVal -> aVal + bVal }
            }
        }

        incrementalSum.get() shouldBe 15
    }

    "test incremental computation" {
        // Create an incremental value with an initial value of 5
        val x = Incremental(5)

        // Create an incremental computation that doubles the value of x
        val doubleX = x.map { it * 2 }

        // Check that the value of doubleX is initially 10
        doubleX.get() shouldBe 10

        // Change the value of x to 10
        x.set(10)

        // Check that the value of doubleX has been updated to 20
        doubleX.get() shouldBe 20

        // Create an incremental computation that triples the value of x
        val tripleX = x.map { it * 3 }

        // Check that the value of tripleX is initially 30
        tripleX.get() shouldBe 30

        // Change the value of x to 7
        x.set(7)

        // Check that both values have been updated
        doubleX.get() shouldBe 14
        tripleX.get() shouldBe 21

        // Force an update of tripleX
        tripleX.get()

        // Check that the value of tripleX has been updated to 21
        tripleX.get() shouldBe 21

        // Create an incremental computation that adds the values of doubleX and tripleX
        val sum = map2(Int::plus, doubleX, tripleX)

        // Check that the value of sum is initially 35
        sum.get() shouldBe 35

        // Change the value of x to 8
        x.set(8)

        // Check that the value of doubleX has been updated to 16, tripleX to 24, and sum to 40
        doubleX.get() shouldBe 16
        tripleX.get() shouldBe 24
        sum.get() shouldBe 40

        // Change the value of x to 9
        x.set(9)

        // Check that the value of doubleX has been updated to 18, tripleX to 27, and sum to 45
        doubleX.get() shouldBe 18
        tripleX.get() shouldBe 27
        sum.get() shouldBe 45
    }

    "multiple chained map operations update properly" {
        val inc1 = Incremental(10)
        val inc2 = inc1.map { it * 2 }
        val inc3 = inc2.map { it.toString() }

        inc1.set(20)

        inc3.get() shouldBe "40"
    }
})
