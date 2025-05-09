package org.archivekeep.utils.flows

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.archivekeep.utils.collections.ListItemChangeLogger
import org.archivekeep.utils.loading.Loadable

fun <T, C : Collection<T>> (Flow<C>).logCollectionFlow(name: String): Flow<C> =
    flow {
        val logger = ListItemChangeLogger<T>(name)

        collect { latestItems ->
            logger.onNewItems(latestItems)
            emit(latestItems)
        }
    }

fun <T, C : Collection<T>> (Flow<Loadable<C>>).logCollectionLoadableFlow(name: String): Flow<Loadable<C>> =
    flow {
        val logger = ListItemChangeLogger<T>(name)

        collect { latestItemsLoadable ->
            if (latestItemsLoadable is Loadable.Loaded) {
                logger.onNewItems(latestItemsLoadable.value.toSet())
            }

            emit(latestItemsLoadable)
        }
    }
