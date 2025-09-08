package Neon_db.library_rag;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EmbeddingIngestService {
    private final BookRepository repo;
    private final OpenAiClient openAi;

    public EmbeddingIngestService(BookRepository repo, OpenAiClient openAi) {
        this.repo = repo;
        this.openAi = openAi;
    }

    public int ingestAll(int batchSize) {
        int total = 0;
        while (true) {
            List<Book> batch = repo.findNeedingEmbeddings(batchSize);
            if (batch.isEmpty()) break;

            for (Book b : batch) {
                String text = (b.title() == null ? "" : b.title()) + "\n" +
                              (b.summary() == null ? "" : b.summary());
                float[] vec = openAi.embed(text);
                repo.updateEmbedding(b.id(), vec);
                total++;
            }
        }
        return total;
    }
}
