package Neon_db.library_rag;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AskController {
    private final RagService rag;

    public AskController(RagService rag) {
        this.rag = rag;
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) throws Exception {
        String question = String.valueOf(body.getOrDefault("question", ""));
        int k = Integer.parseInt(String.valueOf(body.getOrDefault("k", 3)));
        var ans = rag.ask(question, k);
        return Map.of(
                "answer", ans.answer(),
                "sources", ans.sourceIds()
        );
    }
}
