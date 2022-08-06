package com.mgsoftware.billing.utility

fun <E> Iterable<E>.intersect(predicate: (element: E) -> Boolean): Set<E> {
    val set = toMutableSet()
    set.retainAll { predicate(it) }
    return set
}