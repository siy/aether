package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pragmatica.lang.Result.lift;

public interface JacksonJsonCodec extends JsonCodec {
    static JacksonJsonCodec forMapper(ObjectMapper objectMapper) {
        record jacksonJsonCodec(ObjectMapper objectMapper) implements JacksonJsonCodec {
            @Override
            public Result<ByteBuf> serialize(Object value) {
                return lift(CodecError::fromSerializationThrowable,
                            () -> wrappedBuffer(objectMapper.writeValueAsBytes(value)));
            }

            @Override
            public <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token) {
                JavaType javaType = objectMapper.constructType(token.token());
                return lift(CodecError::fromDeserializationThrowable,
                            () -> objectMapper.readValue(ByteBufUtil.getBytes(entity), javaType));
            }

            @Override
            public <T> Result<T> deserialize(String json, TypeToken<T> token) {
                JavaType javaType = objectMapper.constructType(token.token());
                return lift(CodecError::fromDeserializationThrowable, () -> objectMapper.readValue(json, javaType));
            }

            @Override
            public <T> Result<T> deserialize(byte[] bytes, TypeToken<T> token) {
                JavaType javaType = objectMapper.constructType(token.token());
                return lift(CodecError::fromDeserializationThrowable, () -> objectMapper.readValue(bytes, javaType));
            }
        }
        return new jacksonJsonCodec(objectMapper);
    }

    static JacksonJsonCodec defaultCodec() {
        return forMapper(new ObjectMapper().findAndRegisterModules());
    }
}
