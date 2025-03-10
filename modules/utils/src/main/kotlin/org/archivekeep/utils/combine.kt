package org.archivekeep.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

inline fun <reified T, R> safeCombine(
    flows: Collection<Flow<T>>,
    crossinline transform: suspend (Array<T>) -> R,
): Flow<R> =
    if (flows.isEmpty()) {
        flow { emit(transform(emptyArray<T>())) }
    } else {
        combine(flows, transform)
    }

inline fun <reified T, R> combineToList(
    flows: Collection<Flow<T>>,
    crossinline transform: suspend (Array<T>) -> List<R>,
): Flow<List<R>> =
    if (flows.isEmpty()) {
        flow { emit(emptyList()) }
    } else {
        combine(flows, transform)
    }

inline fun <reified T> combineToList(flows: Collection<Flow<T>>): Flow<List<T>> =
    combineToList(flows) { result ->
        result.toList()
    }

inline fun <reified T> combineToFlatMapList(flows: Collection<Flow<Collection<T>>>): Flow<List<T>> =
    combineToList(flows) { result ->
        result.flatMap { it }
    }
