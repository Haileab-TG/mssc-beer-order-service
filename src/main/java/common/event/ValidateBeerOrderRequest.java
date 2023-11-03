package common.event;

import common.model.BeerOrderDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidateBeerOrderRequest extends BeerOrderEvent{
    public ValidateBeerOrderRequest(BeerOrderDto beerOrderDto) {
        super(beerOrderDto);
    }
}
