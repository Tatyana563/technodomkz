package com.example.technodomkz.repository;



import com.example.technodomkz.model.Category;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByUrl(String url);
    @Query("from Category as c ORDER BY c.id ASC")
    List<Category> getChunk(PageRequest pageable);
}
