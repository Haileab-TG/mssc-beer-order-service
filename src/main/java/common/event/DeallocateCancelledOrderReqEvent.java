package common.event;

import common.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeallocateCancelledOrderReqEvent {
    private BeerOrderDto beerOrderDto;
}
