package ru.practicum.interaction.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedRequestsUpdateRequest {

    private Integer delta;
}
