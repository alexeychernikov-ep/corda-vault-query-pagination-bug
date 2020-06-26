package net.corda.samples.contracts

import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
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
import org.junit.Test
import kotlin.test.assertEquals

class VaultQueryTests {
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

    private fun getSimpleObjectMatchingCondition(
        id: Int,
        stateRef: StateRef,
        withPagination: Boolean = true
    ): List<StateAndRef<SimpleObjectState>> {
        val queryToCheckSimpleObjectId = builder {
            val conditionToCheckRegistrationNumber =
                SimpleObjectSchemaV1.PersistentSimpleObjectState::id
                    .equal(id)
            QueryCriteria.VaultCustomQueryCriteria(conditionToCheckRegistrationNumber, Vault.StateStatus.UNCONSUMED)
        }

        val queryToCheckStateRef =
            QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED, stateRefs = listOf(stateRef))

        val result = if (withPagination) {
            serviceHubHandle.vaultService.queryBy(
                queryToCheckSimpleObjectId.or(queryToCheckStateRef),
                PageSpecification(1, DEFAULT_PAGE_SIZE + 1)
            )
        } else {
            serviceHubHandle.vaultService.queryBy<SimpleObjectState>(queryToCheckSimpleObjectId.or(queryToCheckStateRef))
        }
        return result.states
    }

    @Test
    fun `filter query that returns only one tuple throws pagination error`() {
        val numCreatedVehicles = DEFAULT_PAGE_SIZE + 1
        val createdStateRefs = mutableListOf<StateRef>()
        repeat(numCreatedVehicles) { index ->
            createdStateRefs.add(addSimpleObjectToLedger(SimpleObjectDetail(index)))
        }
        var results = getSimpleObjectMatchingCondition(0, createdStateRefs[1], true)
        assertEquals(2, results.size)
        println("Query succeeded with pagination and has the result has size 2")
        getSimpleObjectMatchingCondition(0, createdStateRefs[1], false)
    }
}