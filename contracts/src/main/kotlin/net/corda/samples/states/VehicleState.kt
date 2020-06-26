package net.corda.samples.states

import net.corda.samples.contracts.VehicleContract
import net.corda.samples.schema.VehicleSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.QueryableState
import java.lang.IllegalArgumentException


// *********
// * State *
// *********
@BelongsToContract(VehicleContract::class)
data class VehicleState(val vehicleDetail: VehicleDetail, override val participants: List<AbstractParty>) :
    ContractState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema) =
        when (schema) {
            is VehicleSchemaV1 -> VehicleSchemaV1.PersistentVehicleState(
                vehicleDetail.registrationNumber,
                vehicleDetail.chasisNumber
            )
            else -> throw IllegalArgumentException("Unsupported Schema")
        }

    override fun supportedSchemas() = listOf(VehicleSchemaV1)
}