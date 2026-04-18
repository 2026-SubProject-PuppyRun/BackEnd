package org.zerock.puppyrun.common.s3.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class S3Serializer extends JsonSerializer<Object> {

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        switch (value) {
            case null -> gen.writeNull();

            // 단일 문자열인 경우 (Key -> Full URL 변환)
            case String str -> gen.writeString(buildFullUrl(str));

            // 리스트/컬렉션인 경우 (각 아이템에 대해 Key -> Full URL 변환 시도)
            case Collection<?> collection -> {
                gen.writeStartArray();
                for (Object item : collection) {
                    if (item instanceof String s) {
                        gen.writeString(buildFullUrl(s));
                    } else {
                        // String이 아닌 객체는 기본 직렬화 수행 (안정성 확보)
                        serializers.defaultSerializeValue(item, gen);
                    }
                }
                gen.writeEndArray();
            }

            // 그 외의 타입은 기본 직렬화로 위임
            default -> serializers.defaultSerializeValue(value, gen);
        }
    }

    /**
     * S3 경로를 전체 URL로 변환하는 공통 로직
     */
    private String buildFullUrl(String path) {
        if (path != null && !path.isBlank() && !path.startsWith("http")) {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, path);
        }
        return path;
    }
}
