package common.event;

import common.model.BeerOrderDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DeallocateCancelledOrderReqEvent {
    private BeerOrderDto beerOrderDto;
}
