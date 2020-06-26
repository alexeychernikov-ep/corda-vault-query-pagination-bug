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
import net.corda.samples.schema.VehicleSchemaV1
import net.corda.samples.states.VehicleDetail
import net.corda.samples.states.VehicleState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.internal.cordappsForPackages
import org.junit.Test
import kotlin.test.assertEquals

class ContractTests {
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

    init {
        mockNetwork.runNetwork()
    }

    private fun addVehicleToLedger(vehicle: VehicleDetail) : StateRef {
        val tx = TransactionBuilder(notaryNode.info.legalIdentities.first())
        tx.addCommand(
            VehicleContract.Commands.AddVehicle(), fooCorp.owningKey
        )
        tx.addOutputState(
            VehicleState(vehicle, listOf(fooCorp.party))
        )
        tx.verify(serviceHubHandle)
        val stx = serviceHubHandle.signInitialTransaction(tx)
        serviceHubHandle.recordTransactions(listOf(stx))
        val res = serviceHubHandle.vaultService.queryBy<VehicleState>(
            QueryCriteria.VaultQueryCriteria(
                stateRefs = listOf(StateRef(stx.id, 0))
            )
        )
        require(res.states.size == 1)
        return StateRef(stx.id, 0)
    }

    private fun getVehicleMatchingCondition(registrationNumber: String, stateRef: StateRef, withPagination:Boolean = true) :
            List<StateAndRef<VehicleState>>{
        val queryToCheckRegistrationNumber = builder {
            val conditionToCheckRegistrationNumber =
                VehicleSchemaV1.PersistentVehicleState::registrationNumber
                    .equal(registrationNumber)
            QueryCriteria.VaultCustomQueryCriteria<VehicleSchemaV1.PersistentVehicleState>(
                conditionToCheckRegistrationNumber, Vault.StateStatus.UNCONSUMED
            )
        }

        val queryToCheckStateRef =
            QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED, stateRefs = listOf(stateRef))

        val result = if (withPagination) {
            serviceHubHandle.vaultService.queryBy<VehicleState>(
                queryToCheckRegistrationNumber.or(queryToCheckStateRef),
                PageSpecification(1, DEFAULT_PAGE_SIZE + 1)
            )
        } else {
            serviceHubHandle.vaultService.queryBy<VehicleState>(queryToCheckRegistrationNumber.or(queryToCheckStateRef))
        }
        return result.states
    }


    @Test
    fun `filter query that returns only one tuple throws pagination error`() {
        val numCreatedVehicles = DEFAULT_PAGE_SIZE + 1
        val createdStateRefs = mutableListOf<StateRef>()
        repeat(numCreatedVehicles) { index ->
            createdStateRefs.add(addVehicleToLedger(VehicleDetail("R"+index, "C"+index)))
        }
        var results = getVehicleMatchingCondition("R0", createdStateRefs[1], true)
        assertEquals(2, results.size)
        println("Query succeeded with pagination")
        results = getVehicleMatchingCondition("R0", createdStateRefs[1], false)
    }
}