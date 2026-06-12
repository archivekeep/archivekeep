package org.archivekeep.files

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.utils.loading.Loadable
import kotlin.time.Duration.Companion.seconds

suspend infix fun <T> StateFlow<Loadable<T>>.eventuallyShouldBeLoadedAndValueShouldBe(expectedValue: T) {
    eventually(2.seconds) {
        this.value.assertLoaded { it shouldBe expectedValue }
    }
}

suspend inline infix fun <T> StateFlow<Loadable<T>>.eventuallyShouldBeLoadedAndValueShould(crossinline matcher: (T) -> Unit) {
    eventually(2.seconds) {
        this.value.assertLoaded { it.should(matcher) }
    }
}
