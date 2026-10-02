package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.store.StoreManager

/**
 * The server's teams: a [StoreManager] of [TeamState]. Everything else reads [state] (an immutable snapshot) or asks
 * for a change through [change]; an operation either returns a new valid state or throws ([TeamException] for a
 * refused request), leaving the state exactly as it was. Changes happen on the server thread.
 */
class TeamManager : StoreManager<TeamState>(TeamState.EMPTY)
