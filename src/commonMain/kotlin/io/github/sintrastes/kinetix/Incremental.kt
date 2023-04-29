
package io.github.sintrastes.kinetix

/**
 * An incremental computation of type `T`.
 */
open class Incremental<T>(initialValue: T) {
    private var value = initialValue
    private val listeners = mutableSetOf<(T) -> Unit>()

    /** Get the current value of an incremental computation. */
    fun get(): T = value

    /**
     * Construct a new incremental value from an existing incremental
     *  value by applying a function.
     *
     * The result will be an incremental value whose value
     *  will always be `fn` applied to the current value
     *  of the incremental being mapped.
     **/
    infix fun <R> map(fn: (T) -> R): Incremental<R> {
        val result = Incremental(fn(value))
        val listener: (T) -> Unit = { newValue ->
            result.set(fn(newValue))
        }
        this.listeners.add(listener)
        return result
    }

    /**
     * Takes an `Incremental` value, and an `Incremental` function,
     *  and creates a new `Incremental` that applies the function to
     *  the value such that whenever either the original value or
     *  function update, the resulting incremental updates accordingly.
     */
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

    /**
     * Allows chaining an incremental value with an incremental computation
     *  (i.e. a function returning a new incremental value) to produce a new
     *  incremental value.
     *
     * Allows for the construction of complex incremental computations with
     *  multiple dependent steps.
     */
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

    /** Update the value of a mutable `Incremental`. */
    fun set(newValue: T) {
        value = newValue
        listeners.forEach { it(newValue) }
    }
}

/** Combines two incremental values via a binary function. */
fun <A, B, C> map2(f: (A, B) -> C, fa: Incremental<A>, fb: Incremental<B>): Incremental<C> =
    fb.ap(fa.map(f.curried()))

private fun <A, B, C> ((A, B) -> C).curried(): (A) -> (B) -> C = { a: A -> { b: B -> this(a, b) } }

/**
 * Merges an array of incremental values using a binary tree
 *  update strategy so that single updates to the list
 *  only require O(log(n)) steps rather than O(n).
 **/
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

/**
 * Merges a list of incremental values using a binary tree
 *  update strategy so that single updates to the list
 *  only require O(log(n)) steps rather than O(n).
 */
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


