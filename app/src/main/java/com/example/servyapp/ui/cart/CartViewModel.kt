package com.example.servyapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.CartRepository
import com.example.servyapp.domain.model.CartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartState())
    val uiState: StateFlow<CartState> = _uiState

    init {
        loadCartItems()
    }

    private fun loadCartItems() {
        viewModelScope.launch {
            cartRepository.cartItems.collectLatest { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun incrementQuantity(cartItemId: String) {
        cartRepository.incrementQuantity(cartItemId)
    }

    fun decrementQuantity(cartItemId: String) {
        cartRepository.decrementQuantity(cartItemId)
    }

    fun showDeleteDialog(item: CartItem) {
        _uiState.update { it.copy(showDeleteDialog = item) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }

    fun confirmDeleteItem() {
        val item = _uiState.value.showDeleteDialog
        if (item != null) {
            cartRepository.removeFromCart(item.id)
            dismissDeleteDialog()
        }
    }

    fun clearCart() {
        cartRepository.clearCart()
    }

    fun onItemClick(item: CartItem) {
        _uiState.update {
            it.copy(
                navigationEvent = NavigationEvent.NavigateToDishDetail(
                    restaurantId = item.restaurantId,
                    dishId = item.dish.id
                )
            )
        }
    }

    fun onCheckoutClick() {
        if (_uiState.value.items.isNotEmpty()) {
            _uiState.update { it.copy(navigationEvent = NavigationEvent.NavigateToCheckout) }
        }
    }

    fun onNavigationEventHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}