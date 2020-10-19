package com.example.technodomkz.repository;

import com.example.technodomkz.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item,Integer> {
    Optional<Item> findOneByExternalId(String externalId);

//    @Modifying
//    @Transactional
//    @Query("update Item item set item.available=false where item.category=:category")
//    void resetItemAvailability(Category category);
}
