package cl.iplacex.discografia;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Object> duplicateKey(DuplicateKeyException exception) {
        return new ResponseEntity<>(Map.of("error", "El identificador ya existe"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> databaseError(DataAccessException exception) {
        return new ResponseEntity<>(Map.of("error", "No se pudo completar la operación en la base de datos"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
