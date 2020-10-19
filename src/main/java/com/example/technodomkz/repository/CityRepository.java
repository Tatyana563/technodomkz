package com.example.technodomkz.repository;

import com.example.technodomkz.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City,Integer> {
    boolean existsByUrlSuffix(String urlSuffix);

    @Query("select urlSuffix from City")
    List<String> getAllCities();
}
