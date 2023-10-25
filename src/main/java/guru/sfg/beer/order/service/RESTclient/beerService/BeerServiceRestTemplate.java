package guru.sfg.beer.order.service.RESTclient.beerService;

import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;

import java.util.UUID;

public interface BeerServiceRestTemplate {
    BeerDTO getBeerById(UUID beerId);
}
