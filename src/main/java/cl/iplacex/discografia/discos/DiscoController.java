package cl.iplacex.discografia.discos;

import java.util.List;
import java.util.Map;
import cl.iplacex.discografia.artistas.IArtistaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class DiscoController {
    private final IDiscoRepository discoRepo;
    private final IArtistaRepository artistaRepo;

    public DiscoController(IDiscoRepository discoRepo, IArtistaRepository artistaRepo) {
        this.discoRepo = discoRepo;
        this.artistaRepo = artistaRepo;
    }

    @PostMapping(value = "/disco", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandlePostDiscoRequest(@RequestBody Disco disco) {
        if (disco.idArtista == null || disco.idArtista.isBlank()
                || disco.nombre == null || disco.nombre.isBlank()) {
            return new ResponseEntity<>(Map.of("error", "Nombre e idArtista son obligatorios"), HttpStatus.BAD_REQUEST);
        }
        if (!artistaRepo.existsById(disco.idArtista)) {
            return new ResponseEntity<>(Map.of("error", "Artista no encontrado"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(discoRepo.insert(disco), HttpStatus.CREATED);
    }

    @GetMapping(value = "/discos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Disco>> HandleGetDiscosRequest() {
        return new ResponseEntity<>(discoRepo.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/disco/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> HandleGetDiscoRequest(@PathVariable("id") String id) {
        var disco = discoRepo.findById(id);
        if (disco.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Disco no encontrado"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(disco.get(), HttpStatus.OK);
    }

    @GetMapping(value = "/artista/{id}/discos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Disco>> HandleGetDiscosByArtistaRequest(@PathVariable("id") String id) {
        return new ResponseEntity<>(discoRepo.findDiscosByIdArtista(id), HttpStatus.OK);
    }
}
