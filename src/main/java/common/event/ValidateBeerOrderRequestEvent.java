package common.event;

import common.model.BeerOrderDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidateBeerOrderRequestEvent extends BeerOrderEvent{
    public ValidateBeerOrderRequestEvent(BeerOrderDto beerOrderDto) {
        super(beerOrderDto);
    }
}
