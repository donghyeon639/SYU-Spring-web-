package com.example.allfree;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostService {
    private final PostRepository repo;

    public PostService(PostRepository repo) {
        this.repo = repo;
    }

    public List<Post> findAll() {
        return repo.findAll();
    }

    public Post findById(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public Post save(Post post) {
        return repo.save(post);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
