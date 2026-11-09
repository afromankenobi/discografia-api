package cl.iplacex.discografia.artistas;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class ArtistaController {
    private final IArtistaRepository artistaRepo;

    public ArtistaController(IArtistaRepository artistaRepo) {
        this.artistaRepo = artistaRepo;
    }

    @PostMapping(value = "/artista", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandleInsertArtistaRequest(@RequestBody Artista artista) {
        if (artista.nombre == null || artista.nombre.isBlank()) {
            return new ResponseEntity<>(Map.of("error", "El nombre es obligatorio"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(artistaRepo.insert(artista), HttpStatus.CREATED);
    }

    @GetMapping(value = "/artistas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Artista>> HandleGetAristasRequest() {
        return new ResponseEntity<>(artistaRepo.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/artista/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandleGetArtistaRequest(@PathVariable("id") String id) {
        var artista = artistaRepo.findById(id);
        if (artista.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Artista no encontrado"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(artista.get(), HttpStatus.OK);
    }

    @PutMapping(value = "/artista/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandleUpdateArtistaRequest(
            @PathVariable("id") String id, @RequestBody Artista artista) {
        if (!artistaRepo.existsById(id)) {
            return new ResponseEntity<>(Map.of("error", "Artista no encontrado"), HttpStatus.NOT_FOUND);
        }
        if (artista.nombre == null || artista.nombre.isBlank()) {
            return new ResponseEntity<>(Map.of("error", "El nombre es obligatorio"), HttpStatus.BAD_REQUEST);
        }
        // El ID de la URL determina el documento que se actualiza.
        artista._id = id;
        return new ResponseEntity<>(artistaRepo.save(artista), HttpStatus.OK);
    }

    @DeleteMapping(value = "/artista/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandleDeleteArtistaRequest(@PathVariable("id") String id) {
        if (!artistaRepo.existsById(id)) {
            return new ResponseEntity<>(Map.of("error", "Artista no encontrado"), HttpStatus.NOT_FOUND);
        }
        // shortcut: no elimina discos asociados; definir cascada si el alcance lo requiere.
        artistaRepo.deleteById(id);
        return new ResponseEntity<>(Map.of("mensaje", "Artista eliminado", "_id", id), HttpStatus.OK);
    }
}
