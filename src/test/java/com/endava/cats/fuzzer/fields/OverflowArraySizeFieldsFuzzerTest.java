package com.endava.cats.fuzzer.fields;

import com.endava.cats.args.FilesArguments;
import com.endava.cats.args.MatchArguments;
import com.endava.cats.fuzzer.executor.FieldsIteratorExecutor;
import com.endava.cats.http.HttpMethod;
import com.endava.cats.http.ResponseCodeFamily;
import com.endava.cats.io.ServiceCaller;
import com.endava.cats.model.CatsResponse;
import com.endava.cats.model.FuzzingData;
import com.endava.cats.report.TestCaseExporter;
import com.endava.cats.report.TestCaseListener;
import com.endava.cats.util.CatsUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.swagger.v3.oas.models.media.ArraySchema;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

@QuarkusTest
class OverflowArraySizeFieldsFuzzerTest {
    ServiceCaller serviceCaller;
    @InjectSpy
    TestCaseListener testCaseListener;
    @InjectSpy
    CatsUtil catsUtil;
    FieldsIteratorExecutor catsExecutor;
    private OverflowArraySizeFieldsFuzzer overflowArraySizeFieldsFuzzer;

    @BeforeEach
    void setup() {
        serviceCaller = Mockito.mock(ServiceCaller.class);
        ReflectionTestUtils.setField(testCaseListener, "testCaseExporter", Mockito.mock(TestCaseExporter.class));
        catsExecutor = new FieldsIteratorExecutor(serviceCaller, testCaseListener, catsUtil, Mockito.mock(MatchArguments.class), Mockito.mock(FilesArguments.class));
        overflowArraySizeFieldsFuzzer = new OverflowArraySizeFieldsFuzzer(catsExecutor);
    }

    @Test
    void shouldHaveDescription() {
        Assertions.assertThat(overflowArraySizeFieldsFuzzer.description()).isNotBlank();
    }

    @Test
    void shouldHaveToString() {
        Assertions.assertThat(overflowArraySizeFieldsFuzzer).hasToString("OverflowArraySizeFieldsFuzzer");
    }

    @Test
    void shouldSkipForNonBodyMethods() {
        Assertions.assertThat(overflowArraySizeFieldsFuzzer.skipForHttpMethods())
                .contains(HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD);
    }

    @Test
    void shouldSkipIfFieldPrimitive() {
        FuzzingData data = Mockito.mock(FuzzingData.class);
        Mockito.when(data.getAllFieldsByHttpMethod()).thenReturn(Set.of("primitiveField"));
        Mockito.when(data.getPayload()).thenReturn("""
                    {"primitiveField": 3}
                """);
        overflowArraySizeFieldsFuzzer.fuzz(data);
        Mockito.verifyNoInteractions(testCaseListener);
    }

    @Test
    void shouldRunIfFieldArray() {
        FuzzingData data = Mockito.mock(FuzzingData.class);
        Mockito.when(data.getAllFieldsByHttpMethod()).thenReturn(Set.of("arrayField"));
        Mockito.when(data.getRequestPropertyTypes()).thenReturn(Map.of("arrayField", new ArraySchema().maxItems(20)));
        Mockito.when(data.getPayload()).thenReturn("""
                   {"arrayField": [{
                            "inner": "innerValue"
                        }]
                    }
                """);
        overflowArraySizeFieldsFuzzer.fuzz(data);
        Mockito.verify(testCaseListener, Mockito.times(1)).reportResult(Mockito.any(), Mockito.eq(data), Mockito.any(), Mockito.eq(ResponseCodeFamily.FOURXX));
    }


    @Test
    void shouldSkipIfFieldObjectAndNotArray() {
        FuzzingData data = Mockito.mock(FuzzingData.class);
        Mockito.when(serviceCaller.call(Mockito.any())).thenReturn(CatsResponse.builder().body("{}").responseCode(200).build());

        Mockito.when(data.getAllFieldsByHttpMethod()).thenReturn(Set.of("objectField"));
        Mockito.when(data.getPayload()).thenReturn("""
                    {"objectField": {
                            "inner": "innerValue"
                        }
                    }
                """);
        overflowArraySizeFieldsFuzzer.fuzz(data);
        Mockito.verifyNoInteractions(testCaseListener);
    }
}
