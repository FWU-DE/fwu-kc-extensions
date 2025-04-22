package de.intension.testhelper;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.MediaType;

public class LicenceMockHelper
{

    public static Expectation requestLicenceExpectation(MockServerClient mockServerClient)
    {
        return mockServerClient
            .when(
                  request().withPath("/v1/licences/request")
                      .withMethod("GET")
                      .withHeader("X-API-Key", "sample-api-key")
                      .withQueryStringParameter("userId", "9c7e5634-5021-4c3e-9bea-53f54c299a0f")
                      .withQueryStringParameter("clientName", "account-console")
                      .withQueryStringParameter("bundesland", "de-DE")
                      .withQueryStringParameter("schulnummer", "DE-SN-Schullogin.0815"),
                  Times.exactly(1))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withBody("[{\"licenceCode\":\"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"},{\"licenceCode\":\"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"}]")
                         .withHeaders(
                                      header(CONTENT_TYPE, MediaType.JSON_UTF_8.getType())))[0];
    }

    public static Expectation requestLicenceExpectationBilo(MockServerClient mockServerClient)
    {
        return mockServerClient
            .when(
                  request().withPath("/v1/ucs/request")
                      .withMethod("GET")
                      .withHeader("X-API-Key", "sample-api-key")
                      .withQueryStringParameter("userId", "9c7e5634-5021-4c3e-9bea-53f54c299a0f")
                      .withQueryStringParameter("clientId", "account-console")
                      .withQueryStringParameter("bundesland", "de-DE")
                      .withQueryStringParameter("schulkennung", "DE-SN-Schullogin.0815"),
                  Times.exactly(1))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withBody("{\"id\":\"sample-user-id\",\"first_name\":\"Max\",\"last_name\":\"Muster\",\"licenses\":[\"ucs-license-1\",\"ucs-license-2\"],\"context\":{\"additionalProp1\":{\"licenses\":[\"ucs-license-prop-1\",\"ucs-license-prop-2\"],\"classes\":[{\"name\":\"class-1\",\"id\":\"sample-id1\",\"licenses\":[\"ucs-class-license-1\",\"ucs-classlicense-2\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]},\"additionalProp2\":{\"licenses\":[\"string\"],\"classes\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]},\"additionalProp3\":{\"licenses\":[\"string\"],\"classes\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]}}}")
                         .withHeaders(
                                      header(CONTENT_TYPE, MediaType.JSON_UTF_8.getType())))[0];
    }
}
