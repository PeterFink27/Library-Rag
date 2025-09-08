package Neon_db.library_rag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.pgvector.PGvector;

@Repository
public class BookRepository {
    private final DataSource ds;

    public BookRepository(DataSource ds) {
        this.ds = ds;
    }

    public List<Book> findNeedingEmbeddings(int limit) {
        String sql = "SELECT id, title, summary FROM books WHERE embedding IS NULL LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Book(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("summary")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEmbedding(int id, float[] vec) {
        String sql = "UPDATE books SET embedding = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, new PGvector(vec));
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Book> searchByEmbedding(float[] qvec, int k) {
        String sql = """
            SELECT id, title, summary
            FROM books
            ORDER BY embedding <-> ?
            LIMIT ?
        """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, new PGvector(qvec));
            ps.setInt(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Book(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("summary")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
