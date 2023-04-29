
import io.github.sintrastes.kinetix.Incremental
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlin.math.abs

class RouteSpec : StringSpec({
    "updating an incremental in an edge should update the route's incremental attributes" {
        checkAll(legsArb) { legs ->
            val route = Route(legs)
            val edgeToUpdate = legs.first().edges.first()
            val newDistance = edgeToUpdate.distance.get() + 1.0
            edgeToUpdate.distance.set(newDistance)
            val expectedDistance = legs.sumOf { leg ->
                leg.edges.sumOf { edge ->
                    if (edge == edgeToUpdate) newDistance
                    else edge.distance.get()
                }
            }
            val expectedAverageElevation = legs.flatMap { it.edges }.map { edge ->
                if (edge == edgeToUpdate) edge.elevation.get() else edge.elevation.get()
            }.average()

            abs(route.cumulativeDistance.get() - expectedDistance) shouldBeLessThan  0.0001
            abs(route.averageElevation.get() - expectedAverageElevation) shouldBeLessThan 0.0001
        }
    }
})

val edgeArb = Arb.bind(Arb.numericDouble(0.0, 100.0), Arb.numericDouble(0.0, 100.0)) { distance, elevation ->
    Edge(Incremental(distance), Incremental(elevation))
}

val legArb = Arb.list(edgeArb, IntRange(1, 15))

val legsArb = Arb.list(legArb, IntRange(1, 15)).filter { it.isNotEmpty() }.map { it.map { Leg(it) } }
