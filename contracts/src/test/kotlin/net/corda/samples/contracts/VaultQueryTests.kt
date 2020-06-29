package net.corda.samples.contracts

import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultQueryException
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.schema.SimpleObjectSchemaV1
import net.corda.samples.states.SimpleObjectDetail
import net.corda.samples.states.SimpleObjectState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.internal.cordappsForPackages
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

sealed class QueryFramingOptions {
    object OROperatorWithPagination : QueryFramingOptions()
    object OROperatorWithoutPagination : QueryFramingOptions()
    object WithoutOROperator : QueryFramingOptions()
}

class VaultQueryTests {
    companion object {
        private val fooCorpX500Name = CordaX500Name("FooCorp", "New York", "US")
        private val mockNetwork = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = cordappsForPackages(
                    listOf(
                        "net.corda.samples"
                    )
                )
            )
        )
        private val fooCorpNode = mockNetwork.createPartyNode(fooCorpX500Name)
        private val fooCorp = fooCorpNode.info.legalIdentitiesAndCerts.first()
        private val serviceHubHandle = fooCorpNode.services
        private val notaryNode = mockNetwork.defaultNotaryNode
        private val createdStateRefs = mutableListOf<StateRef>()
        private const val numObjectsInLedger = DEFAULT_PAGE_SIZE + 1

        @BeforeClass
        @JvmStatic
        fun setup() {
            repeat(numObjectsInLedger) { index ->
                createdStateRefs.add(addSimpleObjectToLedger(SimpleObjectDetail(index)))
            }
        }

        private fun addSimpleObjectToLedger(simpleObject: SimpleObjectDetail): StateRef {
            val tx = TransactionBuilder(notaryNode.info.legalIdentities.first())
            tx.addCommand(
                SimpleObjectContract.Commands.AddSimpleObject(), fooCorp.owningKey
            )
            tx.addOutputState(
                SimpleObjectState(simpleObject, listOf(fooCorp.party))
            )
            tx.verify(serviceHubHandle)
            val stx = serviceHubHandle.signInitialTransaction(tx)
            serviceHubHandle.recordTransactions(listOf(stx))
            return StateRef(stx.id, 0)
        }
    }

    private fun getSimpleObjectMatchingCondition(
        id: Int,
        stateRef: StateRef,
        queryFramingOptions: QueryFramingOptions
    ): List<StateAndRef<SimpleObjectState>> {
        val queryToCheckSimpleObjectId = builder {
            val conditionToCheckRegistrationNumber =
                SimpleObjectSchemaV1.PersistentSimpleObjectState::id
                    .equal(id)
            QueryCriteria.VaultCustomQueryCriteria(conditionToCheckRegistrationNumber, Vault.StateStatus.UNCONSUMED)
        }

        val queryToCheckStateRef =
            QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED, stateRefs = listOf(stateRef))

        val result = when (queryFramingOptions) {
            is QueryFramingOptions.OROperatorWithPagination -> serviceHubHandle.vaultService.queryBy<SimpleObjectState>(
                queryToCheckSimpleObjectId.or(queryToCheckStateRef),
                PageSpecification(1, numObjectsInLedger)
            ).states
            is QueryFramingOptions.OROperatorWithoutPagination -> serviceHubHandle.vaultService
                .queryBy<SimpleObjectState>(queryToCheckSimpleObjectId.or(queryToCheckStateRef)).states
            is QueryFramingOptions.WithoutOROperator ->
                serviceHubHandle.vaultService
                    .queryBy<SimpleObjectState>(queryToCheckSimpleObjectId).states +
                        serviceHubHandle.vaultService.queryBy<SimpleObjectState>(queryToCheckStateRef).states
        }
        return result
    }

    @Test
    fun `filter query with OR operator that returns only one tuple with pagination defined`() {
        var results = getSimpleObjectMatchingCondition(
            0, createdStateRefs[numObjectsInLedger-1], QueryFramingOptions
                .OROperatorWithPagination
        )
        assertEquals(2, results.size)
    }

    @Test
    fun `filter query with OR operator that returns only one tuple throws exception without pagination defined`() {
        getSimpleObjectMatchingCondition(
            0,
            createdStateRefs[numObjectsInLedger-1],
            QueryFramingOptions.OROperatorWithoutPagination
        )
    }

    @Test
    fun `filter query without OR operator that returns only one tuple without pagination defined`() {
        val results = getSimpleObjectMatchingCondition(
            0,
            createdStateRefs[numObjectsInLedger-1],
            QueryFramingOptions.WithoutOROperator
        )
        assertEquals(2, results.size)
    }
}