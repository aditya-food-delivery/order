package com.aditya.order_service.outbox.model;




public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}