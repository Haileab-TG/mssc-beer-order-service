package guru.sfg.beer.order.service.RESTclient.beerService;

import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "htg.brewery")
@Component
public class BeerServiceRestTemplateImpl  implements BeerServiceRestTemplate{
    private static final String BEER_SERVICE_PATH = "/api/v1/beer";
    private String beerServiceHost;
    private final RestTemplate restTemplate;

    @Autowired
    public BeerServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID beerId) {
        String url = beerServiceHost + BEER_SERVICE_PATH + "/" + beerId;
        System.out.println("URL " + url);
        BeerDTO beerDTO = restTemplate.getForObject(url, BeerDTO.class
        );
        return Optional.of(beerDTO);
    }

    @Override
    public Optional<BeerDTO> getBeerByUpc(String upc) {
        String url = beerServiceHost + BEER_SERVICE_PATH + "/beerByUpc/" + upc;
        BeerDTO beerDTO = restTemplate.getForObject(url, BeerDTO.class);
        return Optional.of(beerDTO);
    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }
}
