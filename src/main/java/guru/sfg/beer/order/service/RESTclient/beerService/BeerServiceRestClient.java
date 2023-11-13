package guru.sfg.beer.order.service.RESTclient.beerService;

import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;

import java.util.Optional;
import java.util.UUID;

public interface BeerServiceRestClient {
    Optional<BeerDTO> getBeerById(UUID beerId);
    BeerDTO getBeerByUpc(String upc);
}
