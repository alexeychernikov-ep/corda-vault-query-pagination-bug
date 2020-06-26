package net.corda.samples.states

import net.corda.samples.contracts.SimpleObjectContract
import net.corda.samples.schema.SimpleObjectSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.QueryableState
import java.lang.IllegalArgumentException

@BelongsToContract(SimpleObjectContract::class)
data class SimpleObjectState(val simpleObjectDetail: SimpleObjectDetail, override val participants: List<AbstractParty>) :
    ContractState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema) =
        when (schema) {
            is SimpleObjectSchemaV1 -> SimpleObjectSchemaV1.PersistentSimpleObjectState(
                simpleObjectDetail.id
            )
            else -> throw IllegalArgumentException("Unsupported Schema")
        }

    override fun supportedSchemas() = listOf(SimpleObjectSchemaV1)
}