package common.event;

import common.model.BeerOrderDto;
import lombok.Builder;
import lombok.Data;

@Data
public class AllocateOrderRequestEvent extends BeerOrderEvent{
    @Builder
    public AllocateOrderRequestEvent(BeerOrderDto beerOrderDto) {
        super(beerOrderDto);
    }
}
