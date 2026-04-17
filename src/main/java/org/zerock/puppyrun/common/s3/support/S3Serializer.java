package org.zerock.puppyrun.common.s3.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class S3Serializer extends JsonSerializer<String> {

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null && !value.isBlank() && !value.startsWith("http")) {
            String fullUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, value);
            gen.writeString(fullUrl);
        } else {
            gen.writeString(value);
        }
    }
}
