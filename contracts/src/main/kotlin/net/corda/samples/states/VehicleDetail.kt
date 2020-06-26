package net.corda.samples.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class VehicleDetail(
    val registrationNumber: String,
    val chasisNumber: String
)