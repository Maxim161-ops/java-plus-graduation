package client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatClientTest {

    private static final String STATS_SERVICE_ID = "stats-server";

    private StatClient statClient;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private ServiceInstance serviceInstance;

    @BeforeEach
    void setUp() {
        statClient = new StatClient(
                discoveryClient,
                STATS_SERVICE_ID
        );
    }

    @Test
    void hit_shouldFindStatsServerThroughDiscoveryClient() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        assertThatNoException().isThrownBy(() ->
                statClient.hit(
                        "test-app",
                        "/events/1",
                        "127.0.0.1",
                        LocalDateTime.now()
                )
        );

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);

        verify(serviceInstance)
                .getHost();

        verify(serviceInstance)
                .getPort();
    }

    @Test
    void hit_shouldHandleUnavailableStatsServer() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        assertThatNoException().isThrownBy(() ->
                statClient.hit(
                        "test-app",
                        "/events",
                        "127.0.0.1",
                        LocalDateTime.now()
                )
        );

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);
    }

    @Test
    void hit_shouldHandleMissingStatsServer() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() ->
                statClient.hit(
                        "test-app",
                        "/events",
                        "127.0.0.1",
                        LocalDateTime.now()
                )
        );

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);

        verifyNoInteractions(serviceInstance);
    }

    @Test
    void getStat_shouldFindStatsServerThroughDiscoveryClient() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        List<ViewStatsDto> result = statClient.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                List.of("/events"),
                true
        );

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);

        verify(serviceInstance)
                .getHost();

        verify(serviceInstance)
                .getPort();
    }

    @Test
    void getStat_shouldHandleMultipleUris() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        List<ViewStatsDto> result = statClient.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                List.of(
                        "/events",
                        "/events/1",
                        "/events/2"
                ),
                true
        );

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);
    }

    @Test
    void getStat_shouldHandleNullUris() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        List<ViewStatsDto> result = statClient.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                null,
                false
        );

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);
    }

    @Test
    void getStat_shouldHandleEmptyUris() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(List.of(serviceInstance));

        when(serviceInstance.getHost())
                .thenReturn("localhost");

        when(serviceInstance.getPort())
                .thenReturn(9999);

        List<ViewStatsDto> result = statClient.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                Collections.emptyList(),
                false
        );

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);
    }

    @Test
    void getStat_shouldThrowExceptionWhenStatsServerNotFound() {
        when(discoveryClient.getInstances(STATS_SERVICE_ID))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                statClient.getStat(
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        LocalDateTime.of(2024, 1, 2, 0, 0),
                        List.of("/events"),
                        true
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Не найден сервис статистики");

        verify(discoveryClient)
                .getInstances(STATS_SERVICE_ID);

        verifyNoInteractions(serviceInstance);
    }
}
