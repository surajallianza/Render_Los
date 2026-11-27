package com.los.bank.customer.dto;

public record CustomerDocumentFile(String fileName, String contentType, byte[] data) {
}
