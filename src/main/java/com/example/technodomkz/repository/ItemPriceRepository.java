package com.example.technodomkz.repository;

import com.example.technodomkz.model.City;
import com.example.technodomkz.model.Item;
import com.example.technodomkz.model.ItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemPriceRepository extends JpaRepository<ItemPrice, Integer> {
    Optional<ItemPrice> findOneByItemAndCity(Item item, City city);
}
