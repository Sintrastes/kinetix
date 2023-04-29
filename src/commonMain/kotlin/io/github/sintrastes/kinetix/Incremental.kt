
package io.github.sintrastes.kinetix

/**
 * An incremental computation of type `T`.
 */
open class Incremental<T>(initialValue: T) {
    private var value = initialValue
    private val listeners = mutableSetOf<(T) -> Unit>()

    fun get(): T = value

    infix fun <R> map(fn: (T) -> R): Incremental<R> {
        val result = Incremental(fn(value))
        val listener: (T) -> Unit = { newValue ->
            result.set(fn(newValue))
        }
        this.listeners.add(listener)
        return result
    }

    infix fun <R> ap(other: Incremental<(T) -> R>): Incremental<R> {
        val result = Incremental(other.get()(value))
        val listener: (T) -> Unit = { newValue ->
            result.set(other.get()(newValue))
        }
        this.listeners.add(listener)
        other.listeners.add { newFn ->
            result.set(newFn(value))
        }
        return result
    }

    infix fun <R> flatMap(fn: (T) -> Incremental<R>): Incremental<R> {
        val result = fn(value)
        val listener: (T) -> Unit = { newValue ->
            result.set(fn(newValue).get())
        }
        this.listeners.add(listener)
        result.listeners.add { newValue ->
            this.listeners.forEach { it(get()) }
        }
        return result
    }

    fun set(newValue: T) {
        value = newValue
        listeners.forEach { it(newValue) }
    }
}

fun <A, B, C> map2(f: (A, B) -> C, fa: Incremental<A>, fb: Incremental<B>): Incremental<C> =
    fb.ap(fa.map(f.curried()))

fun <A, B, C> ((A, B) -> C).curried(): (A) -> (B) -> C = { a: A -> { b: B -> this(a, b) } }

tailrec fun <A> merge(ar: Array<Incremental<A>>, f: (A, A) -> A): Incremental<A> {
    return if (ar.size == 1) ar[0]
    else {
        val len = ar.size
        val lenPrime = len / 2 + len % 2
        val arPrime = Array(lenPrime) { i ->
            if (i * 2 + 1 >= len) ar[i * 2]
            else ar[i * 2].ap(ar[i * 2 + 1].map { b -> { a: A -> f(a, b) } })
        }
        merge(arPrime, f)
    }
}

fun <A> List<Incremental<A>>.merge(f: (A, A) -> A): Incremental<A> {
    return when (size) {
        1 -> first()
        else -> {
            val mid = size / 2
            val left = subList(0, mid).merge(f)
            val right = subList(mid, size).merge(f)
            map2(f, left, right)
        }
    }
}


