package guru.sfg.beer.order.service.services;

import common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);
    void processValidationResult(UUID orderId, boolean isValid);

    void processAllocationResult(BeerOrderDto beerOrderDto, boolean pendingInventory, boolean allocationError);

    BeerOrder pickUpBeerOrder(UUID beerOrderId);
}
