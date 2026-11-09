package cl.iplacex.discografia;

import java.util.List;
import java.util.Optional;
import cl.iplacex.discografia.artistas.*;
import cl.iplacex.discografia.discos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DiscografiaApiTests {
    private IArtistaRepository artistas;
    private IDiscoRepository discos;
    private MockMvc http;
    private final String artistaJson = """
            {"nombre":"Los Jaivas","estilos":["Rock"],"anioFundacion":1963,"estaActivo":true}
            """;
    private final String discoJson = """
            {"idArtista":"a1","nombre":"Alturas de Machu Picchu","anioLanzamiento":1981,
             "canciones":["La poderosa muerte"]}
            """;

    @BeforeEach
    void setUp() {
        artistas = mock(IArtistaRepository.class);
        discos = mock(IDiscoRepository.class);
        http = MockMvcBuilders.standaloneSetup(new ArtistaController(artistas),
                        new DiscoController(discos, artistas))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void creaArtistaYDevuelveId() throws Exception {
        when(artistas.insert(any(Artista.class))).thenAnswer(invocation -> {
            Artista artista = invocation.getArgument(0);
            artista._id = "a1";
            return artista;
        });
        http.perform(post("/api/artista").contentType(MediaType.APPLICATION_JSON).content(artistaJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$._id").value("a1"))
                .andExpect(jsonPath("$.nombre").value("Los Jaivas"));
    }

    @Test
    void listaYObtieneArtista() throws Exception {
        Artista artista = new Artista();
        artista._id = "a1";
        artista.nombre = "Los Jaivas";
        when(artistas.findAll()).thenReturn(List.of(artista));
        when(artistas.findById("a1")).thenReturn(Optional.of(artista));
        http.perform(get("/api/artistas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0]._id").value("a1"));
        http.perform(get("/api/artista/a1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Los Jaivas"));
    }

    @Test
    void actualizaSoloElIdDeLaUrl() throws Exception {
        when(artistas.existsById("a1")).thenReturn(true);
        when(artistas.save(any(Artista.class))).thenAnswer(i -> i.getArgument(0));
        http.perform(put("/api/artista/a1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"_id\":\"otro\",\"nombre\":\"Actualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._id").value("a1"));
        verify(artistas).save(argThat(a -> a._id.equals("a1") && a.nombre.equals("Actualizado")));
    }

    @Test
    void actualizarYEliminarAusentesNoModificaDatos() throws Exception {
        http.perform(put("/api/artista/ausente").contentType(MediaType.APPLICATION_JSON).content(artistaJson))
                .andExpect(status().isNotFound());
        http.perform(delete("/api/artista/ausente")).andExpect(status().isNotFound());
        verify(artistas, times(2)).existsById("ausente");
        verify(artistas, never()).save(any());
        verify(artistas, never()).deleteById(any());
    }

    @Test
    void eliminaArtistaExistente() throws Exception {
        when(artistas.existsById("a1")).thenReturn(true);
        http.perform(delete("/api/artista/a1")).andExpect(status().isOk())
                .andExpect(jsonPath("$._id").value("a1"));
        verify(artistas).deleteById("a1");
    }

    @Test
    void creaDiscoSoloSiExisteArtista() throws Exception {
        http.perform(post("/api/disco").contentType(MediaType.APPLICATION_JSON).content(discoJson))
                .andExpect(status().isNotFound());
        verify(discos, never()).insert(any(Disco.class));
        when(artistas.existsById("a1")).thenReturn(true);
        when(discos.insert(any(Disco.class))).thenAnswer(i -> {
            Disco disco = i.getArgument(0);
            disco._id = "d1";
            return disco;
        });
        http.perform(post("/api/disco").contentType(MediaType.APPLICATION_JSON).content(discoJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$._id").value("d1"));
    }

    @Test
    void listaObtieneYFiltraDiscos() throws Exception {
        Disco disco = new Disco();
        disco._id = "d1";
        disco.idArtista = "a1";
        when(discos.findAll()).thenReturn(List.of(disco));
        when(discos.findById("d1")).thenReturn(Optional.of(disco));
        when(discos.findDiscosByIdArtista("a1")).thenReturn(List.of(disco));
        http.perform(get("/api/discos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0]._id").value("d1"));
        http.perform(get("/api/disco/d1")).andExpect(status().isOk())
                .andExpect(jsonPath("$._id").value("d1"));
        http.perform(get("/api/artista/a1/discos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idArtista").value("a1"));
        verify(discos).findDiscosByIdArtista("a1");
    }

    @Test
    void devuelve404ParaIdsAusentesYListaVaciaParaFiltroSinDiscos() throws Exception {
        http.perform(get("/api/artista/ausente")).andExpect(status().isNotFound());
        http.perform(get("/api/disco/ausente")).andExpect(status().isNotFound());
        http.perform(get("/api/artista/ausente/discos")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void rechazaDatosInvalidosAntesDeInsertar() throws Exception {
        http.perform(post("/api/artista").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        http.perform(post("/api/disco").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Disco\"}"))
                .andExpect(status().isBadRequest());
        http.perform(post("/api/artista").contentType(MediaType.APPLICATION_JSON).content("{mal"))
                .andExpect(status().isBadRequest());
        http.perform(post("/api/artista").contentType(MediaType.TEXT_PLAIN).content(artistaJson))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(artistas, discos);
    }

    @Test
    void actualizarSinNombreNoGuarda() throws Exception {
        when(artistas.existsById("a1")).thenReturn(true);
        http.perform(put("/api/artista/a1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(artistas, never()).save(any());
    }

    @Test
    void duplicadosSon409YErroresDeBaseSon500SinDetallesInternos() throws Exception {
        when(artistas.insert(any(Artista.class))).thenThrow(new DuplicateKeyException("detalle interno"));
        http.perform(post("/api/artista").contentType(MediaType.APPLICATION_JSON).content(artistaJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El identificador ya existe"));
        when(discos.findAll()).thenThrow(new DataAccessResourceFailureException("detalle interno"));
        http.perform(get("/api/discos")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("No se pudo completar la operación en la base de datos"));
    }
}
