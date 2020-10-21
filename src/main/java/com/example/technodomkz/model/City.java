package com.example.technodomkz.model;

import javax.persistence.*;

@Entity
@Table
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "text")
    private String name;
    @Column(columnDefinition = "text")
    private String urlSuffix;

    public City() {
    }

    public City(String name, String urlSuffix) {
        this.name = name;
        this.urlSuffix = urlSuffix;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlSuffix() {
        return urlSuffix;
    }

    public void setUrlSuffix(String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

}
