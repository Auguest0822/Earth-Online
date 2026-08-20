package com.earthonline.backend.futureletters;

import com.earthonline.backend.futureletters.dto.FutureLetterRequest;
import com.earthonline.backend.futureletters.dto.FutureLetterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/future-letters")
public class FutureLetterController {

    private final FutureLetterService futureLetterService;

    public FutureLetterController(
            FutureLetterService futureLetterService
    ) {
        this.futureLetterService = futureLetterService;
    }

    @GetMapping
    public List<FutureLetterResponse> list(
            Authentication authentication
    ) {
        return futureLetterService.list(
                authentication.getName()
        );
    }

    @GetMapping("/{id}")
    public FutureLetterResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return futureLetterService.findById(
                authentication.getName(),
                id
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FutureLetterResponse create(
            @Valid @RequestBody FutureLetterRequest request,
            Authentication authentication
    ) {
        return futureLetterService.create(
                authentication.getName(),
                request
        );
    }

    @PatchMapping("/{id}")
    public FutureLetterResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FutureLetterRequest request,
            Authentication authentication
    ) {
        return futureLetterService.update(
                authentication.getName(),
                id,
                request
        );
    }

    @PostMapping("/{id}/open")
    public FutureLetterResponse open(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return futureLetterService.open(authentication.getName(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        futureLetterService.delete(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }
}
