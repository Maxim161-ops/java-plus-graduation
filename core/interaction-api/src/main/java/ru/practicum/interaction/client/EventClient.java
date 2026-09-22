package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.event.ConfirmedRequestsUpdateRequest;
import ru.practicum.interaction.dto.event.EventResponse;
import ru.practicum.interaction.dto.event.EventShortResponse;

import java.util.List;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventResponse getEvent(@PathVariable("eventId") Long eventId);

    @PatchMapping("/{eventId}/confirmed-requests")
    void updateConfirmedRequests(
            @PathVariable("eventId") Long eventId,
            @RequestBody ConfirmedRequestsUpdateRequest request
    );

    @GetMapping
    List<EventShortResponse> getEvents(@RequestParam("ids") List<Long> ids);
}
