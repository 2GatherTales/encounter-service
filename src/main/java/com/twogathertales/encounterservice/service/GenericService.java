package com.twogathertales.encounterservice.service;

public interface GenericService<T> {

    public Iterable<T> findAll();

    public T  find(Long id);

    public T save(T t);

    public void update(T t);

    public void delete(Long id);

}