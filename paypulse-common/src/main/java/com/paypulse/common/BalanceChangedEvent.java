package com.paypulse.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class BalanceChangedEvent {
    @NotNull
    private UUID userId;
    @NotNull
    @Size(min = 1, max = 50)
    private String type;
    @Size(max = 255)
    private String description;
    
    public BalanceChangedEvent() {}
    public BalanceChangedEvent(UUID userId, String type, String description) {
        this.userId = userId;
        this.type = type;
        this.description = description;
    }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
} 