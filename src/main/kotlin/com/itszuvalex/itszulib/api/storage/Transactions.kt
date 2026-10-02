package com.itszuvalex.itszulib.api.storage

import net.neoforged.neoforge.transfer.transaction.Transaction

object Transactions {
    /**
     * Opens a transaction nested in the one currently open on this thread, or a root transaction if none is. For
     * adapters that mutate a NeoForge handler on behalf of a caller that may already be inside a transaction (e.g.
     * behind [com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIItemStorage]): a root transaction would
     * throw there, and a nested one makes the change part of the caller's transaction, rolled back if it aborts.
     *
     * @return Null while a transaction is closing, when none may be opened. Adapters then skip the write: it comes from
     * a wrapper's snapshot journal restoring the state it saw, and the backing handler took part in the same
     * transaction, so it restores its own state.
     */
    @JvmStatic
    fun openJoined(): Transaction? = when (Transaction.getLifecycle()) {
        Transaction.Lifecycle.CLOSING, Transaction.Lifecycle.ROOT_CLOSING -> null
        else -> Transaction.getCurrentOpenedTransaction()?.let(Transaction::open) ?: Transaction.openRoot()
    }
}
