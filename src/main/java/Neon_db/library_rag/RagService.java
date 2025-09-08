package Neon_db.library_rag;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final BookRepository repo;
    private final OpenAiClient openai;

    public RagService(BookRepository repo, OpenAiClient openai) {
        this.repo = repo;
        this.openai = openai;
    }

    public record Answer(String answer, List<Integer> sourceIds) {}

    public Answer ask(String question, int k) throws Exception {
        // 1) embed the question
        float[] qvec = openai.embed(question);

        // 2) retrieve top-k matches
        List<Book> hits = repo.searchByEmbedding(qvec, k);

        // 3) build context
        String context = hits.stream()
                .map(b -> "- [" + b.id() + "] " + b.title() + ": " + (b.summary() == null ? "" : b.summary()))
                .collect(Collectors.joining("\n"));
        String system = "Answer using only the provided context. If unsure, say you don't know.";
        String user = "Context:\n" + context + "\n\nQuestion:\n" + question;

        // 4) call OpenAI chat
        String content = openai.chat(system, user);

        // 5) return answer + source IDs
        List<Integer> ids = hits.stream().map(Book::id).toList();
        return new Answer(content, ids);
    }
}
