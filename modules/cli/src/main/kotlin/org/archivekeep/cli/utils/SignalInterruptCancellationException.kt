package org.archivekeep.cli.utils

import kotlinx.coroutines.CancellationException

class SignalInterruptCancellationException : CancellationException("Received SIGINT")
