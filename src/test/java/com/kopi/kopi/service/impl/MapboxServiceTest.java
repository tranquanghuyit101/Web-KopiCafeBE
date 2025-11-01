package com.kopi.kopi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapboxServiceTest {

    @Test
    void geocodeAddress_withContextPlace_returnsGeoResultWithCity() throws Exception {
        MapboxService svc = new MapboxService();
        // mock RestTemplate and response
        var mockRest = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        String body = "{\"features\":[{\"center\":[108.2,16.0],\"context\":[{\"id\":\"place.123\",\"text\":\"Da Nang\"}],\"place_name\":\"Somewhere, Da Nang, Vietnam\"}]}";
        ResponseEntity<String> ok = new ResponseEntity<>(body, HttpStatus.OK);
        Mockito.when(mockRest.getForEntity(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(ok);

        ReflectionTestUtils.setField(svc, "http", mockRest);
        ReflectionTestUtils.setField(svc, "mapboxToken", "token-x");

        var res = svc.geocodeAddress("any address");
        assertThat(res).isNotNull();
        assertThat(res.lat()).isEqualTo(16.0);
        assertThat(res.lng()).isEqualTo(108.2);
        assertThat(res.city()).isEqualTo("Da Nang");
    }

    @Test
    void geocodeAddress_withoutContext_fallsBackToPlaceNameToken() throws Exception {
        MapboxService svc = new MapboxService();
        var mockRest = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        String body = "{\"features\":[{\"center\":[108.3,16.1],\"place_name\":\"House, District, Hoi An, Vietnam\"}]}";
        ResponseEntity<String> ok = new ResponseEntity<>(body, HttpStatus.OK);
        Mockito.when(mockRest.getForEntity(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(ok);

        ReflectionTestUtils.setField(svc, "http", mockRest);
        ReflectionTestUtils.setField(svc, "mapboxToken", "token-x");

        var res = svc.geocodeAddress("addr");
        assertThat(res).isNotNull();
        assertThat(res.lat()).isEqualTo(16.1);
        assertThat(res.lng()).isEqualTo(108.3);
        // fallback selects second-last token -> "Hoi An"
        assertThat(res.city()).isEqualTo("Hoi An");
    }

    @Test
    void geocodeAddress_non2xxResponse_returnsNull() {
        MapboxService svc = new MapboxService();
        var mockRest = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        ResponseEntity<String> bad = new ResponseEntity<>("err", HttpStatus.BAD_REQUEST);
        Mockito.when(mockRest.getForEntity(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(bad);

        ReflectionTestUtils.setField(svc, "http", mockRest);
        ReflectionTestUtils.setField(svc, "mapboxToken", "token-x");

        var res = svc.geocodeAddress("addr");
        assertThat(res).isNull();
    }

    @Test
    void getDrivingDistanceMeters_success_returnsDistance() throws Exception {
        MapboxService svc = new MapboxService();
        var mockRest = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        String body = "{\"routes\":[{\"distance\":1234.5}]}";
        ResponseEntity<String> ok = new ResponseEntity<>(body, HttpStatus.OK);
        Mockito.when(mockRest.getForEntity(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(ok);

        ReflectionTestUtils.setField(svc, "http", mockRest);
        ReflectionTestUtils.setField(svc, "mapboxToken", "token-x");

        double d = svc.getDrivingDistanceMeters(16.0, 108.0, 16.1, 108.1);
        assertThat(d).isEqualTo(1234.5);
    }

    @Test
    void getDrivingDistanceMeters_noRoutes_returnsMinusOne() throws Exception {
        MapboxService svc = new MapboxService();
        var mockRest = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        String body = "{\"routes\":[]}";
        ResponseEntity<String> ok = new ResponseEntity<>(body, HttpStatus.OK);
        Mockito.when(mockRest.getForEntity(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(ok);

        ReflectionTestUtils.setField(svc, "http", mockRest);
        ReflectionTestUtils.setField(svc, "mapboxToken", "token-x");

        double d = svc.getDrivingDistanceMeters(16.0, 108.0, 16.1, 108.1);
        assertThat(d).isEqualTo(-1d);
    }

    @Test
    void missingToken_geocodeAndDistance_returnDefaults() {
        MapboxService svc = new MapboxService();
        // leave mapboxToken null/blank
        ReflectionTestUtils.setField(svc, "mapboxToken", "");

        var geo = svc.geocodeAddress("a");
        assertThat(geo).isNull();

        double d = svc.getDrivingDistanceMeters(0, 0, 0, 0);
        assertThat(d).isEqualTo(-1d);
    }
}
