package Neon_db.library_rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class IngestRunner implements CommandLineRunner {
    private final EmbeddingIngestService service;

    @Value("${app.rag.ingest:false}")
    private boolean runIngestProp;

    public IngestRunner(EmbeddingIngestService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        String envVal = System.getenv().getOrDefault("RAG_INGEST", "false");
        boolean runIngest = runIngestProp || "true".equalsIgnoreCase(envVal);

        System.out.println("[IngestRunner] app.rag.ingest=" + runIngestProp + ", RAG_INGEST=" + envVal + ", effectiveRun=" + runIngest);

        if (!runIngest) {
            System.out.println("[IngestRunner] Skipping ingest (set app.rag.ingest=true or export RAG_INGEST=true)");
            return;
        }

        try {
            int updated = service.ingestAll(100);
            System.out.println("Embedding ingest complete. Rows updated: " + updated);
        } catch (Exception e) {
            System.err.println("[IngestRunner] Ingest failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
