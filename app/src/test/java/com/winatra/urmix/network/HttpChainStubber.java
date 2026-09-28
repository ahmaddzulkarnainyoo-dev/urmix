package com.winatra.urmix.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Java-side Mockito glue for {@code Interceptor.Chain}.
 *
 * OkHttp 5 declares {@code proceed(request: Request)} in Kotlin with a non-null
 * parameter, so the Kotlin compiler inserts a call-site null check that rejects
 * the null returned by {@code ArgumentMatchers.any()} / {@code capture()}.
 * Calling Mockito from Java avoids that check entirely.
 */
final class HttpChainStubber {

    private HttpChainStubber() {
    }

    /** Builds the synthetic response the stubbed {@code proceed} must return. */
    @FunctionalInterface
    interface ProceedHandler {
        Response proceed(Request forwarded) throws IOException;
    }

    /**
     * Stubs {@code chain.proceed} so it answers through the given handler.
     *
     * @param chain   the Mockito mock of {@code Interceptor.Chain}
     * @param handler builds the synthetic response for each forwarded request
     * @throws IOException when the handler does
     */
    static void stubProceed(final Interceptor.Chain chain, final ProceedHandler handler)
            throws IOException {
        when(chain.proceed(any(Request.class))).thenAnswer((Answer<Response>) invocation ->
                handler.proceed(invocation.getArgument(0, Request.class)));
    }

    /**
     * Captures the request the interceptor under test forwarded down the chain.
     *
     * @param chain the Mockito mock of {@code Interceptor.Chain}
     * @return the captured request
     * @throws IOException declared for interface compatibility
     */
    static Request forwardedRequest(final Interceptor.Chain chain) throws IOException {
        final ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(captor.capture());
        return captor.getValue();
    }
}
