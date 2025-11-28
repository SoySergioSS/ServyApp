package com.example.servyapp.ui.waiter

data class WaiterState(
    val isLoading : Boolean = false,
    val navigationEvent: WaiterNavigationEvent? = null
) {

}

sealed class WaiterNavigationEvent {
    object NavigateToOrders : WaiterNavigationEvent()
}