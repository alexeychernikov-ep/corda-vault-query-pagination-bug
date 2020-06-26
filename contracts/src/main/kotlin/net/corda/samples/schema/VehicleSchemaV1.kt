package net.corda.samples.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


/**
 * The family of schemas for IOUState.
 */
object VehicleSchema

/**
 * An IOUState schema.
 */
@CordaSerializable
object VehicleSchemaV1 : MappedSchema(
        schemaFamily = VehicleSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentVehicleState::class.java)) {
    @Entity
    @Table(name = "VEHICLE_DETAIL")
    class PersistentVehicleState(
            @Column(name = "registrationNumber")
            var registrationNumber: String,

            @Column(name = "chasisNumber")
            var chasisNumber: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("R-1", "C-1")
    }
}
