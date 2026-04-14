package com.ordersaga.inventory.application;

import com.ordersaga.inventory.domain.Inventory;
import com.ordersaga.inventory.domain.InventoryRepository;
import com.ordersaga.inventory.fixture.DeductInventoryCommandFixture;
import com.ordersaga.inventory.fixture.InventoryFixtureValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class DeductInventoryApplicationTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryApplicationService inventoryApplicationService;

    @BeforeEach
    void setUp() {
        inventoryApplicationService = new InventoryApplicationService(inventoryRepository);
    }

    @Test
    @DisplayName("재고 차감 성공 시 남은 수량을 반환한다")
    void deductSuccess_returnsRemainingQuantity() {
        // Given
        DeductInventoryCommand command = DeductInventoryCommandFixture.normal();
        given(inventoryRepository.findBySku(command.sku()))
                .willReturn(Optional.of(new Inventory(command.sku(), InventoryFixtureValues.AVAILABLE_QUANTITY)));

        // When
        DeductInventoryResult result = inventoryApplicationService.deductInventory(command);

        // Then
        assertThat(result.sku()).isEqualTo(command.sku());
        assertThat(result.deductedQuantity()).isEqualTo(command.quantity());
        assertThat(result.remainingQuantity()).isEqualTo(InventoryFixtureValues.REMAINING_QUANTITY);
    }

    @Test
    @DisplayName("재고 부족 시 예외가 발생한다")
    void insufficientInventory_throwsException() {
        // Given
        DeductInventoryCommand command = DeductInventoryCommandFixture.normal();
        given(inventoryRepository.findBySku(command.sku()))
                .willReturn(Optional.of(new Inventory(command.sku(), command.quantity() - 1)));

        // When & Then
        assertThatThrownBy(() -> inventoryApplicationService.deductInventory(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(command.sku());
    }
}
