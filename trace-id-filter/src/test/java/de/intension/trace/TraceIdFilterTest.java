package de.intension.trace;

import static de.intension.trace.TraceId.TRACE_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

/**
 * GIVEN: a request with or without trace ID in header or query parameter
 * WHEN: {@link TraceIdFilter} processes the request
 * THEN: the trace ID is taken from the request or generated and put into the MDC, except for excluded paths.
 */
class TraceIdFilterTest
{

    private static final String GENERATED_PATTERN = "generated_[A-Za-z]{10}";

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc()
    {
        MDC.remove(TRACE_ID_KEY);
    }

    @Test
    void should_use_trace_id_from_header()
    {
        var request = mockRequest("/realms/fwu/protocol/openid-connect/auth", "from-header", "x-trace-id", "from-query");

        filter.filter(request);

        assertThat(MDC.get(TRACE_ID_KEY)).isEqualTo("from-header");
        verify(request).setProperty(TraceIdFilter.TRACE_ID_PROPERTY, "from-header");
    }

    @ParameterizedTest
    @ValueSource(strings = {"x-trace-id", "X-Trace-Id"})
    void should_use_trace_id_from_query_parameter_ignoring_case(String parameterName)
    {
        var request = mockRequest("/realms/fwu/protocol/openid-connect/auth", null, parameterName, "from-query");

        filter.filter(request);

        assertThat(MDC.get(TRACE_ID_KEY)).isEqualTo("from-query");
    }

    @Test
    void should_generate_trace_id_when_missing()
    {
        var request = mockRequest("/realms/fwu/protocol/openid-connect/auth", null, null, null);

        filter.filter(request);

        assertThat((String)MDC.get(TRACE_ID_KEY)).matches(GENERATED_PATTERN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "with space", "line\nbreak", "<script>"})
    void should_generate_trace_id_when_invalid(String invalid)
    {
        var request = mockRequest("/realms/fwu/protocol/openid-connect/auth", invalid, null, null);

        filter.filter(request);

        assertThat((String)MDC.get(TRACE_ID_KEY)).matches(GENERATED_PATTERN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/realms/master", "/realms/master/protocol/openid-connect/auth", "/resources", "/resources/abc/login/theme.css"})
    void should_not_process_excluded_paths(String path)
    {
        var request = mockRequest(path, "from-header", null, null);

        filter.filter(request);

        assertThat(MDC.get(TRACE_ID_KEY)).isNull();
        verify(request, never()).setProperty(anyString(), eq("from-header"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/realms/masters/account", "/realms/fwu", "/admin/realms/fwu/users", "/resourcesX"})
    void should_not_exclude_similar_paths(String path)
    {
        assertThat(TraceIdFilter.isExcluded(path)).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {"/realms/fwu,fwu", "/realms/fwu/login-actions/authenticate,fwu", "/realms/,null", "/admin/realms/fwu,null"}, nullValues = "null")
    void should_extract_realm_name_from_path(String path, String expected)
    {
        assertThat(TraceIdFilter.getRealmName(path)).isEqualTo(expected);
    }

    @Test
    void should_remove_trace_id_from_mdc_after_request()
    {
        var request = mockRequest("/realms/fwu/account", "from-header", null, null);
        filter.filter(request);
        when(request.getProperty(TraceIdFilter.TRACE_ID_PROPERTY)).thenReturn("from-header");

        filter.filter(request, mock(ContainerResponseContext.class));

        assertThat(MDC.get(TRACE_ID_KEY)).isNull();
    }

    @Test
    void should_generate_trace_id_in_expected_format()
    {
        assertThat(TraceId.generate()).matches(GENERATED_PATTERN).isNotEqualTo(TraceId.generate());
    }

    private static ContainerRequestContext mockRequest(String path, String header, String queryName, String queryValue)
    {
        var request = mock(ContainerRequestContext.class);
        var uriInfo = mock(UriInfo.class);
        var queryParameters = new MultivaluedHashMap<String, String>();
        if (queryName != null) {
            queryParameters.add(queryName, queryValue);
        }
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(request.getHeaderString(TraceId.TRACE_ID_PARAMETER)).thenReturn(header);
        when(uriInfo.getPath()).thenReturn(path);
        when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
        return request;
    }
}
