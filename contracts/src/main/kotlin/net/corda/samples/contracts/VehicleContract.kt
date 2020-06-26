package net.corda.samples.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat

class VehicleContract : Contract {
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputs = tx.inputs
        val outputs = tx.outputs
        when(command.value){
            is Commands.AddVehicle -> requireThat {
                "Transaction must have no input state and only one output state." using (inputs.isEmpty() && outputs
                    .isNotEmpty())
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class AddVehicle : Commands
    }
}