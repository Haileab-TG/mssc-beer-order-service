package guru.sfg.beer.order.service.web.mappers;

import common.model.BeerOrderLineDto;
import guru.sfg.beer.order.service.RESTclient.beerService.BeerServiceRestClient;
import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public abstract class BeerOrderLineDecorator implements BeerOrderLineMapper{
    private BeerOrderLineMapper beerOrderLineMapper;
    private BeerServiceRestClient beerServiceRestClient;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto beerOrderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);
        BeerDTO beerDto = beerServiceRestClient.getBeerByUpc(beerOrderLineDto.getUpc());
        beerOrderLineDto.setBeerName(beerDto.getBeerName());
        beerOrderLineDto.setBeerId(beerDto.getId());
        return beerOrderLineDto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        return beerOrderLineMapper.dtoToBeerOrderLine(dto);
    }

    @Autowired
    public void setBeerOrderLineMapper(BeerOrderLineMapper beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }

    @Autowired
    public void setBeerServiceRestTemplate(BeerServiceRestClient beerServiceRestClient) {
        this.beerServiceRestClient = beerServiceRestClient;
    }
}
