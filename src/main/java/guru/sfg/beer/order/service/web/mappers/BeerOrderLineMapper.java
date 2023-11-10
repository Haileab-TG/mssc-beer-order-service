package guru.sfg.beer.order.service.web.mappers;

import common.model.BeerOrderLineDto;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
@DecoratedWith(value = BeerOrderLineDecorator.class)
public interface BeerOrderLineMapper {
    BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line);

    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto);
}
