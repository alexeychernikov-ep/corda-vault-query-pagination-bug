package net.corda.samples.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object SimpleObjectSchema

@CordaSerializable
object SimpleObjectSchemaV1 : MappedSchema(
        schemaFamily = SimpleObjectSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSimpleObjectState::class.java)) {
    @Entity
    @Table(name = "SIMPLE_OBJECT")
    class PersistentSimpleObjectState(
            @Column(name = "ID")
            val id: Int
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(-1)
    }
}
