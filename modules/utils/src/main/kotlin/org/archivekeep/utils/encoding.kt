package org.archivekeep.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
fun (String).fromHexToBase64(): String = this.hexToByteArray().let { Base64.encode(it) }

@OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
fun (String).fromBase64ToHex(): String = Base64.decode(this).toHexString()
