package net.corda.samples.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class SimpleObjectDetail(
    val id: Int
)