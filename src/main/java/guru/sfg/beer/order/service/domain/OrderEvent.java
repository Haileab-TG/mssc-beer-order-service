package guru.sfg.beer.order.service.domain;

public enum OrderEvent {
    VALIDATE_ORDER, VALIDATION_PASSED, VALIDATION_FAILED,
    ALLOCATION_SUCCESS, ALLOCATION_NO_INVENTORY, ALLOCATION_FAILED,
    ORDER_PICKED_UP
}
