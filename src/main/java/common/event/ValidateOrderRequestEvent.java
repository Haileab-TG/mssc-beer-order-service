package common.event;

import common.model.BeerOrderDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidateOrderRequestEvent extends BeerOrderEvent{
    public ValidateOrderRequestEvent(BeerOrderDto beerOrderDto) {
        super(beerOrderDto);
    }
}
