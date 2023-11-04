package common.event;

import common.model.BeerOrderDto;
import lombok.Builder;
import lombok.Data;

@Data
public class AllocateOrderResultEvent extends BeerOrderEvent{
    private boolean pendingInventory;
    private boolean allocationError;

    @Builder
    public AllocateOrderResultEvent(BeerOrderDto beerOrderDto, boolean pendingInventory,
                                    boolean allocationError
    ) {
        super(beerOrderDto);
        this.pendingInventory = pendingInventory;
        this.allocationError = allocationError;
    }
}
