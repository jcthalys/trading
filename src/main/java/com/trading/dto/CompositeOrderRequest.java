package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompositeOrderRequest(
    @NotEmpty List<CompositeInstrumentRequest> instruments, @NotBlank String traderId) {}
