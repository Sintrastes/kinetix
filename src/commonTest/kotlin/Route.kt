import io.github.sintrastes.kinetix.Incremental
import io.github.sintrastes.kinetix.merge

// An example implementation of incremental route computations using kinetix.
data class Edge(
    val distance: Incremental<Double>,
    val elevation: Incremental<Double>
)

data class Leg(val edges: List<Edge>) {
    val distance: Incremental<Double> = edges.map { it.distance }
        .merge { a, b -> a + b }
    val averageElevation: Incremental<Double> = edges.map { it.elevation }
        .merge { a, b -> a + b }
        .map { it / edges.size }
}

data class Route(val legs: List<Leg>) {
    val legDistance: List<Incremental<Double>> = legs.mapIndexed { index, leg ->
        legs.subList(0, index + 1).flatMap { it.edges }.map { it.distance }
            .merge { a, b -> a + b }
    }

    val cumulativeDistance: Incremental<Double> = legDistance.last()

    val averageElevation: Incremental<Double> = legs.flatMap { it.edges }.map { it.elevation }
        .merge { a, b -> a + b }
        .map { it / legs.flatMap { it.edges }.size }
}