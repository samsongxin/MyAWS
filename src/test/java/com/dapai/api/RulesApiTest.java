package com.dapai.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RulesApiTest {
    @Autowired
    private TestRestTemplate http;

    @Test
    void healthEndpointIdentifiesDaPai() {
        var response = http.getForEntity("/api/health", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("application").asText()).isEqualTo("DaPai");
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
    }

    @Test
    void evaluatesWildcardPairThroughHttp() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var json = """
                {"level":"TWO","cards":[
                  {"deck":0,"suit":"SPADES","rank":"KING"},
                  {"deck":0,"suit":"HEARTS","rank":"TWO"}
                ]}
                """;
        var response = http.postForEntity("/api/rules/evaluate", new HttpEntity<>(json, headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("valid").asBoolean()).isTrue();
        assertThat(response.getBody().path("combinations").size()).isEqualTo(1);
        var option = response.getBody().path("combinations").get(0);
        assertThat(option.path("type").asText()).isEqualTo("PAIR");
        assertThat(option.path("cardCount").asInt()).isEqualTo(2);
        assertThat(option.path("strength").asInt()).isEqualTo(13);
    }
}