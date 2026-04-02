package com.ordersaga.inventory.application;

import com.ordersaga.inventory.domain.Inventory;
import com.ordersaga.inventory.domain.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryApplicationService {
    private final InventoryRepository inventoryRepository;

    public InventoryApplicationService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResult getInventory(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("inventory not found: " + sku));

        return InventoryResult.from(inventory);
    }

    @Transactional
    public DeductInventoryResult deductInventory(DeductInventoryCommand command) {
        Inventory inventory = inventoryRepository.findBySku(command.sku())
                .orElseThrow(() -> new IllegalArgumentException("inventory not found: " + command.sku()));

        if (command.forceFailure()) {
            throw new IllegalStateException("forced inventory failure for sku: " + command.sku());
        }

        inventory.deduct(command.quantity());

        return DeductInventoryResult.from(inventory, command.quantity());
    }
}
