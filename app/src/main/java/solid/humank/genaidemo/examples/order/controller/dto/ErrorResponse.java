package solid.humank.genaidemo.examples.order.controller.dto;

import java.util.List;

public record ErrorResponse(
    List<String> errors
) {}
