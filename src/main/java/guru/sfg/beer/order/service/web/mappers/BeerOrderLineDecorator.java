package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.RESTclient.beerService.BeerServiceRestTemplate;
import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.web.model.BeerOrderDto;
import guru.sfg.beer.order.service.web.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class BeerOrderLineDecorator implements BeerOrderLineMapper{
    private BeerOrderLineMapperImpl beerOrderLineMapper;
    private BeerServiceRestTemplate beerServiceRestTemplate;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto beerOrderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);
        BeerDTO beerDTO = beerServiceRestTemplate.getBeerById(beerOrderLineDto.getBeerId());
        beerOrderLineDto.setBeerName(beerDTO.getBeerName());
        return beerOrderLineDto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        return beerOrderLineMapper.dtoToBeerOrderLine(dto);
    }

    @Autowired
    public void setBeerOrderLineMapper(BeerOrderLineMapperImpl beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }

    @Autowired
    public void setBeerServiceRestTemplate(BeerServiceRestTemplate beerServiceRestTemplate) {
        this.beerServiceRestTemplate = beerServiceRestTemplate;
    }
}
