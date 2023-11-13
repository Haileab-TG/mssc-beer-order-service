package guru.sfg.beer.order.service.RESTclient.beerService;

import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@ConfigurationProperties(prefix = "htg.brewery")
@Component
public class BeerServiceClientRestTemplate implements BeerServiceRestClient {
    public static final String BEER_SERVICE_GET_BY_UPC_PATH = "/api/v1/beer/beerByUpc/";
    public static final String BEER_SERVICE_GET_BY_ID_PATH = "/api/v1/beer";
    private String beerServiceHost;
    private final RestTemplate restTemplate;

    @Autowired
    public BeerServiceClientRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID beerId) {
        String url = beerServiceHost + BEER_SERVICE_GET_BY_ID_PATH + beerId;
        System.out.println("URL " + url);
        BeerDTO beerDTO = restTemplate.getForObject(url, BeerDTO.class
        );
        return Optional.of(beerDTO);
    }

    @Override
    public BeerDTO getBeerByUpc(String upc) {
        String url = beerServiceHost + BEER_SERVICE_GET_BY_UPC_PATH + upc;
        BeerDTO beerDTO = restTemplate.getForObject(url, BeerDTO.class);
        log.debug("BeerServiceClientRestTemplate : Get beer by UPC from BeerService was invoked and returned " + beerDTO);
        return beerDTO;
    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }
}
