package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException ex) {
        log.error("Получен статус 409 Conflict: {}", ex.getMessage(), ex);

        return buildError(
                ex,
                ex.getMessage(),
                "Запрос приводит к конфликту.",
                "CONFLICT"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {

        log.error("Получен статус 400 Bad Request: {}", ex.getMessage(), ex);

        return buildError(
                ex,
                ex.getMessage(),
                "Неправильно созданный запрос.",
                "BAD_REQUEST"
        );
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(Throwable ex) {
        log.error(
                "Получен статус 500 Internal Server Error: {}",
                ex.getMessage(),
                ex
        );

        return buildError(
                ex,
                ex.getMessage(),
                "Внутренняя ошибка сервера.",
                "INTERNAL_SERVER_ERROR"
        );
    }

    private ApiError buildError(
            Throwable ex,
            String message,
            String reason,
            String status
    ) {
        return ApiError.builder()
                .errors(getStackTrace(ex))
                .message(message)
                .reason(reason)
                .status(status)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    private List<String> getStackTrace(Throwable throwable) {
        return Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .toList();
    }
}
