package forge.wikilaw.backend.integration.frontend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import forge.wikilaw.backend.entity.FonteDados;
import forge.wikilaw.backend.entity.RegistroBruto;
import forge.wikilaw.backend.entity.Tribunal;
import forge.wikilaw.backend.integration.documents.DocumentSource;
import forge.wikilaw.backend.integration.documents.NormalizedDocument;
import forge.wikilaw.backend.repository.FonteDadosRepository;
import forge.wikilaw.backend.repository.RegistroBrutoRepository;
import forge.wikilaw.backend.repository.TribunalRepository;
import forge.wikilaw.backend.service.DocumentRecordProcessor;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida o contrato JSON consumido pela tela de resultados do frontend.
 * O teste não executa React: percorre a API real até o banco H2 e verifica
 * se os campos necessários para montar os cartões são devolvidos corretamente.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FrontendApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRecordProcessor processor;

    @Autowired
    private FonteDadosRepository fontes;

    @Autowired
    private RegistroBrutoRepository registrosBrutos;

    @Autowired
    private TribunalRepository tribunais;

    @Test
    void pesquisaRetornaContratoDeCartaoParaAsTresCategorias() throws Exception {
        tribunal("TJDFT");
        tribunal("STJ");

        criarDocumento(
                DocumentSource.TJDFT,
                "TJDFT",
                NormalizedDocument.builder()
                        .identificador("frontend-decisao-dano")
                        .titulo("Indenização por dano moral")
                        .tipo("Acórdão")
                        .resumo("Ementa sobre dano moral e responsabilidade civil.")
                        .decisao("Pedido parcialmente procedente.")
                        .relator("Desembargador Exemplo")
                        .orgaoJulgador("2ª Turma Cível")
                        .numeroProcesso("0700000-00.2025.8.07.0001")
                        .dataJulgamento(LocalDate.of(2025, 1, 15))
                        .dataPublicacao(LocalDate.of(2025, 1, 20))
                        .url("https://example.test/decisoes/frontend-decisao-dano")
                        .metadados("{}")
                        .build());

        criarDocumento(
                DocumentSource.STJ_PRECEDENTES,
                "STJ",
                NormalizedDocument.builder()
                        .identificador("frontend-precedente-dano")
                        .titulo("Precedente sobre dano moral")
                        .tipo("Recurso Repetitivo")
                        .numeroTema("1234")
                        .resumo("Questão jurídica sobre dano moral.")
                        .tese("A tese firmada trata da reparação por dano.")
                        .situacao("Julgado")
                        .dataJulgamento(LocalDate.of(2025, 2, 10))
                        .dataPublicacao(LocalDate.of(2025, 2, 18))
                        .url("https://example.test/precedentes/frontend-precedente-dano")
                        .metadados("{}")
                        .build());

        criarDocumento(
                DocumentSource.BDJUR,
                "BDJUR",
                NormalizedDocument.builder()
                        .identificador("frontend-doutrina-dano")
                        .titulo("Doutrina sobre dano moral")
                        .tipo("Artigo")
                        .resumo("Estudo doutrinário sobre dano moral.")
                        .autores("Maria Silva")
                        .periodico("Revista de Direito Civil")
                        .dataPublicacao(LocalDate.of(2025, 3, 5))
                        .url("https://example.test/doutrina/frontend-doutrina-dano")
                        .metadados("{}")
                        .build());

        // O frontend consulta cada categoria separadamente ao pesquisar.
        mockMvc.perform(get("/api/documentos/decisoes")
                        .param("termo", "dano")
                        .param("pagina", "0")
                        .param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].categoria").value("decisoes"))
                .andExpect(jsonPath("$.itens[0].fonte").value("TJDFT"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Indenização por dano moral"))
                .andExpect(jsonPath("$.itens[0].resumoOuEmenta")
                        .value("Ementa sobre dano moral e responsabilidade civil."))
                .andExpect(jsonPath("$.itens[0].decisao").value("Pedido parcialmente procedente."))
                .andExpect(jsonPath("$.itens[0].tribunal").value("TJDFT"))
                .andExpect(jsonPath("$.itens[0].orgaoJulgador").value("2ª Turma Cível"))
                .andExpect(jsonPath("$.itens[0].dataPublicacao").value("2025-01-20"))
                .andExpect(jsonPath("$.itens[0].urlOriginal")
                        .value("https://example.test/decisoes/frontend-decisao-dano"));

        mockMvc.perform(get("/api/documentos/precedentes")
                        .param("termo", "dano")
                        .param("pagina", "0")
                        .param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].categoria").value("precedentes"))
                .andExpect(jsonPath("$.itens[0].fonte").value("STJ"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Precedente sobre dano moral"))
                .andExpect(jsonPath("$.itens[0].resumoOuEmenta")
                        .value("Questão jurídica sobre dano moral."))
                .andExpect(jsonPath("$.itens[0].decisao")
                        .value("A tese firmada trata da reparação por dano."))
                .andExpect(jsonPath("$.itens[0].tribunal").value("STJ"))
                .andExpect(jsonPath("$.itens[0].dataPublicacao").value("2025-02-18"))
                .andExpect(jsonPath("$.itens[0].urlOriginal")
                        .value("https://example.test/precedentes/frontend-precedente-dano"));

        mockMvc.perform(get("/api/documentos/doutrina")
                        .param("termo", "dano")
                        .param("pagina", "0")
                        .param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].categoria").value("doutrina"))
                .andExpect(jsonPath("$.itens[0].fonte").value("BDJUR"))
                .andExpect(jsonPath("$.itens[0].titulo").value("Doutrina sobre dano moral"))
                .andExpect(jsonPath("$.itens[0].resumoOuEmenta")
                        .value("Estudo doutrinário sobre dano moral."))
                .andExpect(jsonPath("$.itens[0].autoresOuRelator").value("Maria Silva"))
                .andExpect(jsonPath("$.itens[0].dataPublicacao").value("2025-03-05"))
                .andExpect(jsonPath("$.itens[0].urlOriginal")
                        .value("https://example.test/doutrina/frontend-doutrina-dano"));
    }

    private void criarDocumento(
            DocumentSource source, String siglaFonte, NormalizedDocument documento) {
        RegistroBruto registro = registro(siglaFonte);
        processor.process(source, registro.getFonte().getId(), registro.getId(), documento);
    }

    private RegistroBruto registro(String sigla) {
        FonteDados fonte = new FonteDados();
        fonte.setNome(sigla);
        fonte.setSigla(sigla);
        fonte.setTipoFonte("TESTE");
        fonte = fontes.saveAndFlush(fonte);

        RegistroBruto registro = new RegistroBruto();
        registro.setFonte(fonte);
        registro.setFormatoPayload("JSON");
        registro.setDataColeta(OffsetDateTime.now());
        registro.setHashConteudo("f".repeat(64));
        registro.setPayloadTexto("{}");
        return registrosBrutos.saveAndFlush(registro);
    }

    private void tribunal(String sigla) {
        if (tribunais.findBySigla(sigla).isEmpty()) {
            Tribunal tribunal = new Tribunal();
            tribunal.setSigla(sigla);
            tribunal.setNome(sigla);
            tribunais.saveAndFlush(tribunal);
        }
    }
}
