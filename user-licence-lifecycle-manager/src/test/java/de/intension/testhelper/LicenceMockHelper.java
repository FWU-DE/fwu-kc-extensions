package de.intension.testhelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.intension.rest.licence.model.LicenceRequest;
import de.intension.rest.licence.model.RemoveLicenceRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.MediaType;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

public class LicenceMockHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Expectation requestLicenceExpectation(MockServerClient mockServerClient)
            throws JsonProcessingException {
        LicenceRequest licenceRequestedRequest = new LicenceRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f", "account-console",
                "DE-SN-Schullogin.0815", "de-DE");
        return mockServerClient
                .when(
                        request().withPath("/v1/licences/request")
                                .withMethod("POST")
                                .withHeader("X-API-Key", "sample-api-key")
                                .withBody(objectMapper.writeValueAsString(licenceRequestedRequest)),
                        Times.exactly(1))
                .respond(
                        response()
                                .withStatusCode(OK_200.code())
                                .withReasonPhrase(OK_200.reasonPhrase())
                                .withBody("{\"licences\": [\"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\",\"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"]}")
                                .withHeaders(
                                        header(CONTENT_TYPE, MediaType.JSON_UTF_8.getType())))[0];
    }

    public static Expectation releaseLicenceExpectation(MockServerClient clientAndServer)
            throws JsonProcessingException {
        RemoveLicenceRequest licenceRequestedRequest = new RemoveLicenceRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f");
        return clientAndServer
                .when(
                        request().withPath("/v1/licences/release")
                                .withMethod("POST")
                                .withHeader("X-API-Key", "sample-api-key")
                                .withBody(objectMapper.writeValueAsString(licenceRequestedRequest)),
                        Times.exactly(1))
                .respond(
                        response()
                                .withStatusCode(OK_200.code())
                                .withReasonPhrase(OK_200.reasonPhrase())
                                .withHeaders(
                                        header(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType())))[0];
    }
}
