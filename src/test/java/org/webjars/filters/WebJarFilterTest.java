package org.webjars.filters;

import io.undertow.servlet.spec.ServletOutputStreamImpl;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.webjars.MultipleMatchesException;
import org.webjars.WebJarAssetLocator;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by David
 * Date: 01.05.2018 - 17:36.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebJarFilterTest {
    @InjectMocks
    private WebJarFilter webJarFilter;

    @Mock
    private WebJarAssetLocator webJarAssetLocator;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private FilterChain filterChain;

    @Mock
    private FilterConfig filterConfig;

    @Test
    public void init() {
        Mockito.when(filterConfig.getInitParameter(WebJarFilter.WEB_INIT_PARAM_RESPONSE_SERVE_METHOD)).thenReturn("REDIRECT");
        webJarFilter.init(filterConfig);
        MatcherAssert.assertThat(webJarFilter.responseServeMethod, Matchers.is(WebJarFilter.ResponseServeMethod.REDIRECT));
    }

    @Test
    public void init_withFallback() {
        Mockito.when(filterConfig.getInitParameter(WebJarFilter.WEB_INIT_PARAM_RESPONSE_SERVE_METHOD)).thenReturn("invalid response method");
        webJarFilter.init(filterConfig);
        MatcherAssert.assertThat(webJarFilter.responseServeMethod, Matchers.is(WebJarFilter.ResponseServeMethod.WRITE_BYTE_RESPONSE));
    }

    @Test
    public void doFilter_writeByteResponse_withMultipleMatches() throws IOException, ServletException {
        String requestUri = "/webjars/popper.js/popper.min.js";
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(requestUri);
        List<String> matches = Arrays.asList(
                "META-INF/resources/webjars/popper.js/1.14.1/esm/popper.js",
                "META-INF/resources/webjars/popper.js/1.14.1/umd/popper.js",
                "META-INF/resources/webjars/popper.js/1.14.1/popper.js"
        );
        MultipleMatchesException multipleMatchesException = new MultipleMatchesException("a message..", matches);
        Mockito.when(webJarAssetLocator.getFullPath("popper.js", "popper.min.js")).thenThrow(multipleMatchesException);

        ServletOutputStreamImpl outputStream = Mockito.mock(ServletOutputStreamImpl.class);
        Mockito.when(httpServletResponse.getOutputStream()).thenReturn(outputStream);
        webJarFilter.responseServeMethod = WebJarFilter.ResponseServeMethod.WRITE_BYTE_RESPONSE;
        webJarFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        Mockito.verify(outputStream, Mockito.times(9)).write(ArgumentMatchers.any(byte[].class), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
        Mockito.verifyZeroInteractions(filterChain);
    }

    @Test
    public void doFilter_redirect() throws IOException, ServletException {
        Mockito.when(webJarAssetLocator.getFullPath("jquery", "jquery.min.js")).thenReturn("META-INF/resources/webjars/jquery/3.0.0/jquery.min.js");
        webJarFilter.responseServeMethod = WebJarFilter.ResponseServeMethod.REDIRECT;
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn("/webjars/jquery/jquery.min.js");
        webJarFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        InOrder inOrder = Mockito.inOrder(httpServletResponse, filterChain);
        inOrder.verify(httpServletResponse, Mockito.times(1)).sendRedirect("/webjars/jquery/3.0.0/jquery.min.js");
        inOrder.verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void doFilter_noInfiniteRedirections() throws IOException, ServletException {
        Mockito.when(webJarAssetLocator.getFullPath("jquery", "jquery.min.js")).thenReturn("META-INF/resources/webjars/jquery/3.0.0/jquery.min.js");
        webJarFilter.responseServeMethod = WebJarFilter.ResponseServeMethod.REDIRECT;
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn("/webjars/jquery/3.0.0/jquery.min.js");
        webJarFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        Mockito.verifyZeroInteractions(httpServletResponse);
        Mockito.verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
    }

    // Test if resolution also works, if the context root is not simply set to "/" but to "/foo/"
    @Test
    public void doFilter_nestedContextRoot() throws IOException, ServletException {
        Mockito.when(webJarAssetLocator.getFullPath("jquery", "jquery.min.js")).thenReturn("META-INF/resources/webjars/jquery/3.0.0/jquery.min.js");
        webJarFilter.responseServeMethod = WebJarFilter.ResponseServeMethod.REDIRECT;
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn("foo/webjars/jquery/jquery.min.js");
        webJarFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        InOrder inOrder = Mockito.inOrder(httpServletResponse, filterChain);
        inOrder.verify(httpServletResponse, Mockito.times(1)).sendRedirect("foo/webjars/jquery/3.0.0/jquery.min.js");
        inOrder.verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
        inOrder.verifyNoMoreInteractions();
    }
}
