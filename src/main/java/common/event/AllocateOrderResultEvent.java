package common.event;

import common.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocateOrderResultEvent{
    private BeerOrderDto beerOrderDto;
    private boolean pendingInventory;
    private boolean allocationError;

}
