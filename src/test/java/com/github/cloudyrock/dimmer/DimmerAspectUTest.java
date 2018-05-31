package com.github.cloudyrock.dimmer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;

import static com.github.cloudyrock.dimmer.FeatureOffBehaviour.RETURN_NULL;
import static com.github.cloudyrock.dimmer.FeatureOffBehaviour.THROW_EXCEPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DimmerAspectUTest {

    private String feature;

    @Before
    public void setUp() {
        feature = "FEATURE" + UUID.randomUUID().toString();
    }

    @Test
    public void featureCheckAdvice_call_behaviour_with_args() throws Throwable {

        //given
        Object expectedReturnedValue = "returnedValue";
        Function<FeatureInvocation, Object> behaviourMock = mock(Function.class);
        when(behaviourMock.apply(any(FeatureInvocation.class)))
                .thenReturn(expectedReturnedValue);

        Signature signatureMock = mock(Signature.class);
        String methodName = "METHOD_NAME";
        when(signatureMock.getName()).thenReturn(methodName);
        when(signatureMock.getDeclaringType()).thenReturn(ArrayList.class);

        Object[] expectedArgs = {new Object(), new Object()};

        ProceedingJoinPoint jointPointMock = mock(ProceedingJoinPoint.class);
        when(jointPointMock.getSignature()).thenReturn(signatureMock);
        when(jointPointMock.getArgs()).thenReturn(expectedArgs);

        FeatureCheck featureCheck = mock(FeatureCheck.class);
        when(featureCheck.feature()).thenReturn(feature);

        DimmerConfiguration.featureOffWithBehaviour(feature, behaviourMock);

        //when
        Object actualReturnedValue = new DimmerAspect()
                .featureCheckAdvice(jointPointMock, featureCheck);

        //then
        ArgumentCaptor<FeatureInvocation> captor =
                ArgumentCaptor.forClass(FeatureInvocation.class);
        verify(behaviourMock).apply(captor.capture());
        FeatureInvocation invocation = captor.getValue();

        assertEquals(expectedReturnedValue, actualReturnedValue);
        assertEquals(methodName, invocation.getMethodName());
        assertEquals(expectedArgs.length, invocation.getArgs().length);
        for (int i = 0; i < expectedArgs.length; i++) {
            assertEquals(expectedArgs[i], invocation.getArgs()[i]);
        }
    }

    @Test
    public void featureCheckAdvice_execute_real_method_when_not_feature_off()
            throws Throwable {
        //given
        ProceedingJoinPoint jointPointMock = mock(ProceedingJoinPoint.class);
        Object expectedReturnedValue = "returnedValue";
        when(jointPointMock.proceed()).thenReturn(expectedReturnedValue);
        FeatureCheck featureCheck = mock(FeatureCheck.class);
        when(featureCheck.feature()).thenReturn(feature);

        //when
        Object actualReturnedValue = new DimmerAspect()
                .featureCheckAdvice(jointPointMock, featureCheck);

        //then
        verify(jointPointMock).proceed();
        assertEquals(expectedReturnedValue, actualReturnedValue);
    }

    @Test
    public void featureOff_return_null_when_RETURN_NULL() throws Throwable {
        FeatureOff featureOffAnn = mock(FeatureOff.class);
        when(featureOffAnn.value()).thenReturn(RETURN_NULL);
        assertNull(new DimmerAspect().featureOffAdvice(featureOffAnn));
    }

    @Test(expected = IllegalArgumentException.class)
    public void featureOff_throw_exception_when_THROW_EXCEPTION() throws Throwable {
        FeatureOff featureOffAnn = getFeatureOffException();
        assertNull(new DimmerAspect().featureOffAdvice(featureOffAnn));
    }

    private FeatureOff getFeatureOffException() {
        return new FeatureOff() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public FeatureOffBehaviour value() {
                return THROW_EXCEPTION;
            }

            @Override
            public Class<? extends RuntimeException> exception() {
                return IllegalArgumentException.class;
            }
        };
    }

}
