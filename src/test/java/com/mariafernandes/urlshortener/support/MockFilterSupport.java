package com.mariafernandes.urlshortener.support;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public final class MockFilterSupport {

    private MockFilterSupport() {
    }

    public static void passThrough(Filter... filters) {
        for (Filter filter : filters) {
            doAnswer(invocation -> {
                try {
                    ServletRequest request = invocation.getArgument(0);
                    ServletResponse response = invocation.getArgument(1);
                    FilterChain chain = invocation.getArgument(2);
                    chain.doFilter(request, response);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            }).when(filter).doFilter(any(), any(), any());
        }
    }
}
