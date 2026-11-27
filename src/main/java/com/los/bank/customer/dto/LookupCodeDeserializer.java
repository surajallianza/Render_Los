package com.los.bank.customer.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer that extracts a lookup code string
 * whether the client sends a plain string ("1000801") or an object ({"code": "1000801", "name": "Mr"}).
 */
public class LookupCodeDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        if (node.isObject() && node.has("code")) {
            JsonNode codeNode = node.get("code");
            return codeNode.isNull() ? null : codeNode.asText().trim();
        }
        return node.asText().trim();
    }
}
