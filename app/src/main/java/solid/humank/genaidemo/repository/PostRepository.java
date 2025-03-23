package solid.humank.genaidemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import solid.humank.genaidemo.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
